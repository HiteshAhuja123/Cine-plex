package com.moviebooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configures the Spring scheduling executor to use Java 21 virtual threads.
 *
 * <p>{@link SimpleAsyncTaskScheduler#setVirtualThreads(true)} is the Spring 6.1+
 * idiomatic way to run scheduled tasks on virtual threads.
 * ({@code Executors.newVirtualThreadPerTaskExecutor()} returns a
 * {@code ThreadPerTaskExecutor} which is an {@code ExecutorService} but NOT a
 * {@code ScheduledExecutorService}, so {@code ScheduledTaskRegistrar} rejects it.)
 *
 * <p>Virtual threads are ideal here: the scheduler's JDBC calls block briefly
 * on I/O, and with virtual threads that blocking doesn't tie up a platform thread.
 */
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);  // Java 21 virtual threads
        scheduler.setThreadNamePrefix("scheduler-vt-");
        registrar.setTaskScheduler(scheduler);
    }
}
