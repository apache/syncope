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
package org.syncope.core.test.dao;

import java.io.FileInputStream;
import java.sql.Connection;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.jpa.AbstractJpaTests;
import org.syncope.core.dao.DAO;

public abstract class AbstractDAOTest extends AbstractJpaTests {

    protected String dataSetFileName;
    protected DAO dao;
    protected static final Logger log = LoggerFactory.getLogger(
            AbstractDAOTest.class);

    protected AbstractDAOTest(String beanName, String dataSetFileName) {
        super();
        this.dataSetFileName = dataSetFileName;

        ApplicationContext ctx = super.getApplicationContext();
        dao = (DAO) ctx.getBean(beanName);
        assertNotNull(dao);
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[]{"applicationContext.xml"};
    }

    @Override
    protected void onSetUpInTransaction() throws Exception {
        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        IDatabaseConnection dbUnitConn = new DatabaseConnection(conn);

        DatabaseConfig config = dbUnitConn.getConfig();
        config.setProperty("http://www.dbunit.org/properties/datatypeFactory",
                new HsqldbDataTypeFactory());

        FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
        IDataSet dataSet = dataSetBuilder.build(new FileInputStream(
                "./src/test/resources/dbunit-test-data/"
                + dataSetFileName + ".xml"));

        try {
            DatabaseOperation.CLEAN_INSERT.execute(dbUnitConn, dataSet);
        } catch (Throwable t) {
            log.error("While executing tests", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    protected abstract DAO getDAO();
}
