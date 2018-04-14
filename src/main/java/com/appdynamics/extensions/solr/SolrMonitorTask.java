/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.solr;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.solr.core.Core;
import com.appdynamics.extensions.solr.core.CoreContext;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.solr.input.Stat;
import com.appdynamics.extensions.solr.metrics.MetricCollector;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

public class SolrMonitorTask implements AMonitorTaskRunnable {
    private static final Logger logger = LoggerFactory.getLogger(SolrMonitorTask.class);
    private Map server;
    private MetricWriteHelper metricWriteHelper;
    private MonitorContextConfiguration monitorContextConfiguration;


    public SolrMonitorTask(MonitorContextConfiguration monitorContextConfiguration,  MetricWriteHelper metricWriteHelper,
                           Map<String, String> server) {
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.server = server;
        this.metricWriteHelper = metricWriteHelper;

    }

    public void onTaskComplete() {
        logger.info("Completed the NetScaler Monitoring Task for server {}", server.get("name"));
    }


    public void run(){
        try {
            Phaser phaser = new Phaser();
            Stat.Stats metricConfiguration = (Stat.Stats) monitorContextConfiguration.getMetricsXml();

            for (Stat stat : metricConfiguration.getStats()) {
                phaser.register();
                MetricCollector metricCollector = new MetricCollector(stat,  monitorContextConfiguration, server, phaser, metricWriteHelper);
//                runTask();
                monitorContextConfiguration.getContext().getExecutorService().execute("MetricCollectorTask", metricCollector);
                logger.debug("Registering MetricCollectorTask phaser for {}", server.get("name"));

            }
            phaser.arriveAndAwaitAdvance();
            logger.info("Completed the Solr Metric Monitoring task");
        }
        catch (Exception e){
            logger.error("An error was encountered during the Solr Monitoring Task for server : " + server.get("name"), e.getMessage());

        }
    }

//    public void run() {
//        try {
//            runTask();
//            logger.info("Solr Metric Upload Complete");
//        } catch (Exception ex) {
//            configuration.getMetricWriter().registerError(ex.getMessage(), ex);
//            logger.error("Error while running the task", ex);
//        }
//    }
//
    private void runTask() {
        try {
            CoreContext coreContext = new CoreContext(monitorContextConfiguration.getContext().getHttpClient(), server);
            List<Core> cores = coreContext.getCores(monitorContextConfiguration.getConfigYml());
            populateAndPrintStats(cores, coreContext.getContextRoot());
            logger.info("Solr monitoring task completed successfully.");
        } catch (Exception e) {
            logger.error("Exception while running Solr Monitor Task ", e);
        }
    }

    private void populateAndPrintStats(List<Core> coresConfig, String contextRoot) throws IOException {
        SolrStats solrStats = new SolrStats(server, contextRoot, monitorContextConfiguration.getContext().getHttpClient());
        for (Core coreConfig : coresConfig) {
            Map<String, BigDecimal> metrics = solrStats.populateStats(coreConfig);
//            printMetrics(metrics);
        }

    }

//    private void printMetrics(Map<String, BigDecimal> solrMetrics) {
//        MetricWriteHelper metricWriter = configuration.getMetricWriter();
//        String metricPrefix = configuration.getMetricPrefix();
//        String aggregation = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
//        String cluster = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
//        String timeRollup = MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE;
//
//        for (Map.Entry<String, BigDecimal> entry : solrMetrics.entrySet()) {
//            String metricPath = metricPrefix + "|" + server.get("name").toString() + entry.getKey();
//            String metricValue = String.valueOf(entry.getValue());
//            metricWriter.printMetric(metricPath, metricValue, aggregation, timeRollup, cluster);
//        }
//    }
}
