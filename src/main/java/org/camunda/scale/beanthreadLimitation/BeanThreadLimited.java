package org.camunda.scale.beanthreadLimitation;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.scale.workexecution.WorkExecution;
import org.camunda.scale.workexecution.WorkTracker;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

//
// @ E n  a b l e S  c h e d u  l i n g
// @  C o  m p o n e n t

public class BeanThreadLimited {

    private static final Logger logger = Logger.getLogger(BeanThreadLimited.class.getName());

    /**
     * This object is used only to track the activity - not use it
     */
    private final Set<String> inProgressActivivy = new HashSet<>();

    /**
     * How many thread do I want to execute at a time?
     * Nb: we instantiate a ThreadPoolExecutor, then we can access the size
     */
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

    ExternalTaskClient externalTaskClient;

    public BeanThreadLimited() {
        externalTaskClient = ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest")
                .lockDuration(60000)
                .workerId("WorkBeanThreadLimited")
                .build();
        externalTaskClient.subscribe("work-to-do-replaceit")
                .handler((externalTask, externalTaskService) -> {

                    synchronized (inProgressActivivy) {
                        inProgressActivivy.add(externalTask.getId());
                    }

                    logger.info("BeanThread.execute : >>>>>>>>>>> start [" + externalTask.getId()
                            + "] in parallel " + inProgressActivivy.size() + "In pool " + executor.getQueue().size());
                    if (executor.getQueue().size() >= executor.getCorePoolSize()) {
                        // sleep, we don't want to accept any new job now
                        externalTaskClient.stop();
                    }
                    executor.submit(() -> {
                                WorkExecution workExecution = new WorkExecution();
                                workExecution.execute(externalTask.getId());
                                externalTaskService.complete(externalTask);
                                if (!externalTaskClient.isActive())
                                    externalTaskClient.start();
                            }
                    );
                    synchronized (inProgressActivivy) {
                        inProgressActivivy.remove(externalTask.getId());
                    }

                    logger.info("BeanThread.execute : >>>>>>>>>>>> end [" + externalTask.getId() + "]");
                });
    }

    @Scheduled(fixedDelay = 60000)
    public void ping() {
        WorkTracker.getInstance().checkTracker();
    }
}
