package ru.greemlab.interviewresultsbot.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {

    public enum UserState {
        START,
        WAITING_RESPONSIBILITY,
        WAITING_INTEREST,
        WAITING_RESULT_FOCUS,
        WAITING_INVITE
    }

    @Getter
    @Setter
    public static class UserSession {
        private UserState state = UserState.START;
        private String candidateKey;

        // Для хранения, голосовал ли уже пользователь за конкретного кандидата
        private final Map<String, Boolean> votedCandidates = new ConcurrentHashMap<>();

        // ID «временного» сообщения (с шагами голосования),
        // чтобы редактировать/удалять его при необходимости
        private Integer tempMessageId;

        public boolean hasVotedFor(String candidate) {
            return votedCandidates.getOrDefault(candidate, false);
        }

        public void markVoted(String candidate) {
            votedCandidates.put(candidate, true);
        }
    }

    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    public UserSession getOrCreateSession(Long chatId) {
        return userSessions.computeIfAbsent(chatId, id -> new UserSession());
    }

    public UserState getState(Long chatId) {
        return getOrCreateSession(chatId).getState();
    }

    public void setState(Long chatId, UserState state) {
        getOrCreateSession(chatId).setState(state);
    }

    public String getCandidate(Long chatId) {
        return getOrCreateSession(chatId).getCandidateKey();
    }

    public void setCandidate(Long chatId, String candidateKey) {
        getOrCreateSession(chatId).setCandidateKey(candidateKey);
    }

    /**
     * Сбросить состояние конкретного пользователя (в START).
     */
    public void resetState(Long chatId) {
        var session = userSessions.get(chatId);
        if (session != null) {
            session.setState(UserState.START);
            session.setCandidateKey(null);
            session.setTempMessageId(null);
        }
    }

    /**
     * Полный сброс всех сессий.
     */
    public void resetAllSessions() {
        userSessions.clear();
    }
}
