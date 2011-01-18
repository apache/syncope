/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.rest.controller.UserController;

/**
 * Load default content in the database.
 */
public class DefaultContentLoader implements ServletContextListener {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            DefaultContentLoader.class);

    /**
     * <em>WARNING</em>: this method connects to the database by mean of the
     * underlying Spring's datasource, not using the provided one, to be fetched
     * via JNDI. This in order to avoid potential conflicts and problems with
     * DbUnit.
     * @param sce ServletContext event
     */
    @Override
    public final void contextInitialized(final ServletContextEvent sce) {
        WebApplicationContext springContext =
                WebApplicationContextUtils.getWebApplicationContext(
                sce.getServletContext());

        // 0. DB connection, to be used below
        DataSource dataSource =
                (DataSource) springContext.getBean("localDataSource");
        Connection conn = DataSourceUtils.getConnection(dataSource);

        // 1. read persistence.properties and set search mode
        String dbSchema = null;
        String searchMode = null;
        try {
            InputStream dbPropsStream = getClass().getResourceAsStream(
                    "/persistence.properties");
            Properties dbProps = new Properties();
            dbProps.load(dbPropsStream);
            dbSchema = dbProps.getProperty("database.schema");
            searchMode = dbProps.getProperty("search.mode");
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not find persistence.properties", t);
            } else {
                LOG.error("Could not find persistence.properties");
            }
        }

        LOG.debug("Setting search mode to " + searchMode);
        UserController.setSearchMode(searchMode);
        LOG.debug("Search mode set to {}", UserController.getSearchMode());

        // 2. Check wether we are allowed to load default content into the DB
        Statement statement = null;
        ResultSet resultSet = null;
        boolean existingData = false;
        try {
            statement = conn.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            resultSet = statement.executeQuery("SELECT * FROM "
                    + SyncopeConf.class.getSimpleName());
            resultSet.last();

            existingData = resultSet.getRow() > 0;
        } catch (SQLException e) {
            LOG.error("Could not access to table "
                    + SyncopeConf.class.getSimpleName(), e);

            // Setting this to true make nothing to be done below
            existingData = true;
        } finally {
            try {
                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                LOG.error("While closing SQL statement", e);
            }
        }

        if (existingData) {
            LOG.info("Data found in the database, leaving untouched");
            return;
        }

        LOG.info("Empty database found, loading default content");

        // 3. Create views
        LOG.debug("Creating views");
        try {
            InputStream viewsStream = getClass().getResourceAsStream(
                    "/views.xml");
            Properties views = new Properties();
            views.loadFromXML(viewsStream);

            for (String idx : views.stringPropertyNames()) {
                LOG.debug("Creating view {}", views.get(idx).toString());

                try {
                    statement = conn.createStatement();
                    statement.executeUpdate(views.get(idx).toString().
                            replaceAll("\\n", " "));
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Could not create view ", e);
                }
            }

            LOG.debug("Views created, go for indexes");
        } catch (Throwable t) {
            LOG.error("While creating views", t);
        }

        // 4. Create indexes
        LOG.debug("Creating indexes");
        try {
            InputStream indexesStream = getClass().getResourceAsStream(
                    "/indexes.xml");
            Properties indexes = new Properties();
            indexes.loadFromXML(indexesStream);

            for (String idx : indexes.stringPropertyNames()) {
                LOG.debug("Creating index {}", indexes.get(idx).toString());

                try {
                    statement = conn.createStatement();
                    statement.executeUpdate(indexes.get(idx).toString());
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Could not create index ", e);
                }
            }

            LOG.debug("Indexes created, go for default content");
        } catch (Throwable t) {
            LOG.error("While creating indexes", t);
        }

        // 5. Load default content
        try {
            IDatabaseConnection dbUnitConn = dbSchema == null
                    ? new DatabaseConnection(conn)
                    : new DatabaseConnection(conn, dbSchema);

            DatabaseConfig config = dbUnitConn.getConfig();
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                    (DefaultDataTypeFactory) springContext.getBean(
                    "dbUnitDataTypeFactory"));
            config.setProperty(
                    DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES,
                    true);

            FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
            dataSetBuilder.setColumnSensing(true);
            IDataSet dataSet = dataSetBuilder.build(getClass().
                    getResourceAsStream("/content.xml"));

            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConn, dataSet);

            LOG.debug("Default content successfully loaded");
        } catch (Throwable t) {
            LOG.error("While loading default content", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        try {
            conn.close();
        } catch (SQLException e) {
            LOG.error("While closing SQL connection", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
    }
}
