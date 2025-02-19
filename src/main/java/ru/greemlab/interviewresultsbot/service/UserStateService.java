package ru.greemlab.interviewresultsbot.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserStateService {

    public enum UserState {
        START,
        WAITING_RESPONSIBILITY,
        WAITING_INTEREST,
        WAITING_RESULT_FOCUS,
        WAITING_INVITE,
        FINISHED
    }

    @Getter
    @Setter
    public static class UserSession {
        private UserState state = UserState.START;
        private String candidateKey; // "victoria" / "alexander" / "svetlana" / null

    }

    private final Map<Long, UserSession> userSessions = new HashMap<>();

    private UserSession getUserSession(Long chatId) {
        return userSessions.computeIfAbsent(chatId, c -> new UserSession());
    }

    public UserState getState(Long chatId) {
        return getUserSession(chatId).getState();
    }

    public void setState(Long chatId, UserState state) {
        getUserSession(chatId).setState(state);
    }

    public String getCandidate(Long chatId) {
        return getUserSession(chatId).getCandidateKey();
    }

    public void setCandidate(Long chatId, String candidateKey) {
        getUserSession(chatId).setCandidateKey(candidateKey);
    }
}
