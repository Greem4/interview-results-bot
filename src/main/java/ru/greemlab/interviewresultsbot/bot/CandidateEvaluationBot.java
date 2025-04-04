package ru.greemlab.interviewresultsbot.bot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.greemlab.interviewresultsbot.service.DialogStateMachineService;

@Slf4j
@Getter
@Component
public class CandidateEvaluationBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final DialogStateMachineService dialogStateMachineService;

    public CandidateEvaluationBot(
            @Value("${app.bot.username}") String botUsername,
            @Value("${app.bot.token}") String botToken,
            DefaultBotOptions options,
            DialogStateMachineService dialogStateMachineService
    ) {
        super(options);
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.dialogStateMachineService = dialogStateMachineService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                // Callback запрос (клик по inline-кнопке)
                final var callbackQuery = update.getCallbackQuery();
                // Убираем "часики" на нажатой кнопке
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .build());
                dialogStateMachineService.processCallbackQuery(this, callbackQuery);
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                dialogStateMachineService.processTextMessage(this, update.getMessage());
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения: {}", e.getMessage(), e);
        }
    }

    /**
     * Утилитный метод: отправка нового сообщения в чат.
     */
    public Integer sendTextMessage(Long chatId, String text,
                                   org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        try {
            var send = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(keyboard)
                    .build();
            var msg = execute(send);
            return msg.getMessageId(); // Возвращаем ID отправленного сообщения
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Редактирование существующего сообщения (меняем текст и/или клавиатуру).
     */
    public void editMessage(Long chatId, Integer messageId, String newText,
                            org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        try {
            execute(EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(newText)
                    .replyMarkup(keyboard)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при редактировании сообщения: {}", e.getMessage(), e);
        }
    }

    /**
     * Удаление сообщения из чата по его ID.
     */
    public void deleteMessage(Long chatId, Integer messageId) {
        try {
            execute(DeleteMessage.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при удалении сообщения: {}", e.getMessage(), e);
        }
    }
}
