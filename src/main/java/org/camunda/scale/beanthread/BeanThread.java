package org.camunda.scale.beanthread;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.scale.externalclient.ExternalJava;
import org.camunda.scale.workexecution.WorkExecution;
import org.camunda.scale.workexecution.WorkTracker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

//

@Configuration
@EnableScheduling



public class BeanThread {

    private static final Logger logger = Logger.getLogger(BeanThread.class.getName());

    private final Set<String> inProgressActivivy = new HashSet<>();
    ExecutorService executor = Executors.newFixedThreadPool(20);


    @Bean
    @ExternalTaskSubscription(
            topicName = "work-to-do",
            lockDuration = 60000)
    public ExternalTaskHandler workToDoHandler() {
        return (externalTask, externalTaskService) -> {
            synchronized (inProgressActivivy) {
                inProgressActivivy.add(externalTask.getId());
            }

                logger.info("BeanThread.execute : >>>>>>>>>>> start ["+externalTask.getId()+"] in parallel "+inProgressActivivy.size());

            executor.submit(() -> {
                        WorkExecution workExecution = new WorkExecution();
                        workExecution.execute(externalTask.getId());
                        externalTaskService.complete(externalTask);
                    }
            );
            synchronized (inProgressActivivy) {
                inProgressActivivy.remove(externalTask.getId());
            }

            logger.info("BeanThread.execute : >>>>>>>>>>>> end ["+externalTask.getId()+"]");
        };
    }

    @Scheduled(fixedDelay = 60000)
    public void ping() {
        WorkTracker.getInstance().checkTracker();
    }
}
