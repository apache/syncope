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
package org.apache.syncope.core;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:syncopeContext.xml", "classpath:persistenceContext.xml",
    "classpath:schedulingContext.xml", "classpath:workflowContext.xml"})
public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static String connidSoapVersion;

    protected static String bundlesDirectory;

    protected void logTableContent(final Connection conn, final String tableName) throws SQLException {

        LOG.debug("Table: " + tableName);

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM " + tableName);

            final StringBuilder row = new StringBuilder();
            while (rs.next()) {
                for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                    row.append(rs.getMetaData().getColumnLabel(i + 1)).append("=").append(rs.getString(i + 1)).append(
                            " ");
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

    @BeforeClass
    public static void setUpIdentityConnectorsBundles() throws IOException {
        Properties props = new java.util.Properties();
        InputStream propStream = null;
        try {
            propStream = AbstractTest.class.getResourceAsStream("/bundles.properties");
            props.load(propStream);
            connidSoapVersion = props.getProperty("connid.soap.version");
            bundlesDirectory = props.getProperty("bundles.directory");
        } catch (Exception e) {
            LOG.error("Could not load bundles.properties", e);
        } finally {
            if (propStream != null) {
                propStream.close();
            }
        }
        assertNotNull(connidSoapVersion);
        assertNotNull(bundlesDirectory);
    }
}
