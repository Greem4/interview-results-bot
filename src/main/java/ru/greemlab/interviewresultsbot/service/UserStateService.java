package ru.greemlab.interviewresultsbot.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис управления состояниями пользователей
 */
@Service
public class UserStateService {

    /**
     * Состояния диалога с пользователем
     */
    public enum UserState {
        START,
        WAITING_RESPONSIBILITY,
        WAITING_INTEREST,
        WAITING_RESULT_FOCUS,
        WAITING_INVITE,
        FINISHED
    }

    /**
     * Внутренний класс для хранения данных сессии пользователя
     */
    @Getter
    @Setter
    public static class UserSession {
        private UserState state = UserState.START;
        private String candidateKey;
    }

    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    /**
     * Сброс состояния конкретного пользователя
     * @param chatId ID чата пользователя
     */
    public void resetState(Long chatId) {
        UserSession session = userSessions.get(chatId);
        if (session != null) {
            session.setState(UserState.START);
            session.setCandidateKey(null);
        }
    }

    /**
     * Получение текущего состояния пользователя
     * @param chatId ID чата пользователя
     * @return Текущее состояние
     */
    public UserState getState(Long chatId) {
        return getOrCreateSession(chatId).getState();
    }

    /**
     * Установка нового состояния
     * @param chatId ID чата пользователя
     * @param state Новое состояние
     */
    public void setState(Long chatId, UserState state) {
        getOrCreateSession(chatId).setState(state);
    }

    /**
     * Получение выбранного кандидата
     * @param chatId ID чата пользователя
     * @return Ключ выбранного кандидата
     */
    public String getCandidate(Long chatId) {
        return getOrCreateSession(chatId).getCandidateKey();
    }

    /**
     * Установка выбранного кандидата
     * @param chatId ID чата пользователя
     * @param candidateKey Ключ кандидата
     */
    public void setCandidate(Long chatId, String candidateKey) {
        getOrCreateSession(chatId).setCandidateKey(candidateKey);
    }

    /**
     * Сброс всех сессий
     */
    public void resetAllSessions() {
        userSessions.clear();
    }

    private UserSession getOrCreateSession(Long chatId) {
        return userSessions.computeIfAbsent(chatId, k -> new UserSession());
    }
}