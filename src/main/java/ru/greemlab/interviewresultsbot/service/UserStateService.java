package ru.greemlab.interviewresultsbot.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис управления состояниями диалогов для каждого chatId.
 */
@Service
public class UserStateService {

    /**
     * Состояния диалога
     */
    public enum UserState {
        START,
        WAITING_RESPONSIBILITY,
        WAITING_INTEREST,
        WAITING_RESULT_FOCUS,
        WAITING_INVITE
    }

    /**
     * Сессия пользователя
     */
    @Getter
    @Setter
    public static class UserSession {
        private UserState state = UserState.START;
        private String candidateKey;
    }

    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    /**
     * Получить текущее состояние пользователя.
     */
    public UserState getState(Long chatId) {
        return getOrCreateSession(chatId).getState();
    }

    /**
     * Установить состояние пользователя.
     */
    public void setState(Long chatId, UserState state) {
        getOrCreateSession(chatId).setState(state);
    }

    /**
     * Получить выбранного кандидата.
     */
    public String getCandidate(Long chatId) {
        return getOrCreateSession(chatId).getCandidateKey();
    }

    /**
     * Установить выбранного кандидата.
     */
    public void setCandidate(Long chatId, String candidateKey) {
        getOrCreateSession(chatId).setCandidateKey(candidateKey);
    }

    /**
     * Сбросить состояние данного пользователя в START.
     */
    public void resetState(Long chatId) {
        UserSession session = userSessions.get(chatId);
        if (session != null) {
            session.setState(UserState.START);
            session.setCandidateKey(null);
        }
    }

    /**
     * Сбросить все сессии пользователей (например, при /restart).
     */
    public void resetAllSessions() {
        userSessions.clear();
    }

    private UserSession getOrCreateSession(Long chatId) {
        return userSessions.computeIfAbsent(chatId, id -> new UserSession());
    }
}
