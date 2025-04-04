package ru.greemlab.interviewresultsbot.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис сбора и анализа статистики по кандидатам.
 */
@Service
public class VoteStatisticsService {

    /**
     * Вложенный класс: хранит суммарные оценки и счётчики.
     */
    public static class CandidateStats {
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

        public String getStatsText() {
            return String.format(
                    """
                    
                    📈 Статистика:
                    
                    Ответственность: %s
                    Интерес: %s
                    Результативность: %s
                    
                    Приглашения: ✅ %d | ❌ %d
                    """,
                    formatScore(countResponsibility.get(), totalResponsibility.get()),
                    formatScore(countInterest.get(), totalInterest.get()),
                    formatScore(countResultFocus.get(), totalResultFocus.get()),
                    yesCount.get(), noCount.get()
            );
        }

        private String formatScore(int count, int total) {
            if (count == 0) {
                return "нет оценок";
            }
            double avg = (double) total / count;
            return String.format("%.2f (голосов: %d)", avg, count);
        }
    }

    private final Map<String, CandidateStats> statsMap = new ConcurrentHashMap<>();

    /**
     * Сброс всей статистики (например, при /restart).
     */
    public void resetStatistic() {
        statsMap.clear();
    }

    /**
     * Статистика по всем кандидатам.
     */
    public String getAllCandidatesStatistics() {
        if (statsMap.isEmpty()) {
            return "📭 Нет данных о голосованиях";
        }
        StringBuilder sb = new StringBuilder("📊 Текущая статистика:\n\n");
        statsMap.forEach((candidateKey, stats) -> {
            sb.append("👤 Кандидат: ")
                    .append(CandidateConstants.getCandidateName(candidateKey))
                    .append("\n")
                    .append(stats.getStatsText())
                    .append("\n\n");
        });
        return sb.toString();
    }

    /**
     * Статистика по конкретному кандидату.
     */
    public String getCandidateStatistics(String candidateKey) {
        CandidateStats stats = statsMap.get(candidateKey);
        if (stats == null) {
            return "Статистика отсутствует.";
        }
        return stats.getStatsText();
    }

    /**
     * Возвращает (или создаёт) объект статистики по кандидату.
     */
    private CandidateStats getOrCreate(String candidateKey) {
        return statsMap.computeIfAbsent(candidateKey, k -> new CandidateStats());
    }

    public void addResponsibility(String candidateKey, int score) {
        getOrCreate(candidateKey).addResponsibility(score);
    }

    public void addInterest(String candidateKey, int score) {
        getOrCreate(candidateKey).addInterest(score);
    }

    public void addResultFocus(String candidateKey, int score) {
        getOrCreate(candidateKey).addResultFocus(score);
    }

    public void addInviteYes(String candidateKey) {
        getOrCreate(candidateKey).addInviteYes();
    }

    public void addInviteNo(String candidateKey) {
        getOrCreate(candidateKey).addInviteNo();
    }
}
