# camunda-scale-workers
This project demonstrates different ways to scale a worker in the same Java Machine.

# Scope
A process has multi-instance service tasks, and each process instance generates 200 tasks.
The service task to perform (workexecution/WorkerExecution) sleeps 14 seconds in order to simulate something to perform.
With only one worker executing in sequential the treatment, it needs than 200*14 s = 2800 s, so 46 mn.


Note: in all options after, increasing the number of thread after 40 does not improve performances. 
WorkerExecution has a synchronized() method to track the number of execution in progress and to determine when the work is finished

## option 1 Multiple Workers objects:

Visit externalclient/External.java
This solution is a Java program that creates multiple Client Workers.
Each client run in a different thread.
Starting the software with numberOfClients = 20, the total time to execute the treatment is 3 mn 39.


20 clients => 218704 ( 3mn 38)
40 clients => 134,384 ms for 236 (2 mn 14 )
80 clients => 128372

# Option 2 BeanThreadApplication
Visit beanthread/BeanThread
In this implementation, each call immediately starts a new thread, which handles the execution.
To control the maximum number of threads running on the machine, a ThreadPoolExecutor is used, with a limit of 40 threads.

Attention: This method has a limitation. Because any task is immediately taken in charge by the client, it locks the 200 tasks immediately. Because it takes time to process it, then the lock expires on some tasks. Camunda resubmits them.
Using this method implies having a big lock duration or using the next option

# Option 3 BeanThreadApplication Limited
Visit beanthreadlimitation/BeanThreadLimitedByExecutor
Same as before, except when the ThreadPool is full, the external client is stopped. The client does not accept any new task.
To do the stop() and start, a new thread has to be used. External task does not accept that the handle thread does this kind of operation.
Note: this way to handle the start/stop is very dynamique, and can be used to resize the number of workers differently in the day. 

Visit beanthreadlimitation/BeanThreadLimitedByObject
In this implementation, the bean created a limited number of ExternalTask Client.

Nota: you have to change the topic to get the work between beanthread/BeanThread and beanthreadlimitation/BeanThreadLimited
 
# Option 4 Spring Boot multiple workers
In the spring boot config, describe multiple environment. Each environment starts its own workers.
Recommendation:
* Each worker should have a different worker ID
* Set the max task to 1 
==> To verify



Spring Boot Dynamique bean
Create multiple beans, to have multiple threads
See:
https://docs.camunda.org/manual/develop/user-guide/ext-client/spring-boot-starter/#custom-clien
==> To verify
