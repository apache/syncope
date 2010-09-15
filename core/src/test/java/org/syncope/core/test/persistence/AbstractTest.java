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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
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

    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractTest.class);
    protected static String bundles_version;
    protected static String bundles_directory;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private DefaultDataTypeFactory dbUnitDataTypeFactory;

    private void logTableContent(Connection conn, String tableName)
            throws SQLException {

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();

            rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData metaData = rs.getMetaData();
            LOG.debug("Table: " + tableName);
            StringBuilder row = new StringBuilder();
            while (rs.next()) {
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    row.append(metaData.getColumnLabel(i + 1)).append("=").
                            append(rs.getString(i + 1)).append(" ");
                }

                LOG.debug(row.toString());
                row.delete(0, row.length());
            }
        } catch (SQLException sqle) {
            LOG.error("While dumping " + tableName + "content", sqle);
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
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                dbUnitDataTypeFactory);

        FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
        dataSetBuilder.setColumnSensing(true);
        IDataSet dataSet = dataSetBuilder.build(getClass().getResourceAsStream(
                "/org/syncope/core/persistence/content.xml"));
        try {
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConn, dataSet);
        } catch (Throwable t) {
            LOG.error("While executing tests", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        if (LOG.isDebugEnabled()) {
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

    @Before
    public void init() {
        Properties props = new java.util.Properties();
        try {
            InputStream propStream =
                    getClass().getResourceAsStream(
                    "/bundles.properties");
            props.load(propStream);
            bundles_version = props.getProperty("bundles.version");
            bundles_directory = props.getProperty("bundles.directory");
        } catch (Throwable t) {
            LOG.error("Could not load bundles.properties", t);
        }
        assertNotNull(bundles_version);
        assertNotNull(bundles_directory);
    }
}
