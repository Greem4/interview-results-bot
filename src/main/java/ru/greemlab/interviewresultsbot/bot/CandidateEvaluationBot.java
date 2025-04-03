package ru.greemlab.interviewresultsbot.bot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.greemlab.interviewresultsbot.service.DialogStateMachineService;

/**
 * Telegram-бот для оценки кандидатов.
 * Здесь «точка входа», которая принимает Update, а всю логику
 * диалога и состояний отправляет в DialogStateMachineService.
 */
@Slf4j
@Getter
@Component
public class CandidateEvaluationBot extends TelegramLongPollingBot {

    // Эти поля инициализируем вручную в конструкторе
    private final String botUsername;
    private final String botToken;
    private final DialogStateMachineService dialogStateMachineService;

    /**
     * Дополнительный конструктор, позволяющий передать DefaultBotOptions
     * с настроенным пулом потоков (setMaxThreads).
     */
    public CandidateEvaluationBot(
            @Value("${app.bot.username}") String botUsername,
            @Value("${app.bot.token}") String botToken,
            DefaultBotOptions options,
            DialogStateMachineService dialogStateMachineService
    ) {
        // передаём options в родительский конструктор
        super(options);
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.dialogStateMachineService = dialogStateMachineService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Весь «груз» обработки уходит в dialogStateMachineService
        try {
            if (update.hasCallbackQuery()) {
                // Callback запрос (клик по inline-кнопке)
                final var callbackQuery = update.getCallbackQuery();
                // Telegram требует присылать AnswerCallbackQuery, чтобы «убрать» часики
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
     * Утилитный метод: отправка обычного сообщения в чат
     */
    public void sendTextMessage(Long chatId, String text,
                                org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(keyboard)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
        }
    }

    /**
     * Утилитный метод: редактирование существующего сообщения
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
}
