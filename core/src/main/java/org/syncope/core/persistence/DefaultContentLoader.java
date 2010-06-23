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

import java.sql.Connection;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DefaultContentLoader implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(
            DefaultContentLoader.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext springContext =
                WebApplicationContextUtils.getWebApplicationContext(
                sce.getServletContext());

        DataSource dataSource =
                (DataSource) springContext.getBean("dataSource");
        DefaultDataTypeFactory dbUnitDataTypeFactory =
                (DefaultDataTypeFactory) springContext.getBean(
                "dbUnitDataTypeFactory");

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            IDatabaseConnection dbUnitConn = new DatabaseConnection(conn);

            DatabaseConfig config = dbUnitConn.getConfig();
            config.setProperty(
                    "http://www.dbunit.org/properties/datatypeFactory",
                    dbUnitDataTypeFactory);

            boolean existingData = false;
            IDataSet existingDataSet = dbUnitConn.createDataSet();
            for (ITableIterator itor = existingDataSet.iterator();
                    itor.next() && !existingData;) {

                existingData = (itor.getTable().getRowCount() > 0);
            }

            if (existingData) {
                log.info("Data found in the database, leaving untouched");
            } else {
                log.info("Empty database found, loading default content");

                FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
                dataSetBuilder.setColumnSensing(true);
                IDataSet dataSet = dataSetBuilder.build(
                        getClass().getResourceAsStream(
                        "/org/syncope/core/persistence/content.xml"));

                DatabaseOperation.REFRESH.execute(dbUnitConn, dataSet);
            }
        } catch (Throwable t) {
            log.error("While loading default content", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
