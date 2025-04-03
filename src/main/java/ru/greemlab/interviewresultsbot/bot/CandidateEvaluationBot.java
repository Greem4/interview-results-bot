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
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.greemlab.interviewresultsbot.service.DialogStateMachineService;

/**
 * Telegram-бот для оценки кандидатов.
 * Здесь **только** «точка входа», которая принимает Update, а всю логику
 * диалога и состояний отправляет в DialogStateMachineService.
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class CandidateEvaluationBot extends TelegramLongPollingBot {

    @Value("${app.bot.username}")
    private String botUsername;

    @Value("${app.bot.token}")
    private String botToken;

    private final DialogStateMachineService dialogStateMachineService;

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
    public void sendTextMessage(Long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
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
    public void editMessage(Long chatId, Integer messageId, String newText, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
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
