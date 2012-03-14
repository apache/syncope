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
package org.syncope.core.audit;

import ch.qos.logback.core.db.ConnectionSource;
import ch.qos.logback.core.db.ConnectionSourceBase;
import ch.qos.logback.core.db.DataSourceConnectionSource;
import ch.qos.logback.core.db.JNDIConnectionSource;
import ch.qos.logback.core.db.dialect.SQLDialectCode;
import ch.qos.logback.core.spi.ContextAwareBase;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;

/**
 * The <id>FallbackConnectionSource</id> is an implementation of
 * {@link ConnectionSource} that attempts at first to obtain a {@link javax.sql.DataSource} from a JNDI provider and, if
 * not found, from a provided {@link javax.sql.DataSource DataSource}.
 *
 * @author <a href="mailto:rdecampo@twcny.rr.com">Ray DeCampo</a>
 */
public class JNDIFallbackConnectionSource extends ContextAwareBase implements ConnectionSource {

    private String jndiLocation;

    private DataSource dataSource;

    private ConnectionSourceBase delegate;

    public String getJndiLocation() {
        return jndiLocation;
    }

    public void setJndiLocation(final String jndiLocation) {
        this.jndiLocation = jndiLocation;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void chooseDelegate() {
        if (delegate != null) {
            return;
        }

        JNDIConnectionSource jndiCS = new JNDIConnectionSource();
        jndiCS.setJndiLocation(jndiLocation);
        try {
            Context ctx = new InitialContext();
            Object obj = ctx.lookup(jndiCS.getJndiLocation());

            PortableRemoteObject.narrow(obj, DataSource.class);

            delegate = jndiCS;
            addInfo("DataSource obtained from " + jndiLocation);
        } catch (NamingException e) {
            addError("During lookup of " + jndiLocation);
        } catch (ClassCastException e) {
            addError("Object at " + jndiLocation + " does not seem to be a DataSource instance", e);
        }

        if (delegate == null) {
            addInfo("Could not obtain DataSource via JNDI");

            DataSourceConnectionSource dataSourceCS = new DataSourceConnectionSource();
            dataSourceCS.setDataSource(dataSource);
            Connection conn = null;
            try {
                conn = dataSourceCS.getConnection();

                delegate = dataSourceCS;
                addInfo("Provided DataSource successfully reported");
            } catch (SQLException e) {
                addError("While trying to get connection from DataSource " + dataSource, e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException sqle) {
                        addError("Could not close connection", sqle);
                    }
                }
            }
        }

        if (delegate != null) {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isStarted() {
        chooseDelegate();
        return delegate.isStarted();
    }

    @Override
    public void start() {
        chooseDelegate();
        delegate.start();
    }

    @Override
    public void stop() {
        chooseDelegate();
        delegate.stop();
    }

    @Override
    public Connection getConnection()
            throws SQLException {

        chooseDelegate();
        return delegate.getConnection();
    }

    @Override
    public SQLDialectCode getSQLDialectCode() {
        chooseDelegate();
        return delegate.getSQLDialectCode();
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        chooseDelegate();
        return delegate.supportsGetGeneratedKeys();
    }

    @Override
    public boolean supportsBatchUpdates() {
        chooseDelegate();
        return delegate.supportsBatchUpdates();
    }
}
