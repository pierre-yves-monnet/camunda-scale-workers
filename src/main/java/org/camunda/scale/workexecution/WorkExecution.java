package org.camunda.scale.workexecution;

import java.util.HashMap;
import java.util.logging.Logger;

public class WorkExecution {
    private static final HashMap<String, Long> registerSignature = new HashMap<>();
    Logger logger = Logger.getLogger(WorkExecution.class.getName());

    public void execute(String signature) {
        try {
            if (registerSignature.containsKey(signature)) {
                logger.info("WorkExecution - this [" + signature + "] show up multiple time ! " + Long.valueOf(registerSignature.get(signature) + 1));
            }
            synchronized (registerSignature) {
                long execution = registerSignature.getOrDefault(signature, Long.valueOf(0));
                registerSignature.put(signature, Long.valueOf(execution + 1));
            }

            // is this signature was already registered?
            logger.info("WorkExecution - start [" + signature + "]");
            WorkTracker.getInstance().startActivity(signature);
            Thread.sleep(14 * 1000);
            WorkTracker.getInstance().endActivity(signature);
            logger.info("WorkExecution - end [" + signature + "]");
        } catch (InterruptedException e) {
            logger.info("WorkExecution - error [" + signature + "]" + e);
        }
    }
}
