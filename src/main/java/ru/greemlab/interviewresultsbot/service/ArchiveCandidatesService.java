package ru.greemlab.interviewresultsbot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис работы с архивными данными кандидатов.
 * В реальном приложении вы можете хранить это в БД.
 */
@Service
public class ArchiveCandidatesService {

    private final Map<String, VoteStatisticsService.CandidateStats> archive = new ConcurrentHashMap<>();

    /**
     * Пример инициализации демо-данных в архиве.
     */
    @PostConstruct
    public void init() {
        addSampleCandidate("Петров П.П.", new int[]{4,5}, new int[]{4,3}, new int[]{5,5}, 2, 0);
        addSampleCandidate("Сидорова К.К.", new int[]{3}, new int[]{3,3,4}, new int[]{2,3}, 0, 1);
        addSampleCandidate("Иванов С.С.", new int[]{5,5}, new int[]{5}, new int[]{4,4,4}, 3, 0);
        addSampleCandidate("Ковалёва Л.Л.", new int[]{3,4}, new int[]{4,5}, new int[]{5}, 1, 1);
        addSampleCandidate("Самойлов Р.Р.", new int[]{5,5,5}, new int[]{5,5}, new int[]{5}, 2, 0);
    }

    public String getArchiveSummary() {
        if (archive.isEmpty()) {
            return "📭 Архив пуст";
        }
        StringBuilder sb = new StringBuilder("📁 Архив соискателей:\n\n");
        archive.forEach((name, stats) -> {
            sb.append("▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄\n")
                    .append("👤 Кандидат: ").append(name).append("\n")
                    .append(stats.getStatsText()).append("\n\n");
        });
        return sb.toString();
    }

    private void addSampleCandidate(String name,
                                    int[] responsibilityScores,
                                    int[] interestScores,
                                    int[] focusScores,
                                    int yesCount,
                                    int noCount) {
        var stats = new VoteStatisticsService.CandidateStats();
        for (int r : responsibilityScores) stats.addResponsibility(r);
        for (int i : interestScores) stats.addInterest(i);
        for (int f : focusScores) stats.addResultFocus(f);
        for (int i = 0; i < yesCount; i++) stats.addInviteYes();
        for (int i = 0; i < noCount; i++) stats.addInviteNo();
        archive.put(name, stats);
    }
}
