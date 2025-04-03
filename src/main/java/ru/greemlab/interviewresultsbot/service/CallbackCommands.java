package ru.greemlab.interviewresultsbot.service;

/**
 * Храним callbackData, используемые в Inline-кнопках.
 */
public final class CallbackCommands {

    private CallbackCommands() {}

    public static final String CURRENT_STATS = "current_stats";
    public static final String ARCHIVE = "archive";

    // Префиксы для оценки
    public static final String RESP_PREFIX = "RESP_";
    public static final String INTR_PREFIX = "INTR_";
    public static final String RESF_PREFIX = "RESF_";

    // Решение о приглашении
    public static final String INVITE_YES = "INVITE_YES";
    public static final String INVITE_NO = "INVITE_NO";
}
