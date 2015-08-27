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

import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
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
public class H2StartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(H2StartStopListener.class);

    private static final String H2_TESTDB = "h2TestDb";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);

        try {
            Server h2TestDb = new Server();
            h2TestDb.runTool("-tcp", "-tcpDaemon", "-web", "-webDaemon",
                    "-webPort", ctx.getBean("testdb.webport", String.class));

            context.setAttribute(H2_TESTDB, h2TestDb);
        } catch (SQLException e) {
            LOG.error("Could not start H2 test db", e);
        }

        DataSource datasource = ctx.getBean(DataSource.class);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(ctx.getResource("classpath:/testdb.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        DataSourceInitializer init = new DataSourceInitializer();
        init.setDataSource(datasource);
        init.setEnabled(true);
        init.setDatabasePopulator(populator);
        init.afterPropertiesSet();
        LOG.info("Database successfully initialized");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();

        final Server h2TestDb = (Server) context.getAttribute(H2_TESTDB);
        if (h2TestDb != null) {
            h2TestDb.shutdown();
        }
    }
}
