# camunda-scale-workers
# Scope
Execute 200 tasks
Each task run in 14 seconds
==> Sequential is 200 * 14000 = 2 800,000 ms (46 mn )

## External client
Execute ExternalJava. Then, multiple client worker are enabled.

20 clients => 218704 ( 3mn 38)
40 clients => 134,384 ms for 236 (2 mn 14 )
80 clients => 128372

# BeanThreadApplication

