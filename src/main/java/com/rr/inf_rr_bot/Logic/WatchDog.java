package com.rr.inf_rr_bot.Logic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class WatchDog implements Runnable {
    private final Worker worker;

    @Autowired
    public WatchDog(Worker worker) {
        this.worker = worker;
    }

    @Override
    public void run() {
        // Здесь вызываем метод, который нужно выполнять каждый час
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        // Запускаем задачу только если текущее время находится в промежутке с 8:00 до 19:00
        if (currentHour >= 7 && currentHour < 19) {
            // Здесь вызываем метод, который нужно выполнять каждый час
            worker.process();
        }
    }

    public void start() {
        // Запускаем задачу сразу
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        // Рассчитываем время для следующего запуска задачи с интервалом в 1 час
        executorService.scheduleAtFixedRate(this, 0, 1, TimeUnit.HOURS);
    }

   }
