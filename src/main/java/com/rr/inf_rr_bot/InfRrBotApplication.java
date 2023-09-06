package com.rr.inf_rr_bot;

import com.rr.inf_rr_bot.Beans.ChatID;
import com.rr.inf_rr_bot.Logic.WatchDog;
import com.rr.inf_rr_bot.Logic.Worker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class InfRrBotApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(InfRrBotApplication.class, args);
        WatchDog watchDog = context.getBean(WatchDog.class);
        watchDog.start();


    }


}
