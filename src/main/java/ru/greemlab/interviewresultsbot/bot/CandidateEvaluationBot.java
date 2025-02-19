package ru.greemlab.interviewresultsbot.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.greemlab.interviewresultsbot.service.UserStateService;
import ru.greemlab.interviewresultsbot.service.VoteStatisticsService;

import java.util.ArrayList;
import java.util.List;

import static ru.greemlab.interviewresultsbot.service.UserStateService.*;

@Getter
@Component
@RequiredArgsConstructor
public class CandidateEvaluationBot extends TelegramLongPollingBot {

    @Value("${app.bot.username}")
    private String botUsername;
    @Value("${app.bot.token}")
    private String botToken;

    private final UserStateService userStateService;
    private final VoteStatisticsService voteStatisticsService;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        var text = message.getText();
        var chatId = message.getChatId();

        if (text.equals("/start")) {
            userStateService.setState(chatId, UserState.START);
            sendMessage(chatId, "Привет! Нажмите, чтобы оценить кандидата:", makeCandidateButton());
        } else {
            // Если пользователь пишет что-то ещё — повторим инструкцию
            sendMessage(chatId, "Для начала введите /start", null);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        var data = callbackQuery.getData();
        var chatId = callbackQuery.getMessage().getChatId();
        var messageId = callbackQuery.getMessage().getMessageId();

        var currentState = userStateService.getState(chatId);

        switch (currentState) {
            case START -> {
                if (data.equals("ivanov")) {
                    userStateService.setState(chatId, UserState.WAITING_RESPONSIBILITY);
                    editMessageText(chatId,
                            messageId,
                            "Оцените ответственность (1-5):", makeScoreButton("RESP"));
                }
            }
            case WAITING_RESPONSIBILITY -> {
                if (data.startsWith("RESP_")) {
                    var score = Integer.parseInt(data.substring("RESP_".length()));
                    voteStatisticsService.addResponsibility(score);

                    userStateService.setState(chatId, UserState.WAITING_INTEREST);
                    editMessageText(chatId,
                            messageId,
                            "Оцените интерес к делу (1-5):", makeScoreButton("INTR"));
                }
            }
            case WAITING_INTEREST -> {
                if (data.startsWith("INTR_")) {
                    var score = Integer.parseInt(data.substring("INTR_".length()));
                    voteStatisticsService.addInterest(score);

                    userStateService.setState(chatId, UserState.WAITING_RESULT_FOCUS);
                    editMessageText(chatId,
                            messageId,
                            "Оцените направленность на результат (1-5):", makeScoreButton("RESF"));
                }
            }
            case WAITING_RESULT_FOCUS -> {
                if (data.startsWith("RESF_")) {
                    var score = Integer.parseInt(data.substring("RESF_".length()));
                    voteStatisticsService.addResultFocus(score);
                }

                userStateService.setState(chatId, UserState.WAITING_INVITE);
                editMessageText(chatId,
                        messageId,
                        "Пригласите данного кандидата на работу?",
                        makeInviteButtons());
            }
            case WAITING_INVITE -> {
                if (data.startsWith("INVITE_YES")) {
                    voteStatisticsService.addInviteYes();
                    finishPoll(chatId, messageId);
                }
                if (data.startsWith("INVITE_NO")) {
                    voteStatisticsService.addInviteNo();
                    finishPoll(chatId, messageId);
                }
            }


        }
    }

    private void finishPoll(Long chatId, Integer messageId) {
        userStateService.setState(chatId, UserState.FINISHED);

        var statMessage = voteStatisticsService.getStatisticsMessage();
        editMessageText(chatId, messageId,
                "Спасибо за вашу оценку!\n\n" + statMessage, null);
    }

    private InlineKeyboardMarkup makeCandidateButton() {
        var btn = new InlineKeyboardButton();
        btn.setText("Иванов И.И.");
        btn.setCallbackData("ivanov");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(btn);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        var markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }


    private InlineKeyboardMarkup makeScoreButton(String prefix) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            var btn = new InlineKeyboardButton();
            btn.setText(String.valueOf(i));
            btn.setCallbackData(prefix + "_" + i);
            row.add(btn);
        }
        keyboard.add(row);

        var markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup makeInviteButtons() {
        var yesBtn = new InlineKeyboardButton();
        yesBtn.setText("Да");
        yesBtn.setCallbackData("INVITE_YES");

        var noBtn = new InlineKeyboardButton();
        noBtn.setText("Нет");
        noBtn.setCallbackData("INVITE_NO");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(yesBtn);
        row.add(noBtn);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        var markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        var msg = new SendMessage();
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

    private void editMessageText(Long chatId, Integer messageId, String newText, InlineKeyboardMarkup replyMarkup) {
        var editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        if (replyMarkup != null) {
            editMessage.setReplyMarkup(replyMarkup);
        }
        try {
            execute(editMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
