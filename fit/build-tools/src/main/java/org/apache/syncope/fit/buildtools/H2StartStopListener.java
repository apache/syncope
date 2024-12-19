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
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utility servlet context listener managing H2 test server instance (to be used as external resource).
 */
@WebListener
public class H2StartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(H2StartStopListener.class);

    private Server h2TestDb;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());

        try {
            h2TestDb = new Server();
            h2TestDb.runTool("-ifNotExists", "-tcp", "-tcpDaemon", "-web", "-webDaemon",
                    "-webPort", Objects.requireNonNull(ctx).getEnvironment().getProperty("testdb.webport"));
        } catch (SQLException e) {
            LOG.error("Could not start H2 test db", e);
        }

        DataSource datasource = ctx.getBean("testDataSource", DataSource.class);

        try {
            ResourceDatabasePopulator populator =
                    new ResourceDatabasePopulator(ctx.getResource("classpath:/testdb.sql"));
            populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            DataSourceInitializer init = new DataSourceInitializer();
            init.setDataSource(datasource);
            init.setEnabled(true);
            init.setDatabasePopulator(populator);
            init.afterPropertiesSet();
            LOG.info("H2 database successfully initialized");
        } catch (Exception e) {
            LOG.error("Could not initialize H2", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        if (h2TestDb != null) {
            h2TestDb.shutdown();
        }
    }
}
