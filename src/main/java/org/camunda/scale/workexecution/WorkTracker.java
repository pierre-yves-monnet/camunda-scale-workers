package org.camunda.scale.workexecution;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class WorkTracker {
    private static final Logger logger = Logger.getLogger(WorkTracker.class.getName());

    private static final WorkTracker workTracker = new WorkTracker();
    public static WorkTracker getInstance() {
        return workTracker;
    }

    private long lastTouchTime =0;
    private long startActivityTime =0;
    private long countNbTouch=0;

    public Set<String> inProgressActivity = new HashSet<>();
    int maxParallel=0;

    private boolean inProgress=false;
    public void startActivity(String signature) {
        synchronized (this) {
            lastTouchTime = System.currentTimeMillis();
            countNbTouch++;
            inProgressActivity.add(signature);
            if (inProgressActivity.size()>maxParallel) {
                maxParallel=inProgressActivity.size();
                logger.info("pick parallele activity " + maxParallel);

            }
        }
        if (! inProgress) {
            // restart the activity
            inProgress=true;
            startActivityTime = lastTouchTime;
            countNbTouch=1;
        }
    }
    public void endActivity(String signature) {
        synchronized (this) {
            inProgressActivity.remove(signature);
        }
    }
    public void monitorTracker() {
        while(true) {
            checkTracker();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.severe("Error during sleep");
            }
        }
    }
    public void checkTracker() {
        long currentTimeMillis=System.currentTimeMillis();
        if (inProgress && currentTimeMillis - lastTouchTime > 40 * 1000) {
            // activity is done now
            logger.info("========================= Last Activity. Last run duration is " + (lastTouchTime - startActivityTime) + " ms for " + countNbTouch +" parallele "+maxParallel);
            inProgress = false;
            maxParallel=0;
        }
    }
}
