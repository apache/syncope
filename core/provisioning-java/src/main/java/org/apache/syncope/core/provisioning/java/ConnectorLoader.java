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
package org.apache.syncope.core.provisioning.java;

import javax.sql.DataSource;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;

public class ConnectorLoader implements SyncopeCoreLoader {

    protected final ConnectorManager connectorManager;

    public ConnectorLoader(final ConnectorManager connectorManager) {
        this.connectorManager = connectorManager;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        AuthContextUtils.callAsAdmin(domain, () -> {
            connectorManager.load();
            return null;
        });
    }
}
