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
import java.util.List;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.jpa.AbstractJpaTests;
import org.syncope.core.beans.SyncopeUser;
import org.syncope.core.dao.SyncopeUserDAO;

public class SyncopeUserDAOTest extends AbstractJpaTests {

    private SyncopeUserDAO dao;
    private static final Logger log = LoggerFactory.getLogger(
            SyncopeUserDAOTest.class);

    public SyncopeUserDAOTest() {
        super();

        ApplicationContext ctx = super.getApplicationContext();
        dao = (SyncopeUserDAO) ctx.getBean("userDAO");
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

        FlatXmlDataSetBuilder dataSetBuilder = new FlatXmlDataSetBuilder();
        IDataSet dataSet = dataSetBuilder.build(new FileInputStream(
                "./src/test/resources/dbunit-test-data/SyncopeUserDaoImpl.xml"));

        try {
            DatabaseOperation.REFRESH.execute(dbUnitConn, dataSet);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Test
    public final void testFindAll() {
        List<SyncopeUser> list = dao.findAll();
        assertEquals("did not get expected number of users ", 3, list.size());
    }

    @Test
    public final void testFindById() {
        SyncopeUser user = dao.find(1L);
        assertNotNull("did not find expected user", user);
        user = dao.find(3L);
        assertNotNull("did not find expected user", user);
        user = dao.find(4L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public final void testSave() {
        SyncopeUser user = new SyncopeUser();
        user.setId(4L);

        dao.save(user);

        SyncopeUser actual = dao.find(4L);
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void testDelete() {
        SyncopeUser user = dao.find(1L);

        dao.delete(user.getId());

        SyncopeUser actual = dao.find(1L);
        assertNull("delete did not work", actual);
    }
}
