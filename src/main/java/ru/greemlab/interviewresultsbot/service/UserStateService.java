package ru.greemlab.interviewresultsbot.service;

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

    private final Map<Long, UserState> userStates = new HashMap<>();

    public UserState getState(Long userId) {
        return userStates.getOrDefault(userId, UserState.START);
    }

    public void setState(Long userId, UserState state) {
        userStates.put(userId, state);
    }
}
