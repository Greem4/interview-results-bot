package ru.greemlab.interviewresultsbot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис работы с архивными данными кандидатов
 */
@Service
public class ArchiveCandidatesService {

    // Хранилище архивных данных
    private final Map<String, VoteStatisticsService.CandidateStats> archive = new ConcurrentHashMap<>();

    /**
     * Инициализация демонстрационных данных
     */
    @PostConstruct
    public void init() {
        addSampleCandidate(
                "Петров П.П.",
                new int[]{4,5}, // Ответственность
                new int[]{4,3},  // Интерес
                new int[]{5,5},  // Результативность
                2, 0             // Приглашения
        );
        addSampleCandidate(
                "Сидорова К.К.",
                new int[]{3},
                new int[]{3,3,4},
                new int[]{2,3},
                0, 1
        );
        addSampleCandidate(
                "Иванов С.С.",
                new int[]{5,5},
                new int[]{5},
                new int[]{4,4,4},
                3, 0
        );
        addSampleCandidate(
                "Ковалёва Л.Л.",
                new int[]{3,4},
                new int[]{4,5},
                new int[]{5},
                1, 1
        );
        addSampleCandidate(
                "Самойлов Р.Р.",
                new int[]{5,5,5},
                new int[]{5,5},
                new int[]{5},
                2, 0
        );
    }

    /**
     * Добавление кандидата в архив
     */
    private void addSampleCandidate(String name, int[] responsibility,
                                    int[] interest, int[] resultFocus,
                                    int yesCount, int noCount) {
        VoteStatisticsService.CandidateStats stats = new VoteStatisticsService.CandidateStats();

        for (int r : responsibility) stats.addResponsibility(r);
        for (int i : interest) stats.addInterest(i);
        for (int f : resultFocus) stats.addResultFocus(f);

        for (int i = 0; i < yesCount; i++) stats.addInviteYes();
        for (int i = 0; i < noCount; i++) stats.addInviteNo();

        archive.put(name, stats);
    }

    /**
     * Получение сводки по архиву
     */
    public String getArchiveSummary() {
        if (archive.isEmpty()) return "📭 Архив пуст";

        StringBuilder sb = new StringBuilder("📁 Архив соискателей:\n\n");
        archive.forEach((name, stats) ->
                sb.append(formatCandidateEntry(name, stats))
        );
        return sb.toString();
    }

    private String formatCandidateEntry(String name, VoteStatisticsService.CandidateStats stats) {
        return String.format(
                "▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄\n" +
                "👤 Кандидат: %s\n%s\n\n",
                name,
                stats.getStatsText()
        );
    }
}