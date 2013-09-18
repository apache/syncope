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
package org.apache.syncope.core.audit;

import ch.qos.logback.core.db.DataSourceConnectionSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.dao.impl.AbstractContentDealer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

/**
 * Specialization of {@link DataSourceConnectionSource} that first attempts to obtain a {@link javax.sql.DataSource}
 * from the JNDI name configured in Spring or, when not found, builds a new {@link javax.sql.DataSource DataSource} via
 * Commons DBCP; if any datasource if found, the SQL init script is used to populate the database.
 */
public class JNDIFallbackConnectionSource extends DataSourceConnectionSource {

    private static final String PERSISTENCE_CONTEXT = "/persistenceContext.xml";

    private static DataSource datasource;

    static {
        // 1. Attempts to lookup for configured JNDI datasource (if present and available)
        InputStream springConf = JNDIFallbackConnectionSource.class.getResourceAsStream(PERSISTENCE_CONTEXT);
        try {
            DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
            LSParser parser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
            LSInput lsinput = impl.createLSInput();
            lsinput.setByteStream(springConf);

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//*[local-name()='property' and @name='jndiName']/@value");
            String jndiName = (String) expr.evaluate(parser.parse(lsinput), XPathConstants.STRING);

            Context ctx = new InitialContext();
            Object obj = ctx.lookup(jndiName);

            datasource = (DataSource) PortableRemoteObject.narrow(obj, DataSource.class);
        } catch (Exception e) {
            // ignore
        } finally {
            IOUtils.closeQuietly(springConf);
        }

        // 2. Creates Commons DBCP datasource
        String initSQLScript = null;
        try {
            Properties persistence = PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource(AbstractContentDealer.PERSISTENCE_PROPERTIES));

            initSQLScript = persistence.getProperty("logback.sql");

            if (datasource == null) {
                BasicDataSource bds = new BasicDataSource();
                bds.setDriverClassName(persistence.getProperty("jpa.driverClassName"));
                bds.setUrl(persistence.getProperty("jpa.url"));
                bds.setUsername(persistence.getProperty("jpa.username"));
                bds.setPassword(persistence.getProperty("jpa.password"));

                bds.setLogAbandoned(true);
                bds.setRemoveAbandoned(true);

                datasource = bds;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Audit datasource configuration failed", e);
        }

        // 3. Initializes the chosen datasource
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setScripts(new Resource[] {new ClassPathResource("/logback/" + initSQLScript)});
        // forces statement separation via ;; in order to support stored procedures
        populator.setSeparator(";;");
        Connection conn = DataSourceUtils.getConnection(datasource);
        try {
            populator.populate(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not init the Audit datasource", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, datasource);
        }
    }

    public JNDIFallbackConnectionSource() {
        super.setDataSource(datasource);
    }
}
