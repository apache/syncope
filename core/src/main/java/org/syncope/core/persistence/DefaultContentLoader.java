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
import org.syncope.core.persistence.beans.SyncopeConfiguration;

/**
 * Load default content in the database.
 */
public class DefaultContentLoader implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(
            DefaultContentLoader.class);

    /**
     * <em>WARNING</em>: this method connects to the database by mean of the 
     * underlying Spring's datasource, not using the provided one, to be fetched
     * via JNDI. This in order to avoid potential conflicts and problems with
     * DbUnit.
     * @param sce
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        WebApplicationContext springContext =
                WebApplicationContextUtils.getWebApplicationContext(
                sce.getServletContext());

        DataSource dataSource =
                (DataSource) springContext.getBean("localDataSource");
        DefaultDataTypeFactory dbUnitDataTypeFactory =
                (DefaultDataTypeFactory) springContext.getBean(
                "dbUnitDataTypeFactory");

        String dbSchema = null;
        try {
            InputStream dbPropsStream =
                    sce.getServletContext().getResourceAsStream(
                    "WEB-INF/classes/"
                    + "org/syncope/core/persistence/db.properties");
            Properties dbProps = new Properties();
            dbProps.load(dbPropsStream);
            dbSchema = dbProps.getProperty("database.schema");
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("Could not find db.properties", t);
            } else {
                log.error("Could not find db.properties");
            }

            dbSchema = null;
        }

        Connection conn = DataSourceUtils.getConnection(dataSource);

        Statement statement = null;
        ResultSet resultSet = null;
        boolean existingData = false;
        try {
            statement = conn.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            resultSet = statement.executeQuery("SELECT * FROM "
                    + SyncopeConfiguration.class.getSimpleName());
            resultSet.last();

            existingData = resultSet.getRow() > 0;
        } catch (SQLException e) {
            log.error("Could not access to table "
                    + SyncopeConfiguration.class.getSimpleName(), e);

            // Setting this to true make nothing to be done below
            existingData = true;
        } finally {
            try {
                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                log.error("While closing SQL connection", e);
            }
        }
        try {
            IDatabaseConnection dbUnitConn = dbSchema == null
                    ? new DatabaseConnection(conn)
                    : new DatabaseConnection(conn, dbSchema);

            DatabaseConfig config = dbUnitConn.getConfig();
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                    dbUnitDataTypeFactory);
            config.setProperty(
                    DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, true);

            if (existingData) {
                log.info("Data found in the database, leaving untouched");
            } else {
                log.info("Empty database found, loading default content");

                FlatXmlDataSetBuilder dataSetBuilder =
                        new FlatXmlDataSetBuilder();
                dataSetBuilder.setColumnSensing(true);
                IDataSet dataSet = dataSetBuilder.build(
                        getClass().getResourceAsStream("content.xml"));

                DatabaseOperation.REFRESH.execute(dbUnitConn, dataSet);
            }
        } catch (Throwable t) {
            log.error("While loading default content", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
    }
}
