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
package org.syncope.core.dev.init;

import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility serlvet context listener managing H2 web console lifecycle.
 */
public class H2ConsoleContextListener implements ServletContextListener {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(H2ConsoleContextListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        Server h2Console;
        try {
            h2Console = Server.createWebServer("");
            h2Console.start();

            context.setAttribute("h2Console", h2Console);
        } catch (SQLException e) {
            LOG.error("Could not start H2 web console", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        Server h2Console = (Server) context.getAttribute("h2Console");
        if (h2Console != null) {
            h2Console.stop();
        }
    }
}
