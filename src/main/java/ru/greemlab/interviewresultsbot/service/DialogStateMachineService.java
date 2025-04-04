package ru.greemlab.interviewresultsbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.greemlab.interviewresultsbot.bot.CandidateEvaluationBot;
import ru.greemlab.interviewresultsbot.util.KeyboardFactory;

import static ru.greemlab.interviewresultsbot.service.UserStateService.UserState;

/**
 * Основная логика «диалога» и «состояний» вынесена сюда:
 * 1. processTextMessage() – реакции на обычные сообщения.
 * 2. processCallbackQuery() – реакции на нажатие inline-кнопок.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogStateMachineService {

    private final UserStateService userStateService;
    private final VoteStatisticsService voteStatisticsService;
    private final ArchiveCandidatesService archiveCandidatesService;

    /**
     * Обрабатывает входящий текст от пользователя (команды и т.д.)
     */
    public void processTextMessage(CandidateEvaluationBot bot, Message message) {
        final Long chatId = message.getChatId();
        final String text = message.getText().trim();

        switch (text) {
            case "/start" -> handleStartCommand(bot, chatId);
            case "/restart" -> handleRestartCommand(bot, chatId);
            default -> sendDefaultResponse(bot, chatId);
        }
    }

    /**
     * Обрабатывает нажатие на inline-кнопку
     */
    public void processCallbackQuery(CandidateEvaluationBot bot, CallbackQuery callbackQuery) {
        final Long chatId = callbackQuery.getMessage().getChatId();
        final Integer messageId = callbackQuery.getMessage().getMessageId();
        final String data = callbackQuery.getData();

        // «Служебные» кнопки (статистика, архив)
        if (CallbackCommands.ARCHIVE.equals(data)) {
            bot.editMessage(chatId, messageId, archiveCandidatesService.getArchiveSummary(), null);
            return;
        }
        if (CallbackCommands.CURRENT_STATS.equals(data)) {
            bot.editMessage(chatId, messageId, voteStatisticsService.getAllCandidatesStatistics(), null);
            return;
        }

        // Логика пошагового заполнения
        final UserState currentState = userStateService.getState(chatId);
        switch (currentState) {
            case START -> handleCandidateSelection(bot, chatId, messageId, data);
            case WAITING_RESPONSIBILITY -> handleRatingSelection(bot, chatId, messageId, data,
                    CallbackCommands.RESP_PREFIX, UserState.WAITING_INTEREST);
            case WAITING_INTEREST -> handleRatingSelection(bot, chatId, messageId, data,
                    CallbackCommands.INTR_PREFIX, UserState.WAITING_RESULT_FOCUS);
            case WAITING_RESULT_FOCUS -> handleRatingSelection(bot, chatId, messageId, data,
                    CallbackCommands.RESF_PREFIX, UserState.WAITING_INVITE);
            case WAITING_INVITE -> handleInvitationDecision(bot, chatId, messageId, data);
            default ->
                    log.warn("Непредусмотренное состояние диалога: {} (chatId={})", currentState, chatId);
        }
    }

    /** ====================== Методы для /start, /restart ======================== */

    private void handleStartCommand(CandidateEvaluationBot bot, Long chatId) {
        userStateService.resetState(chatId);
        bot.sendTextMessage(
                chatId,
                "🌟 Добро пожаловать! Выберите действие:",
                KeyboardFactory.buildMainMenuKeyboard()
        );
    }

    private void handleRestartCommand(CandidateEvaluationBot bot, Long chatId) {
        userStateService.resetAllSessions();
        voteStatisticsService.resetStatistic();
        bot.sendTextMessage(
                chatId,
                "🔄 Все данные сброшены. Начните заново с /start.",
                null
        );
    }

    private void sendDefaultResponse(CandidateEvaluationBot bot, Long chatId) {
        bot.sendTextMessage(
                chatId,
                "ℹ Введите /start, чтобы начать.",
                null
        );
    }

    /* ====================== Логика выбора кандидата и оценок ======================== */

    private void handleCandidateSelection(CandidateEvaluationBot bot, Long chatId,
                                          Integer messageId, String data) {
        if (!CandidateConstants.ALL.contains(data)) {
            log.warn("Получен невалидный кандидат: {} (chatId={})", data, chatId);
            return;
        }
        // Сохраняем выбранного кандидата в state
        userStateService.setCandidate(chatId, data);
        userStateService.setState(chatId, UserState.WAITING_RESPONSIBILITY);

        bot.editMessage(
                chatId,
                messageId,
                "📝 Вы выбрали: " + CandidateConstants.getCandidateName(data)
                + "\n\n➡ Шаг 1/4: Оцените ответственность (1-5):",
                KeyboardFactory.buildRatingButtons(CallbackCommands.RESP_PREFIX)
        );
    }

    private void handleRatingSelection(
            CandidateEvaluationBot bot,
            Long chatId,
            Integer messageId,
            String data,
            String expectedPrefix,
            UserState nextState
    ) {
        if (!data.startsWith(expectedPrefix)) {
            log.warn("Неверный префикс для оценки: {} (chatId={})", data, chatId);
            return;
        }
        final String scoreStr = data.substring(expectedPrefix.length());
        int score;
        try {
            score = Integer.parseInt(scoreStr);
            if (score < 1 || score > 5) {
                throw new NumberFormatException("Score out of range 1..5");
            }
        } catch (NumberFormatException e) {
            log.error("Некорректная оценка: {} (chatId={}), {}", scoreStr, chatId, e.getMessage());
            return;
        }

        // Сохраняем оценку
        final String candidateKey = userStateService.getCandidate(chatId);
        switch (expectedPrefix) {
            case CallbackCommands.RESP_PREFIX -> voteStatisticsService.addResponsibility(candidateKey, score);
            case CallbackCommands.INTR_PREFIX -> voteStatisticsService.addInterest(candidateKey, score);
            case CallbackCommands.RESF_PREFIX -> voteStatisticsService.addResultFocus(candidateKey, score);
            default -> log.warn("Неизвестный тип оценки: {}", expectedPrefix);
        }

        // Следующий шаг
        userStateService.setState(chatId, nextState);

        final String nextText;
        final var nextKeyboard = switch (nextState) {
            case WAITING_INTEREST -> {
                nextText = "➡ Шаг 2/4: Оцените интерес к делу (1-5):";
                yield KeyboardFactory.buildRatingButtons(CallbackCommands.INTR_PREFIX);
            }
            case WAITING_RESULT_FOCUS -> {
                nextText = "➡ Шаг 3/4: Оцените направленность на результат (1-5):";
                yield KeyboardFactory.buildRatingButtons(CallbackCommands.RESF_PREFIX);
            }
            case WAITING_INVITE -> {
                nextText = "➡ Шаг 4/4: Пригласили ли вы данного кандидата?";
                yield KeyboardFactory.buildInviteKeyboard();
            }
            default -> {
                nextText = "Неизвестный шаг...";
                yield null;
            }
        };

        bot.editMessage(chatId, messageId, nextText, nextKeyboard);
    }

    private void handleInvitationDecision(CandidateEvaluationBot bot, Long chatId,
                                          Integer messageId, String data) {
        if (!data.equals(CallbackCommands.INVITE_YES) && !data.equals(CallbackCommands.INVITE_NO)) {
            log.warn("Неизвестная кнопка на шаге INVITE: {} (chatId={})", data, chatId);
            return;
        }

        final String candidateKey = userStateService.getCandidate(chatId);
        if (data.equals(CallbackCommands.INVITE_YES)) {
            voteStatisticsService.addInviteYes(candidateKey);
        } else {
            voteStatisticsService.addInviteNo(candidateKey);
        }

        // Завершаем сессию
        final String stats = voteStatisticsService.getCandidateStatistics(candidateKey);
        final String msg = String.format(
                "✅ Спасибо за оценку!\n\n📊 Статистика по кандидату %s:\n%s",
                CandidateConstants.getCandidateName(candidateKey),
                stats
        );

        bot.editMessage(chatId, messageId, msg, null);

        // Возвращаем пользователя в состояние START
        userStateService.setState(chatId, UserState.START);
        userStateService.setCandidate(chatId, null);

        // Предлагаем начать заново
        bot.sendTextMessage(chatId, "🔄 Для нового голосования введите /start",
                KeyboardFactory.buildMainMenuKeyboard());
    }
}
