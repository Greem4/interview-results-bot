package ru.greemlab.interviewresultsbot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// Допустим, мы хотим использовать ту же CandidateStats, что и в VoteStatisticsService.
@Service
public class ArchiveCandidatesService {

    // Ключ: ФИО / имя кандидата
    // Значение: статистика (оценки, кол-во голосов "да/нет" и т.д.)
    private final Map<String, VoteStatisticsService.CandidateStats> archiveMap = new HashMap<>();

    /**
     * Метод, который заполнит архив "фиктивными" (но более реалистичными) результатами.
     * Он будет вызван один раз при старте приложения.
     */
    @PostConstruct
    public void initArchive() {
        // Пример: добавим 5 архивных кандидатов с некоторыми оценками.
        addArchivedCandidate("Петров П.П.", new int[]{4,5}, new int[]{4,3}, new int[]{5,5}, 2, 0);
        addArchivedCandidate("Сидорова К.К.", new int[]{3},   new int[]{3,3,4}, new int[]{2,3}, 0, 1);
        addArchivedCandidate("Иванов С.С.",   new int[]{5,5}, new int[]{5},     new int[]{4,4,4}, 3, 0);
        addArchivedCandidate("Ковалёва Л.Л.",new int[]{3,4}, new int[]{4,5},   new int[]{5}, 1, 1);
        addArchivedCandidate("Самойлов Р.Р.",new int[]{5,5,5}, new int[]{5,5}, new int[]{5}, 2, 0);
    }

    /**
     * Вспомогательный метод для заполнения данных.
     * @param name           ФИО
     * @param respScores     массив оценок ответственности
     * @param interestScores массив оценок интереса
     * @param focusScores    массив оценок направленности на результат
     * @param yesCount       сколько раз за него проголосовали "пригласить"
     * @param noCount        сколько раз проголосовали "не приглашать"
     */
    private void addArchivedCandidate(String name,
                                      int[] respScores,
                                      int[] interestScores,
                                      int[] focusScores,
                                      int yesCount,
                                      int noCount) {

        VoteStatisticsService.CandidateStats stats = new VoteStatisticsService.CandidateStats();
        // Заполним оценки
        for (int r : respScores) {
            stats.addResponsibility(r);
        }
        for (int i : interestScores) {
            stats.addInterest(i);
        }
        for (int f : focusScores) {
            stats.addResultFocus(f);
        }
        // Установим голоса за приглашение/не приглашение
        for (int i = 0; i < yesCount; i++) {
            stats.addInviteYes();
        }
        for (int i = 0; i < noCount; i++) {
            stats.addInviteNo();
        }

        archiveMap.put(name, stats);
    }

    /**
     * Вернёт текстовую сводку по всем архивным кандидатам.
     */
    public String getArchiveSummary() {
        if (archiveMap.isEmpty()) {
            return "Архив соискателей пока пуст.";
        }

        // Формируем человекочитаемую статистику
        StringBuilder sb = new StringBuilder("АРХИВ СОИСКАТЕЛЕЙ:\n\n");
        int idx = 1;
        for (Map.Entry<String, VoteStatisticsService.CandidateStats> entry : archiveMap.entrySet()) {
            String candidateName = entry.getKey();
            VoteStatisticsService.CandidateStats stats = entry.getValue();
            sb.append(String.format("%d) %s\n", idx, candidateName));
            sb.append(stats.getStatsText()); // уже готовый метод, выводит "Ответственность, Интерес, ... Да/Нет..."
            sb.append("\n");
            idx++;
        }
        return sb.toString();
    }
}
