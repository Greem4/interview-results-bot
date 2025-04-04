package ru.greemlab.interviewresultsbot.service;

import java.util.List;

/**
 * Храним «ключи» наших кандидатов и вспомогательный метод для вывода их «читаемого» имени.
 */
public final class CandidateConstants {

    private CandidateConstants() {}

    public static final String VICTORIA = "victoria";
    public static final String ALEXANDER = "alexander";
    public static final String SVETLANA = "svetlana";

    public static final List<String> ALL = List.of(VICTORIA, ALEXANDER, SVETLANA);

    public static String getCandidateName(String key) {
        return switch (key) {
            case VICTORIA -> "Виктория ";
            case SVETLANA -> "Светлана ";
            case ALEXANDER -> "Александр ";
            default -> "Неизвестный кандидат";
        };
    }
}
