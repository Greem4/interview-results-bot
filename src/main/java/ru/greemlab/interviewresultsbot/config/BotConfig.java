package ru.greemlab.interviewresultsbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.greemlab.interviewresultsbot.bot.CandidateEvaluationBot;

/**
 * Регистрируем нашего бота в TelegramBotsApi.
 */
@Configuration
public class BotConfig {

    /**
     * Регистрируемся как LongPollingBot.
     * Если нужно очень много подключений, рассмотрите переход на Webhook (требует HTTPS-сервер).
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(CandidateEvaluationBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}
