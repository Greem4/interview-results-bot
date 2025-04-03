package ru.greemlab.interviewresultsbot.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VoteStatisticsService {

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
                    "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                    + "┫ 📈 Статистика:\n"
                    + "┣────────────────────────────\n"
                    + "┃ Ответственность: %s\n"
                    + "┃ Интерес к делу: %s\n"
                    + "┃ Направленность на результат: %s\n"
                    + "┣────────────────────────────\n"
                    + "┃ Приглашения: ✅ %d | ❌ %d\n"
                    + "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    formatScore(countResponsibility.get(), totalResponsibility.get()),
                    formatScore(countInterest.get(), totalInterest.get()),
                    formatScore(countResultFocus.get(), totalResultFocus.get()),
                    yesCount.get(),
                    noCount.get()
            );
        }
    }

    public void resetStatistic() {
        statsMap.clear();
    }

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

    private static String formatScore(int count, int total) {
        return count == 0 ? "нет оценок" :
                String.format("%.2f (голосов: %d)", (double) total / count, count);
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
