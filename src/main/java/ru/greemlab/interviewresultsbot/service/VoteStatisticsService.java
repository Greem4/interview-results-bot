package ru.greemlab.interviewresultsbot.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VoteStatisticsService {

    private final AtomicInteger totalResponsibility = new AtomicInteger(0);
    private final AtomicInteger countResponsibility = new AtomicInteger(0);

    private final AtomicInteger totalInterest = new AtomicInteger(0);
    private final AtomicInteger countInterest = new AtomicInteger(0);

    private final AtomicInteger totalResultFocus = new AtomicInteger(0);
    private final AtomicInteger countResultFocus = new AtomicInteger(0);

    private final AtomicInteger yesCount = new AtomicInteger(0);
    private final AtomicInteger noCount = new AtomicInteger(0);

    public void addResponsibility(int score) {
        totalResponsibility.addAndGet(score);
        countResponsibility.incrementAndGet();
    }

    public void addInterest(int score) {
        totalInterest.addAndGet(score);
        countInterest.incrementAndGet();
    }

    public void addResultFocus(int score) {
        totalResultFocus.addAndGet(score);
        countResultFocus.incrementAndGet();
    }

    public void addInviteYes() {
        yesCount.incrementAndGet();
    }

    public void addInviteNo() {
        noCount.incrementAndGet();
    }

    public String getStatisticsMessage() {
        StringBuilder sb;
        sb = new StringBuilder("Итог:\n");
        sb.append(String.format("Ответственность: %d\n", totalResponsibility.get()));
        sb.append(String.format("Интерес к делу: %d\n", totalInterest.get()));
        sb.append(String.format("Направленность на результат: %d\n", totalResultFocus.get()));
        sb.append(String.format("Приглашение на работу:\n Да = %d, Нет = %d\n", yesCount.get(), noCount.get()));
        return sb.toString();
    }
}
