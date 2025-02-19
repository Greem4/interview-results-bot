package ru.greemlab.interviewresultsbot.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VoteStatisticsService {

    // Суммы оценок и количество голосов по трём критериям
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
        var sb = new StringBuilder();
        sb.append("Итог:\n");

        double avgResp = countResponsibility.get() == 0 ? 0
                : (double) totalResponsibility.get() / countResponsibility.get();
        double avgInt = countInterest.get() == 0 ? 0
                : (double) totalInterest.get() / countInterest.get();
        double avgResFocus = countResultFocus.get() == 0 ? 0
                : (double) totalResultFocus.get() / countResultFocus.get();

        sb.append(String.format("Ответственность: средняя оценка = %.2f, всего голосов = %d\n",
                avgResp, countResponsibility.get()));
        sb.append(String.format("Интерес к делу: средняя оценка = %.2f, всего голосов = %d\n",
                avgInt, countInterest.get()));
        sb.append(String.format("Направленность на результат: средняя оценка = %.2f, всего голосов = %d\n",
                avgResFocus, countResultFocus.get()));
        sb.append(String.format("Пригласили ли на работу: Да = %d, Нет = %d\n",
                yesCount.get(), noCount.get()));

        return sb.toString();
    }
}
