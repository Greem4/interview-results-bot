package ru.greemlab.interviewresultsbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.greemlab.interviewresultsbot.bot.CandidateEvaluationBot;
import ru.greemlab.interviewresultsbot.service.DialogStateMachineService;

/**
 * Регистрируем нашего бота в TelegramBotsApi.
 */
@Configuration
public class BotConfig {

    /**
     * Настраиваем DefaultBotOptions и задаём многопоточность (maxThreads).
     */
    @Bean
    public DefaultBotOptions defaultBotOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        // например, выставим пул в 10 потоков
        options.setMaxThreads(50);
        return options;
    }

    /**
     * Создаём бина нашего бота, передавая ему:
     * - username
     * - token
     * - настроенный DefaultBotOptions
     * - сервис DialogStateMachineService
     */
    @Bean
    public CandidateEvaluationBot candidateEvaluationBot(
            DefaultBotOptions options,
            @Value("${app.bot.username}") String botUsername,
            @Value("${app.bot.token}") String botToken,
            DialogStateMachineService dialogStateMachineService
    ) {
        return new CandidateEvaluationBot(botUsername, botToken, options, dialogStateMachineService);
    }

    /**
     * Регистрируемся как LongPollingBot с многопоточными опциями.
     * Если нужно очень много подключений, рассмотрите переход на Webhook.
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(CandidateEvaluationBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        // Регистрируем бота (уже с многопоточными настройками)
        api.registerBot(bot);
        return api;
    }
}
