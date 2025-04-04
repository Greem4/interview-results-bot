package ru.greemlab.interviewresultsbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.greemlab.interviewresultsbot.bot.CandidateEvaluationBot;
import ru.greemlab.interviewresultsbot.util.KeyboardFactory;

import static ru.greemlab.interviewresultsbot.service.UserStateService.UserState;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogStateMachineService {

    private final UserStateService userStateService;
    private final VoteStatisticsService voteStatisticsService;
    private final ArchiveCandidatesService archiveCandidatesService;

    /**
     * Обработка обычных текстовых сообщений.
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
     * Обработка inline-кнопок.
     */
    public void processCallbackQuery(CandidateEvaluationBot bot, CallbackQuery callbackQuery) {
        final Long chatId = callbackQuery.getMessage().getChatId();
        final Integer messageId = callbackQuery.getMessage().getMessageId();
        final String data = callbackQuery.getData();

        // Проверяем кнопки "Архив" / "Статистика"
        if (CallbackCommands.ARCHIVE.equals(data)) {
            // Просто шлём новое (временное) сообщение с архивом, потом можем удалить его,
            // либо оставить решение на пользователе
            var archiveMsgId = bot.sendTextMessage(chatId, archiveCandidatesService.getArchiveSummary(), null);
            // Если нужно, можете убрать сообщение через несколько секунд, или оставить
            handleStartCommand(bot, chatId);
            return;
        }
        if (CallbackCommands.CURRENT_STATS.equals(data)) {
            var statsMsgId = bot.sendTextMessage(chatId, voteStatisticsService.getAllCandidatesStatistics(), null);
            // Аналогично, можно по желанию потом удалить
            return;
        }

        // Узнаём текущее состояние
        final UserState currentState = userStateService.getState(chatId);

        switch (currentState) {
            case START -> handleCandidateSelection(bot, chatId, data);
            case WAITING_RESPONSIBILITY ->
                    handleRatingSelection(bot, chatId, data, CallbackCommands.RESP_PREFIX, UserState.WAITING_INTEREST);
            case WAITING_INTEREST ->
                    handleRatingSelection(bot, chatId, data, CallbackCommands.INTR_PREFIX, UserState.WAITING_RESULT_FOCUS);
            case WAITING_RESULT_FOCUS ->
                    handleRatingSelection(bot, chatId, data, CallbackCommands.RESF_PREFIX, UserState.WAITING_INVITE);
            case WAITING_INVITE -> handleInvitationDecision(bot, chatId, data);
            default ->
                    log.warn("Непредусмотренное состояние диалога: {} (chatId={})", currentState, chatId);
        }
    }

    /* ====================== /start, /restart и дефолтные ответы ====================== */

    private void handleStartCommand(CandidateEvaluationBot bot, Long chatId) {
        // Сбрасываем сессию этого пользователя
        userStateService.resetState(chatId);

        // Отправляем только ОДНО «главное меню» (кандидаты + кнопки статистики/архива),
        // которое не будем редактировать в дальнейшем
        bot.sendTextMessage(
                chatId,
                "🌟 Добро пожаловать! Ниже кнопки для выбора кандидата:",
                KeyboardFactory.buildMainMenuKeyboard()
        );
    }

    private void handleRestartCommand(CandidateEvaluationBot bot, Long chatId) {
        // Полный сброс всех пользователей, статистики
        userStateService.resetAllSessions();
        voteStatisticsService.resetStatistic();
        bot.sendTextMessage(
                chatId,
                "🔄 Данные сброшены. Наберите /start, чтобы начать заново.",
                null
        );
    }

    private void sendDefaultResponse(CandidateEvaluationBot bot, Long chatId) {
        bot.sendTextMessage(
                chatId,
                "ℹ Неизвестная команда. Введите /start, чтобы начать.",
                null
        );
    }

    /* ====================== Шаги голосования ====================== */

    /**
     * Пользователь в состоянии START нажал на имя кандидата.
     */
    private void handleCandidateSelection(CandidateEvaluationBot bot, Long chatId, String candidateKey) {
        if (!CandidateConstants.ALL.contains(candidateKey)) {
            log.warn("Некорректный выбор кандидата: {} (chatId={})", candidateKey, chatId);
            return;
        }
        var session = userStateService.getOrCreateSession(chatId);

        // Проверим, не голосовал ли уже пользователь
        if (session.hasVotedFor(candidateKey)) {
            // Уже голосовал за этого кандидата – просто покажем статистику
            var stats = voteStatisticsService.getCandidateStatistics(candidateKey);
            var msgText = "Вы уже голосовали за " + CandidateConstants.getCandidateName(candidateKey)
                          + "\n\nТекущая статистика:\n" + stats;
            bot.sendTextMessage(chatId, msgText, null);
            handleStartCommand(bot, chatId);
            return;
        }

        // Иначе начинаем новый цикл голосования.
        session.setCandidateKey(candidateKey);
        session.setState(UserState.WAITING_RESPONSIBILITY);

        // Отправим «временное» сообщение (шаг 1)
        // Запишем ID этого сообщения в сессию, чтобы потом обновлять или удалить
        var messageId = bot.sendTextMessage(
                chatId,
                "📝 Вы выбрали: " + CandidateConstants.getCandidateName(candidateKey)
                + "\n➡ Шаг 1/4: Оцените ответственность (1-5)",
                KeyboardFactory.buildRatingButtons(CallbackCommands.RESP_PREFIX)
        );

        session.setTempMessageId(messageId); // сохраним, чтобы дальше редактировать
    }

    /**
     * Обработка оценки (1..5) для одного из трёх критериев (ответственность, интерес, результативность).
     */
    private void handleRatingSelection(
            CandidateEvaluationBot bot,
            Long chatId,
            String data,
            String expectedPrefix,
            UserState nextState
    ) {
        if (!data.startsWith(expectedPrefix)) {
            log.warn("Неправильный префикс для оценки: {} (chatId={})", data, chatId);
            return;
        }
        var session = userStateService.getOrCreateSession(chatId);
        var messageId = session.getTempMessageId(); // то самое «временное» сообщение

        // Извлекаем число
        int score;
        try {
            var scoreStr = data.substring(expectedPrefix.length());
            score = Integer.parseInt(scoreStr);
            if (score < 1 || score > 5) {
                throw new NumberFormatException("Score out of range");
            }
        } catch (NumberFormatException e) {
            log.error("Некорректная оценка: {} (chatId={})", data, chatId);
            return;
        }

        var candidateKey = session.getCandidateKey();
        switch (expectedPrefix) {
            case CallbackCommands.RESP_PREFIX -> voteStatisticsService.addResponsibility(candidateKey, score);
            case CallbackCommands.INTR_PREFIX -> voteStatisticsService.addInterest(candidateKey, score);
            case CallbackCommands.RESF_PREFIX -> voteStatisticsService.addResultFocus(candidateKey, score);
            default -> log.warn("Неизвестный тип оценки: {}", expectedPrefix);
        }

        // Переходим к следующему шагу
        session.setState(nextState);

        // Формируем текст и клавиатуру для следующего шага
        String nextText;
        var nextKeyboard = switch (nextState) {
            case WAITING_INTEREST -> {
                nextText = "➡ Шаг 2/4: Оцените интерес к делу  (1-5)";
                yield KeyboardFactory.buildRatingButtons(CallbackCommands.INTR_PREFIX);
            }
            case WAITING_RESULT_FOCUS -> {
                nextText = "➡ Шаг 3/4: Оцените направленность на результат (1-5)";
                yield KeyboardFactory.buildRatingButtons(CallbackCommands.RESF_PREFIX);
            }
            case WAITING_INVITE -> {
                nextText = "➡ Шаг 4/4: Пригласили ли Вы данного кандидата на работу?";
                yield KeyboardFactory.buildInviteKeyboard();
            }
            default -> {
                nextText = "Неизвестный шаг";
                yield null;
            }
        };

        // Редактируем то же самое «временное» сообщение
        bot.editMessage(
                chatId,
                messageId,
                "📝 Вы выбрали: " + CandidateConstants.getCandidateName(candidateKey)
                + "\n" + nextText,
                nextKeyboard
        );
    }

    /**
     * Шаг "Пригласить / Не приглашать".
     */
    private void handleInvitationDecision(CandidateEvaluationBot bot, Long chatId, String data) {
        if (!data.equals(CallbackCommands.INVITE_YES) && !data.equals(CallbackCommands.INVITE_NO)) {
            log.warn("Непонятная кнопка в шаге INVITE: {} (chatId={})", data, chatId);
            return;
        }

        var session = userStateService.getOrCreateSession(chatId);
        var candidateKey = session.getCandidateKey();
        var tempMsgId = session.getTempMessageId();

        // Сохраняем голос
        if (data.equals(CallbackCommands.INVITE_YES)) {
            voteStatisticsService.addInviteYes(candidateKey);
        } else {
            voteStatisticsService.addInviteNo(candidateKey);
        }
        // Помечаем, что пользователь проголосовал за этого кандидата
        session.markVoted(candidateKey);

        // Завершение цикла голосования
        session.setState(UserState.START);
        session.setCandidateKey(null);

        // (По желанию) показываем в «временном» сообщении итоги, а потом удаляем
        // Или можно сразу удалить без показа
        var stats = voteStatisticsService.getCandidateStatistics(candidateKey);
        var finalText = "✅ Голосование завершено!\n\n"
                        + "Результаты по кандидату " + CandidateConstants.getCandidateName(candidateKey) + ":\n"
                        + stats
                        + "\n\n(Сообщение сейчас исчезнет)";

        // Редактируем текст итогового шага (без клавиатуры)
        bot.editMessage(chatId, tempMsgId, finalText, null);

        // Удаляем «временное» сообщение, чтобы «всё остальное» исчезло
        bot.deleteMessage(chatId, tempMsgId);

        // Стираем информацию о tempMessageId, чтобы не было путаницы
        session.setTempMessageId(null);

        // Если нужно – можем ничего больше не отправлять,
        // так как в чате остаётся только «главное меню» с именами и статистикой/архивом.
    }
}
