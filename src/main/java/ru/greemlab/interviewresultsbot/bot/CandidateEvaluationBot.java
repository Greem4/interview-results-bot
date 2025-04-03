package ru.greemlab.interviewresultsbot.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

import static ru.greemlab.interviewresultsbot.service.UserStateService.*;

@Getter
@Component
@RequiredArgsConstructor
public class CandidateEvaluationBot extends TelegramLongPollingBot {

    private static final String CANDIDATE_VICTORIA = "victoria";
    private static final String CANDIDATE_ALEXANDER = "alexander";
    private static final String CANDIDATE_SVETLANA = "svetlana";
    private static final String CURRENT_STATS_CALLBACK = "current_stats";
    private static final String ARCHIVE_CALLBACK = "archive";

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
        String text = message.getText();
        Long chatId = message.getChatId();

        if (text.equals("/start")) {
            userStateService.setState(chatId, UserState.START);
            userStateService.setCandidate(chatId, null);
            sendMessage(
                    chatId,
                    "🌟 Добро пожаловать! Выберите действие:",
                    makeStartButtons()
            );
        } else if (text.equals("/restart")) {
            userStateService.resetAllSessions();
            voteStatisticsService.resetStatistic();
            sendMessage(chatId, "🔄 Все данные сброшены. Начните заново с /start.", null);
        } else {
            sendMessage(chatId, "ℹ Введите /start, чтобы начать.", null);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        try {
            execute(new AnswerCallbackQuery(callbackQuery.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        // Обработка общих кнопок
        if (data.equals(ARCHIVE_CALLBACK)) {
            editMessageText(chatId, messageId, archiveCandidatesService.getArchiveSummary(), null);
            return;
        }
        if (data.equals(CURRENT_STATS_CALLBACK)) {
            editMessageText(chatId, messageId, voteStatisticsService.getAllCandidatesStatistics(), null);
            return;
        }

        // Обработка состояний
        UserState currentState = userStateService.getState(chatId);
        switch (currentState) {
            case START -> handleStartState(chatId, messageId, data);
            case WAITING_RESPONSIBILITY -> handleResponsibilityState(chatId, messageId, data);
            case WAITING_INTEREST -> handleInterestState(chatId, messageId, data);
            case WAITING_RESULT_FOCUS -> handleResultFocusState(chatId, messageId, data);
            case WAITING_INVITE -> handleInviteState(chatId, messageId, data);
        }
    }

    private void handleStartState(Long chatId, Integer messageId, String data) {
        if (List.of(CANDIDATE_VICTORIA, CANDIDATE_ALEXANDER, CANDIDATE_SVETLANA).contains(data)) {
            userStateService.setCandidate(chatId, data);
            userStateService.setState(chatId, UserState.WAITING_RESPONSIBILITY);
            editMessageText(
                    chatId,
                    messageId,
                    "📝 Вы выбрали: " + convertKeyToName(data) + "\n\n"
                    + "➡ Шаг 1/4: Оцените ответственность (1-5):",
                    makeScoreButtons("RESP_")
            );
        }
    }

    private void handleResponsibilityState(Long chatId, Integer messageId, String data) {
        if (data.startsWith("RESP_")) {
            processScore(chatId, data.substring(5), "RESP");
            editMessageText(
                    chatId,
                    messageId,
                    "➡ Шаг 2/4: Оцените интерес к делу (1-5):",
                    makeScoreButtons("INTR_")
            );
            userStateService.setState(chatId, UserState.WAITING_INTEREST);
        }
    }

    private void handleInterestState(Long chatId, Integer messageId, String data) {
        if (data.startsWith("INTR_")) {
            processScore(chatId, data.substring(5), "INTR");
            editMessageText(
                    chatId,
                    messageId,
                    "➡ Шаг 3/4: Оцените направленность на результат (1-5):",
                    makeScoreButtons("RESF_")
            );
            userStateService.setState(chatId, UserState.WAITING_RESULT_FOCUS);
        }
    }

    private void handleResultFocusState(Long chatId, Integer messageId, String data) {
        if (data.startsWith("RESF_")) {
            processScore(chatId, data.substring(5), "RESF");
            editMessageText(
                    chatId,
                    messageId,
                    "➡ Шаг 4/4: Пригласили ли Вы данного кандидата на работу?",
                    makeInviteButtons()
            );
            userStateService.setState(chatId, UserState.WAITING_INVITE);
        }
    }

    private void handleInviteState(Long chatId, Integer messageId, String data) {
        if (data.equals("INVITE_YES") || data.equals("INVITE_NO")) {
            String cKey = userStateService.getCandidate(chatId);
            if (data.equals("INVITE_YES")) voteStatisticsService.addInviteYes(cKey);
            else voteStatisticsService.addInviteNo(cKey);

            String finalText = "✅ Спасибо за оценку!\n\n"
                               + "📊 Статистика по кандидату " + convertKeyToName(cKey) + ":\n"
                               + voteStatisticsService.getCandidateStatistics(cKey)
                               + "\n─────────────────────";

            editMessageText(chatId, messageId, finalText, null);
            resetUserSession(chatId);
            sendMessage(chatId, "🔄 Для нового голосования введите /start", makeStartButtons());
        }
    }

    private void processScore(Long chatId, String scoreStr, String type) {
        String cKey = userStateService.getCandidate(chatId);
        int score = Integer.parseInt(scoreStr);
        switch (type) {
            case "RESP" -> voteStatisticsService.addResponsibility(cKey, score);
            case "INTR" -> voteStatisticsService.addInterest(cKey, score);
            case "RESF" -> voteStatisticsService.addResultFocus(cKey, score);
        }
    }

    private void resetUserSession(Long chatId) {
        userStateService.setState(chatId, UserState.START);
        userStateService.setCandidate(chatId, null);
    }

    private InlineKeyboardMarkup makeStartButtons() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопки кандидатов
        keyboard.add(List.of(createButton("Виктория 🧑💼", CANDIDATE_VICTORIA)));
        keyboard.add(List.of(createButton("Светлана 👩💻", CANDIDATE_SVETLANA)));
        keyboard.add(List.of(createButton("Александр 👨🔧", CANDIDATE_ALEXANDER)));

        // Сервисные кнопки
        keyboard.add(List.of(
                createButton("📊 Текущая статистика", CURRENT_STATS_CALLBACK),
                createButton("📁 Архив", ARCHIVE_CALLBACK)
        ));

        return new InlineKeyboardMarkup(keyboard);
    }

    private InlineKeyboardButton createButton(String text, String callback) {
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callback);
        return button;
    }

    private InlineKeyboardMarkup makeScoreButtons(String prefix) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            row.add(createButton("⭐ " + i, prefix + i));
        }
        return new InlineKeyboardMarkup(List.of(row));
    }

    private InlineKeyboardMarkup makeInviteButtons() {
        return new InlineKeyboardMarkup(List.of(
                List.of(createButton("✅ Да", "INVITE_YES")),
                List.of(createButton("❌ Нет", "INVITE_NO"))
        ));
    }

    private String convertKeyToName(String key) {
        return switch (key) {
            case CANDIDATE_VICTORIA -> "Виктория 🧑💼";
            case CANDIDATE_SVETLANA -> "Светлана 👩💻";
            case CANDIDATE_ALEXANDER -> "Александр 👨🔧";
            default -> "Неизвестный кандидат";
        };
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(replyMarkup)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editMessageText(Long chatId, Integer messageId, String newText, InlineKeyboardMarkup markup) {
        try {
            execute(EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(newText)
                    .replyMarkup(markup)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}