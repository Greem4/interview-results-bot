package ru.greemlab.interviewresultsbot.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.greemlab.interviewresultsbot.service.ArchiveCandidatesService;
import ru.greemlab.interviewresultsbot.service.UserStateService;
import ru.greemlab.interviewresultsbot.service.VoteStatisticsService;

import java.util.ArrayList;
import java.util.List;

import static ru.greemlab.interviewresultsbot.service.UserStateService.UserState;

/**
 * Telegram бот для оценки кандидатов на собеседованиях.
 * Обрабатывает интерактивное голосование и предоставляет статистику.
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class CandidateEvaluationBot extends TelegramLongPollingBot {

    private static final class Candidate {
        static final String VICTORIA = "victoria";
        static final String ALEXANDER = "alexander";
        static final String SVETLANA = "svetlana";
        static final List<String> ALL = List.of(VICTORIA, ALEXANDER, SVETLANA);
    }

    private static final class Callback {
        static final String CURRENT_STATS = "current_stats";
        static final String ARCHIVE = "archive";
        static final String RESP_PREFIX = "RESP_";
        static final String INTR_PREFIX = "INTR_";
        static final String RESF_PREFIX = "RESF_";
        static final String INVITE_YES = "INVITE_YES";
        static final String INVITE_NO = "INVITE_NO";
    }

    @Value("${app.bot.username}")
    private String botUsername;

    @Value("${app.bot.token}")
    private String botToken;

    private final UserStateService userStateService;
    private final VoteStatisticsService voteStatisticsService;
    private final ArchiveCandidatesService archiveCandidatesService;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        final String text = message.getText();
        final Long chatId = message.getChatId();

        if ("/start".equals(text)) {
            handleStartCommand(chatId);
        } else if ("/restart".equals(text)) {
            handleRestartCommand(chatId);
        } else {
            sendDefaultResponse(chatId);
        }
    }

    private void handleStartCommand(Long chatId) {
        userStateService.resetState(chatId);
        sendMessage(
                chatId,
                "🌟 Добро пожаловать! Выберите действие:",
                buildMainMenuKeyboard()
        );
    }

    private void handleRestartCommand(Long chatId) {
        userStateService.resetAllSessions();
        voteStatisticsService.resetStatistic();
        sendMessage(
                chatId,
                "🔄 Все данные сброшены. Начните заново с /start.",
                null
        );
    }

    private void sendDefaultResponse(Long chatId) {
        sendMessage(
                chatId,
                "ℹ Введите /start, чтобы начать.",
                null
        );
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        try {
            execute(new AnswerCallbackQuery(callbackQuery.getId()));
        } catch (Exception e) {
            log.error("Ошибка при обработке callback: {}", e.getMessage());
        }

        final String data = callbackQuery.getData();
        final Long chatId = callbackQuery.getMessage().getChatId();
        final Integer messageId = callbackQuery.getMessage().getMessageId();

        processCallbackData(chatId, messageId, data);
    }

    private void processCallbackData(Long chatId, Integer messageId, String data) {
        if (Callback.ARCHIVE.equals(data)) {
            showArchive(chatId, messageId);
            return;
        }
        if (Callback.CURRENT_STATS.equals(data)) {
            showCurrentStats(chatId, messageId);
            return;
        }

        switch (userStateService.getState(chatId)) {
            case START -> handleCandidateSelection(chatId, messageId, data);
            case WAITING_RESPONSIBILITY -> handleRatingSelection(
                    chatId, messageId, data, Callback.RESP_PREFIX, UserState.WAITING_INTEREST);
            case WAITING_INTEREST -> handleRatingSelection(
                    chatId, messageId, data, Callback.INTR_PREFIX, UserState.WAITING_RESULT_FOCUS);
            case WAITING_RESULT_FOCUS -> handleRatingSelection(
                    chatId, messageId, data, Callback.RESF_PREFIX, UserState.WAITING_INVITE);
            case WAITING_INVITE -> handleInvitationDecision(chatId, messageId, data);
            default -> log.warn("Необработанное состояние: {}", userStateService.getState(chatId));
        }
    }

    private void handleCandidateSelection(Long chatId, Integer messageId, String data) {
        if (!Candidate.ALL.contains(data)) {
            log.warn("Получен невалидный кандидат: {}", data);
            return;
        }

        userStateService.setCandidate(chatId, data);
        userStateService.setState(chatId, UserState.WAITING_RESPONSIBILITY);

        editMessageText(
                chatId,
                messageId,
                "📝 Вы выбрали: " + getCandidateName(data) + "\n\n➡ Шаг 1/4: Оцените ответственность (1-5):",
                buildRatingButtons(Callback.RESP_PREFIX)
        );
    }

    private void handleRatingSelection(Long chatId, Integer messageId, String data,
                                       String prefix, UserState nextState) {
        if (!data.startsWith(prefix)) {
            log.warn("Неверный префикс для оценки: {}", data);
            return;
        }

        final String scoreStr = data.substring(prefix.length());
        try {
            final int score = Integer.parseInt(scoreStr);
            if (score < 1 || score > 5) {
                throw new NumberFormatException();
            }
            processScore(chatId, score, prefix);
        } catch (NumberFormatException e) {
            log.error("Некорректная оценка: {}", scoreStr);
            return;
        }

        final String nextQuestion = switch (nextState) {
            case WAITING_INTEREST -> "➡ Шаг 2/4: Оцените интерес к делу (1-5):";
            case WAITING_RESULT_FOCUS -> "➡ Шаг 3/4: Оцените направленность на результат (1-5):";
            case WAITING_INVITE -> "➡ Шаг 4/4: Пригласили ли Вы данного кандидата на работу?";
            default -> "";
        };

        editMessageText(
                chatId,
                messageId,
                nextQuestion,
                nextState == UserState.WAITING_INVITE
                        ? buildInviteKeyboard()
                        : buildRatingButtons(getNextPrefix(nextState))
        );
        userStateService.setState(chatId, nextState);
    }

    private void processScore(Long chatId, int score, String type) {
        final String candidateKey = userStateService.getCandidate(chatId);
        switch (type) {
            case Callback.RESP_PREFIX -> voteStatisticsService.addResponsibility(candidateKey, score);
            case Callback.INTR_PREFIX -> voteStatisticsService.addInterest(candidateKey, score);
            case Callback.RESF_PREFIX -> voteStatisticsService.addResultFocus(candidateKey, score);
        }
    }

    private void handleInvitationDecision(Long chatId, Integer messageId, String data) {
        if (!Callback.INVITE_YES.equals(data) && !Callback.INVITE_NO.equals(data)) {
            log.warn("Неизвестное решение о приглашении: {}", data);
            return;
        }

        final String candidateKey = userStateService.getCandidate(chatId);
        if (Callback.INVITE_YES.equals(data)) {
            voteStatisticsService.addInviteYes(candidateKey);
        } else {
            voteStatisticsService.addInviteNo(candidateKey);
        }

        final String stats = voteStatisticsService.getCandidateStatistics(candidateKey);
        final String response = String.format(
                "✅ Спасибо за оценку!\n\n📊 Статистика по кандидату %s:\n%s\n─────────────────────",
                getCandidateName(candidateKey),
                stats
        );

        editMessageText(chatId, messageId, response, null);
        resetUserSession(chatId);
        sendMessage(chatId, "🔄 Для нового голосования введите /start", buildMainMenuKeyboard());
    }

    private void resetUserSession(Long chatId) {
        userStateService.setState(chatId, UserState.START);
        userStateService.setCandidate(chatId, null);
    }

    private InlineKeyboardMarkup buildMainMenuKeyboard() {
        final List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки выбора кандидатов
        rows.add(List.of(createButton("Виктория 🧑💼", Candidate.VICTORIA)));
        rows.add(List.of(createButton("Светлана 👩💻", Candidate.SVETLANA)));
        rows.add(List.of(createButton("Александр 👨🔧", Candidate.ALEXANDER)));

        // Сервисные кнопки
        rows.add(List.of(
                createButton("📊 Текущая статистика", Callback.CURRENT_STATS),
                createButton("📁 Архив", Callback.ARCHIVE)
        ));

        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup buildRatingButtons(String prefix) {
        final List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            buttons.add(createButton("⭐ " + i, prefix + i));
        }
        return new InlineKeyboardMarkup(List.of(buttons));
    }

    private InlineKeyboardMarkup buildInviteKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(createButton("✅ Да", Callback.INVITE_YES)),
                List.of(createButton("❌ Нет", Callback.INVITE_NO))
        ));
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        final InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private String getCandidateName(String key) {
        return switch (key) {
            case Candidate.VICTORIA -> "Виктория 🧑💼";
            case Candidate.SVETLANA -> "Светлана 👩💻";
            case Candidate.ALEXANDER -> "Александр 👨🔧";
            default -> "Неизвестный кандидат";
        };
    }

    private String getNextPrefix(UserState state) {
        return switch (state) {
            case WAITING_INTEREST -> Callback.INTR_PREFIX;
            case WAITING_RESULT_FOCUS -> Callback.RESF_PREFIX;
            default -> "";
        };
    }

    private void showArchive(Long chatId, Integer messageId) {
        editMessageText(
                chatId,
                messageId,
                archiveCandidatesService.getArchiveSummary(),
                null
        );
    }

    private void showCurrentStats(Long chatId, Integer messageId) {
        editMessageText(
                chatId,
                messageId,
                voteStatisticsService.getAllCandidatesStatistics(),
                null
        );
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        final SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    private void editMessageText(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        final EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setMessageId(messageId);
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            log.error("Ошибка при редактировании сообщения: {}", e.getMessage());
        }
    }
}