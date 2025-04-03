package ru.greemlab.interviewresultsbot.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис сбора и анализа статистики голосований
 */
@Service
public class VoteStatisticsService {

    /**
     * Внутренний класс для хранения статистики по кандидату
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

        /**
         * Добавление оценки ответственности
         */
        public void addResponsibility(int score) {
            totalResponsibility.addAndGet(score);
            countResponsibility.incrementAndGet();
        }

        /**
         * Добавление оценки интереса
         */
        public void addInterest(int score) {
            totalInterest.addAndGet(score);
            countInterest.incrementAndGet();
        }

        /**
         * Добавление оценки результативности
         */
        public void addResultFocus(int score) {
            totalResultFocus.addAndGet(score);
            countResultFocus.incrementAndGet();
        }

        /**
         * Увеличение счетчика приглашений
         */
        public void addInviteYes() {
            yesCount.incrementAndGet();
        }

        /**
         * Увеличение счетчика отказов
         */
        public void addInviteNo() {
            noCount.incrementAndGet();
        }

        /**
         * Форматирование статистики для отображения
         */
        public String getStatsText() {
            return String.format(
                    "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "┫ 📈 Статистика:\n" +
                    "┣────────────────────────────\n" +
                    "┃ Ответственность: %s\n" +
                    "┃ Интерес к делу: %s\n" +
                    "┃ Направленность на результат: %s\n" +
                    "┣────────────────────────────\n" +
                    "┃ Приглашения: ✅ %d | ❌ %d\n" +
                    "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    formatScore(countResponsibility.get(), totalResponsibility.get()),
                    formatScore(countInterest.get(), totalInterest.get()),
                    formatScore(countResultFocus.get(), totalResultFocus.get()),
                    yesCount.get(),
                    noCount.get()
            );
        }

        private String formatScore(int count, int total) {
            return count == 0 ? "нет оценок" :
                    String.format("%.2f (голосов: %d)", (double) total / count, count);
        }
    }

    // Хранилище статистики
    private final Map<String, CandidateStats> statsMap = new ConcurrentHashMap<>();

    /**
     * Сброс всей статистики
     */
    public void resetStatistic() {
        statsMap.clear();
    }

    /**
     * Получение статистики по всем кандидатам
     */
    public String getAllCandidatesStatistics() {
        if (statsMap.isEmpty()) return "📭 Нет данных о голосованиях";

        StringBuilder sb = new StringBuilder("📊 Текущая статистика:\n\n");
        statsMap.forEach((key, stats) ->
                sb.append("▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄\n")
                        .append("👤 Кандидат: ").append(convertKeyName(key)).append("\n")
                        .append(stats.getStatsText()).append("\n\n")
        );
        return sb.toString();
    }

    /**
     * Добавление оценки ответственности
     */
    public void addResponsibility(String candidateKey, int score) {
        getStatsForCandidate(candidateKey).addResponsibility(score);
    }

    /**
     * Добавление оценки интереса
     */
    public void addInterest(String candidateKey, int score) {
        getStatsForCandidate(candidateKey).addInterest(score);
    }

    /**
     * Добавление оценки результативности
     */
    public void addResultFocus(String candidateKey, int score) {
        getStatsForCandidate(candidateKey).addResultFocus(score);
    }

    /**
     * Добавление голоса за приглашение
     */
    public void addInviteYes(String candidateKey) {
        getStatsForCandidate(candidateKey).addInviteYes();
    }

    /**
     * Добавление голоса против приглашения
     */
    public void addInviteNo(String candidateKey) {
        getStatsForCandidate(candidateKey).addInviteNo();
    }

    /**
     * Получение статистики по конкретному кандидату
     */
    public String getCandidateStatistics(String candidateKey) {
        CandidateStats stats = statsMap.get(candidateKey);
        return stats != null ? stats.getStatsText() : "Статистика отсутствует.";
    }

    private CandidateStats getStatsForCandidate(String candidateKey) {
        return statsMap.computeIfAbsent(candidateKey, k -> new CandidateStats());
    }

    private String convertKeyName(String key) {
        return switch (key) {
            case "victoria" -> "Виктория";
            case "alexander" -> "Александр";
            case "svetlana" -> "Светлана";
            default -> "Неизвестный кандидат";
        };
    }
}