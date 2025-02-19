package ru.greemlab.interviewresultsbot.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.greemlab.interviewresultsbot.bot.CandidateEvaluationBot;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(CandidateEvaluationBot bot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}
