package ru.greemlab.interviewresultsbot.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VoteStatisticsService {

    public static class CandidateStats {

        private AtomicInteger totalResponsibility = new AtomicInteger(0);
        private AtomicInteger countResponsibility = new AtomicInteger(0);

        private AtomicInteger totalInterest = new AtomicInteger(0);
        private AtomicInteger countInterest = new AtomicInteger(0);

        private AtomicInteger totalResultFocus = new AtomicInteger(0);
        private AtomicInteger countResultFocus = new AtomicInteger(0);

        private AtomicInteger yesCount = new AtomicInteger(0);
        private AtomicInteger noCount = new AtomicInteger(0);

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

        public String getStatsText() {
            double avgResp = countResponsibility.get() == 0 ? 0
                    : (double) totalResponsibility.get() / countResponsibility.get();
            double avgInt = countInterest.get() == 0 ? 0
                    : (double) totalInterest.get() / countInterest.get();
            double avgResFocus = countResultFocus.get() == 0 ? 0
                    : (double) totalResultFocus.get() / countResultFocus.get();

            return String.format(
                    "Ответственность: средний балл = %.2f (голосов: %d)\n" +
                    "Интерес к делу: средний балл = %.2f (голосов: %d)\n" +
                    "Направленность на результат: средний балл = %.2f (голосов: %d)\n" +
                    "Пригласили на работу:\n Да = %d, Нет = %d\n",
                    avgResp, countResponsibility.get(),
                    avgInt, countInterest.get(),
                    avgResFocus, countResultFocus.get(),
                    yesCount.get(), noCount.get()
            );
        }
    }

    // Храним статистику для каждого кандидата по имени (ключ)
    private final Map<String, CandidateStats> statsMap = new HashMap<>();

    // Получить объект CandidateStats для конкретного кандидата (создаст, если не было)
    private CandidateStats getStatsForCandidate(String candidateKey) {
        return statsMap.computeIfAbsent(candidateKey, c -> new CandidateStats());
    }

    public void addResponsibility(String candidateKey, int score) {
        getStatsForCandidate(candidateKey).addResponsibility(score);
    }

    public void addInterest(String candidateKey, int score) {
        getStatsForCandidate(candidateKey).addInterest(score);
    }

    public void addResultFocus(String candidateKey, int score) {
        getStatsForCandidate(candidateKey).addResultFocus(score);
    }

    public void addInviteYes(String candidateKey) {
        getStatsForCandidate(candidateKey).addInviteYes();
    }

    public void addInviteNo(String candidateKey) {
        getStatsForCandidate(candidateKey).addInviteNo();
    }

    public String getCandidateStatistics(String candidateKey) {
        CandidateStats stats = statsMap.get(candidateKey);
        if (stats == null) {
            return "Статистика отсутствует.";
        } else {
            return stats.getStatsText();
        }
    }
}
