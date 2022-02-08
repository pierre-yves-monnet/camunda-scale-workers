package org.camunda.scale.beanthreadLimitation;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.scale.workexecution.WorkExecution;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;


@Component
public class BeanThreadLimitedByExecutor {

    private static final Logger logger = Logger.getLogger(BeanThreadLimitedByExecutor.class.getName());

    public final static int MAX_THREADS = 20;
    /**
     * This object is used only to track the activity - not use it
     */
    private final Set<String> inProgressActivity = new HashSet<>();

    /**
     * How many thread do I want to execute at a time?
     * Nb: we instantiate a ThreadPoolExecutor, then we can access the size
     * Nota: you can set the maxTask() to this number
     */
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);

    ThreadPoolExecutor externalTaskController = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public BeanThreadLimitedByExecutor() {
    }


    @PostConstruct
    public void postConstruct() {
        logger.info("BeanThreadLimitedByExecutor: postContruct");
        String baseUrl = "http://localhost:8080/engine-rest";

            ExternalTaskClient client =
                    ExternalTaskClient.create()
                            .baseUrl(baseUrl)
                            .workerId("WorkBeanThreadExecutorLimited")
                            .maxTasks(1)
                            .build();

        // Attention, the topic "work-to-do" must be handled at a time by only one bean.
        // So, when you replace the value here, check that all other Bean does not get it.
        client.subscribe("work-to-do")
                .lockDuration(60000)
                .handler((externalTask, externalTaskService) -> {

                    synchronized (inProgressActivity) {
                        inProgressActivity.add(externalTask.getId());
                    }

                    logger.info("BeanThread.execute : >>>>>>>>>>> start [" + externalTask.getId()
                            + "] in parallel " + inProgressActivity.size() + ", queueSize=" + executor.getQueue().size());
                    if (executor.getQueue().size() >= MAX_THREADS && client.isActive()) {
                        // sleep, we don't want to accept any new job no
                        logger.info("BeanThreadLimitedByExecutor.execute: >>>>>>>>>>>>>>>>>>>>> Too much item in the queue, stop the subscription <<<<<<<<<<<<<<<<");

                        // stop now. Stop wait that all threads are ended, so to commit a suicide, we have to start a new thread.
                        externalTaskController.submit(() -> client.stop());
                        // so, actually, we can restart now
                        client.start();

                    }
                    executor.submit(() -> {
                                WorkExecution workExecution = new WorkExecution();
                                workExecution.execute("BatchThreadLimitedByExecutor", externalTask.getId());
                                externalTaskService.complete(externalTask);
                                logger.info("BeanThreadLimitedByExecutor.execute: how is the client? active="+client.isActive());
                                if (!client.isActive() && executor.getQueue().size() < MAX_THREADS) {
                                    logger.info("BeanThreadLimitedByExecutor.execute: >>>>>>>>>>>>>>>>>>>>>>> Restart the client <<<<<<<<<<<<<<<<<<");
                                    // must be an external thread to operate
                                    externalTaskController.submit(()-> client.start());
                                }
                                logger.info("BeanThreadLimitedByExecutor.execute: end of executor");

                            }
                    );
                    synchronized (inProgressActivity) {
                        inProgressActivity.remove(externalTask.getId());
                    }

                    logger.info("BeanThreadLimitedByExecutor.execute : >>>>>>>>>>>> end [" + externalTask.getId() + "] queueSize="+executor.getQueue().size());
                })
                .open();
    }


}
