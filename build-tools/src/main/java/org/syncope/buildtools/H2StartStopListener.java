/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.buildtools;

import java.io.File;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utility serlvet context listener managing H2 test server instance
 * (to be used as external resource).
 */
public class H2StartStopListener implements ServletContextListener {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(H2StartStopListener.class);

    private static final String H2_TESTDB = "h2TestDb";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        File workDir = (File) sce.getServletContext().getAttribute(
                "javax.servlet.context.tempdir");
        Server h2TestDb;
        try {
            h2TestDb = new Server();
            h2TestDb.runTool(
                    "-baseDir", workDir.getAbsolutePath(),
                    "-tcp", "-tcpDaemon",
                    "-web", "-webDaemon", "-webPort",
                    sce.getServletContext().getInitParameter("testdb.webport"));

            context.setAttribute(H2_TESTDB, h2TestDb);
        } catch (SQLException e) {
            LOG.error("Could not start H2 test db", e);
        }

        WebApplicationContext ctx =
                WebApplicationContextUtils.getWebApplicationContext(context);
        DataSource datasource = ctx.getBean(DataSource.class);

        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(datasource),
                new ClassPathResource("/testdb.sql"), false);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        Server h2TestDb = (Server) context.getAttribute(H2_TESTDB);
        if (h2TestDb != null) {
            h2TestDb.shutdown();
        }
    }
}
