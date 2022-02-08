package org.camunda.scale.beanthread;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.scale.workexecution.WorkExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

//

@Configuration

public class BeanThread {

    private static final Logger logger = Logger.getLogger(BeanThread.class.getName());

    /**
     * This object is used only to track the activity - not use it
     */
    private final Set<String> inProgressActivivy = new HashSet<>();

    /**
     * How many threads do I want to execute at a time?
     */
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);


    // Attention, the topic "work-to-do" must be handled at a time by only one bean.
    // So, when you replace the value here, check that all other Bean does not get it.
    @Bean
    @ExternalTaskSubscription(
            topicName = "work-to-do-replaceit",
            lockDuration = 60000)
    public ExternalTaskHandler workToDoHandler() {
        return (externalTask, externalTaskService) -> {
            synchronized (inProgressActivivy) {
                inProgressActivivy.add(externalTask.getId());
            }

            logger.info("BeanThread.execute : >>>>>>>>>>> start ["+externalTask.getId()
                    +"] in parallel "+inProgressActivivy.size() + "In pool "+executor.getQueue().size());

            executor.submit(() -> {
                        WorkExecution workExecution = new WorkExecution();
                        workExecution.execute("BatchThread", externalTask.getId());
                        externalTaskService.complete(externalTask);
                    }
            );
            synchronized (inProgressActivivy) {
                inProgressActivivy.remove(externalTask.getId());
            }

            logger.info("BeanThread.execute : >>>>>>>>>>>> end ["+externalTask.getId()+"]");
        };
    }


}
