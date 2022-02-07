package org.camunda.scale.externalclient;


import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
import org.camunda.scale.workexecution.WorkExecution;
import org.camunda.scale.workexecution.WorkTracker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.logging.Logger;


public class ExternalJava {
    private static final Logger logger = Logger.getLogger(ExternalJava.class.getName());

    public static void main(String[] args) {

        // prepare back-off strategy for external task
        final BackoffStrategy backoffStrategy = new ExponentialBackoffStrategy(1000, 2, 16000);
        // bootstrap the client
        String baseUrl = "http://localhost:8080/engine-rest";

        List<ExternalTaskClient> listClients = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ExternalTaskClient client =
                    ExternalTaskClient.create()
                            .baseUrl(baseUrl)
                            .workerId("client-" + i)
                            .backoffStrategy(backoffStrategy)
                            .maxTasks(1)
                            .build();

            client.subscribe("work-to-do")
                    .lockDuration(60000)
                    .handler(
                            (externalTask, externalTaskService) -> {

                                WorkExecution workExecution = new WorkExecution();
                                workExecution.execute(externalTask.getId());

                                externalTaskService.complete(externalTask);


                            })
                    .open();
            listClients.add(client);
        }
        WorkTracker tracker=WorkTracker.getInstance();
        tracker.monitorTracker();
        logger.info("ExternalJava: the end");
    }


}
