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
                    "Добро пожаловать! Выберите кандидата для оценки или посмотрите архив:",
                    makeStartButtons()
            );
        } else if (text.equals("/restart")) {
            userStateService.resetAllSessions();
            voteStatisticsService.resetStatistic();
            sendMessage(chatId, "✅ Все данные сброшены. Начните заново с /start.", null);
        } else {
            sendMessage(chatId, "Введите /start, чтобы начать.", null);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        // Отвечаем на callback, чтобы убрать "часики"
        try {
            execute(new AnswerCallbackQuery(callbackQuery.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        UserState currentState = userStateService.getState(chatId);

        // Если нажали "Архив соискателей"
        if (data.equals(ARCHIVE_CALLBACK)) {
            String archiveText = archiveCandidatesService.getArchiveSummary();
            editMessageText(chatId, messageId, archiveText, null);
            return;
        }

        if (data.equals(CURRENT_STATS_CALLBACK)) {
            var statsText = voteStatisticsService.getAllCandidatesStatistics();
            editMessageText(chatId, messageId, statsText, null);
            return;
        }

        switch (currentState) {
            case START:
                if (data.equals(CANDIDATE_VICTORIA) ||
                    data.equals(CANDIDATE_ALEXANDER) ||
                    data.equals(CANDIDATE_SVETLANA)) {

                    userStateService.setCandidate(chatId, data);
                    userStateService.setState(chatId, UserState.WAITING_RESPONSIBILITY);

                    String candidateName = convertKeyToName(data);
                    editMessageText(
                            chatId,
                            messageId,
                            "Вы выбрали кандидата: " + candidateName +
                            "\n\nОцените ответственность (1..5):",
                            makeScoreButtons("RESP_")
                    );
                }
                break;

            case WAITING_RESPONSIBILITY:
                if (data.startsWith("RESP_")) {
                    int score = Integer.parseInt(data.substring("RESP_".length()));
                    String cKey = userStateService.getCandidate(chatId);
                    voteStatisticsService.addResponsibility(cKey, score);

                    userStateService.setState(chatId, UserState.WAITING_INTEREST);
                    editMessageText(chatId, messageId,
                            "Оцените интерес к делу (1..5):",
                            makeScoreButtons("INTR_"));
                }
                break;

            case WAITING_INTEREST:
                if (data.startsWith("INTR_")) {
                    int score = Integer.parseInt(data.substring("INTR_".length()));
                    String cKey = userStateService.getCandidate(chatId);
                    voteStatisticsService.addInterest(cKey, score);

                    userStateService.setState(chatId, UserState.WAITING_RESULT_FOCUS);
                    editMessageText(chatId, messageId,
                            "Оцените направленность на результат (1..5):",
                            makeScoreButtons("RESF_"));
                }
                break;

            case WAITING_RESULT_FOCUS:
                if (data.startsWith("RESF_")) {
                    int score = Integer.parseInt(data.substring("RESF_".length()));
                    String cKey = userStateService.getCandidate(chatId);
                    voteStatisticsService.addResultFocus(cKey, score);

                    userStateService.setState(chatId, UserState.WAITING_INVITE);
                    editMessageText(chatId, messageId,
                            "Пригласили ли Вы данного кандидата на работу?",
                            makeInviteButtons());
                }
                break;

            case WAITING_INVITE:
                if (data.equals("INVITE_YES") || data.equals("INVITE_NO")) {
                    String cKey = userStateService.getCandidate(chatId);
                    if (data.equals("INVITE_YES")) {
                        voteStatisticsService.addInviteYes(cKey);
                    } else {
                        voteStatisticsService.addInviteNo(cKey);
                    }

                    // Получаем статистику и формируем итоговое сообщение
                    String stats = voteStatisticsService.getCandidateStatistics(cKey);
                    String finalText = "Спасибо за вашу оценку!\n\nСтатистика по кандидату:\n"
                                       + convertKeyToName(cKey) + "\n\n"
                                       + stats;
                    // Отредактируем сообщение с результатами
                    editMessageText(chatId, messageId, finalText, null);

                    // Сбрасываем состояние в старт, чтобы новые нажатия обрабатывались
                    userStateService.setState(chatId, UserState.START);
                    userStateService.setCandidate(chatId, null);

                    // Отправляем новое сообщение со стартовыми кнопками
                    sendMessage(chatId, "Для нового голосования введите /start или нажмите кнопку ниже:",
                            makeStartButtons());
                }
                break;

            case FINISHED:
            default:
                // Игнорируем любые другие нажатия
                break;
        }
    }

    // Фабрика стартовых кнопок
    private InlineKeyboardMarkup makeStartButtons() {
        var btnVictoria = new InlineKeyboardButton("Виктория");
        btnVictoria.setCallbackData(CANDIDATE_VICTORIA);

        var btnSvetlana = new InlineKeyboardButton("Светлана");
        btnSvetlana.setCallbackData(CANDIDATE_SVETLANA);

        var btnAlexander = new InlineKeyboardButton("Александр");
        btnAlexander.setCallbackData(CANDIDATE_ALEXANDER);

        var btnCurrentsStats = new InlineKeyboardButton("Текущие кандидаты");
        btnCurrentsStats.setCallbackData(CURRENT_STATS_CALLBACK);

        var btnArchive = new InlineKeyboardButton("Архив соискателей");
        btnArchive.setCallbackData(ARCHIVE_CALLBACK);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(List.of(btnVictoria));
        keyboard.add(List.of(btnSvetlana));
        keyboard.add(List.of(btnAlexander));
        keyboard.add(List.of(btnCurrentsStats));
        keyboard.add(List.of(btnArchive));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    // Фабрика кнопок-оценок (1..5)
    private InlineKeyboardMarkup makeScoreButtons(String prefix) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton btn = new InlineKeyboardButton(String.valueOf(i));
            btn.setCallbackData(prefix + i);
            row.add(btn);
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }

    // Фабрика кнопок "Да"/"Нет"
    private InlineKeyboardMarkup makeInviteButtons() {
        InlineKeyboardButton yesBtn = new InlineKeyboardButton("Да");
        yesBtn.setCallbackData("INVITE_YES");

        InlineKeyboardButton noBtn = new InlineKeyboardButton("Нет");
        noBtn.setCallbackData("INVITE_NO");

        List<InlineKeyboardButton> row = List.of(yesBtn, noBtn);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }

    // Преобразуем ключ в отображаемое имя
    private String convertKeyToName(String key) {
        switch (key) {
            case CANDIDATE_VICTORIA:
                return "Виктория";
            case CANDIDATE_SVETLANA:
                return "Светлана";
            case CANDIDATE_ALEXANDER:
                return "Александр";
            default:
                return "Неизвестно";
        }
    }

    // Отправка нового сообщения
    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        if (replyMarkup != null) {
            msg.setReplyMarkup(replyMarkup);
        }
        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Редактирование существующего сообщения
    private void editMessageText(Long chatId, Integer messageId, String newText, InlineKeyboardMarkup markup) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setText(newText);
        if (markup != null) {
            edit.setReplyMarkup(markup);
        }
        try {
            execute(edit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

