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
package org.syncope.core.test.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:persistenceContext.xml"})
public abstract class AbstractTest {

    protected static final Logger log = LoggerFactory.getLogger(
            AbstractTest.class);
    @Autowired
    protected EntityManager entityManager;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private HsqldbDataTypeFactory dbUnitDataTypeFactory;

    private void logTableContent(Connection conn, String tableName)
            throws SQLException {

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();

            rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData metaData = rs.getMetaData();
            log.debug("Table: " + tableName);
            StringBuilder row = new StringBuilder();
            while (rs.next()) {
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    row.append(metaData.getColumnLabel(i + 1)).append("=").
                            append(rs.getString(i + 1)).append(" ");
                }

                log.debug(row.toString());
                row.delete(0, row.length());
            }
        } catch (SQLException sqle) {
            log.error("While dumping " + tableName + "content", sqle);
        } finally {
            rs.close();
            stmt.close();
        }
    }

    @Before
    public void setUpTestDataWithinTransaction() throws Exception {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection dbUnitConn = new DatabaseConnection(conn);

        DatabaseConfig config = dbUnitConn.getConfig();
        config.setProperty("http://www.dbunit.org/properties/datatypeFactory",
                dbUnitDataTypeFactory);

        FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
        dataSetBuilder.setColumnSensing(true);
        IDataSet dataSet = dataSetBuilder.build(getClass().getResourceAsStream(
                "/org/syncope/core/persistence/content.xml"));
        try {
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConn, dataSet);
        } catch (Throwable t) {
            log.error("While executing tests", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        if (log.isDebugEnabled()) {
            conn = DataSourceUtils.getConnection(dataSource);

            DatabaseMetaData dbm = conn.getMetaData();
            String[] types = {"TABLE"};
            ResultSet rs = dbm.getTables(null, null, "%", types);
            while (rs.next()) {
                logTableContent(conn, rs.getString("TABLE_NAME"));
            }

            try {
                rs.close();
                conn.close();
            } catch (SQLException e) {
            }
        }
    }
}
