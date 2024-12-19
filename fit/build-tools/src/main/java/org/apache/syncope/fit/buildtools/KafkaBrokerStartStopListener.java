/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.buildtools;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@WebListener
public class KafkaBrokerStartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaBrokerStartStopListener.class);

    private EmbeddedKafkaZKBroker embeddedKafkaBroker;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());

        embeddedKafkaBroker = new EmbeddedKafkaZKBroker(
                1,
                false,
                ctx.getEnvironment().getProperty("kafka.topics", String[].class)).
                kafkaPorts(ctx.getEnvironment().getProperty("kafka.port", Integer.class));

        embeddedKafkaBroker.afterPropertiesSet();

        LOG.info("Kafka broker successfully (re)started");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        if (embeddedKafkaBroker != null) {
            embeddedKafkaBroker.destroy();

            LOG.info("Kafka broker successfully stopped");
        }
    }
}
