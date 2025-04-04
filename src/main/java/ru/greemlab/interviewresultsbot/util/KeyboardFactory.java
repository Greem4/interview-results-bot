package ru.greemlab.interviewresultsbot.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.greemlab.interviewresultsbot.service.CallbackCommands;
import ru.greemlab.interviewresultsbot.service.CandidateConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для создания клавиатур (InlineKeyboardMarkup).
 */
public class KeyboardFactory {

    /**
     * Главное меню (выбор кандидата + просмотр статистики/архива)
     */
    public static InlineKeyboardMarkup buildMainMenuKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопки выбора кандидатов
        rows.add(List.of(createButton("Виктория", CandidateConstants.VICTORIA)));
        rows.add(List.of(createButton("Светлана", CandidateConstants.SVETLANA)));
        rows.add(List.of(createButton("Александр", CandidateConstants.ALEXANDER)));

        // «Сервисные» кнопки
        rows.add(List.of(
                createButton("📊 Текущая статистика", CallbackCommands.CURRENT_STATS),
                createButton("📁 Архив", CallbackCommands.ARCHIVE)
        ));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Клавиатура для выбора оценки (1..5)
     */
    public static InlineKeyboardMarkup buildRatingButtons(String prefix) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            row.add(createButton("⭐ " + i, prefix + i));
        }
        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();
    }

    /**
     * Клавиатура «Пригласить / Не пригласить»
     */
    public static InlineKeyboardMarkup buildInviteKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                createButton("✅ Да", CallbackCommands.INVITE_YES),
                createButton("❌ Нет", CallbackCommands.INVITE_NO)
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
