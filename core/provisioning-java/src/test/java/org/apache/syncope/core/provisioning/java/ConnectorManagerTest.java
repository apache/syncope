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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ConnectorManagerTest extends AbstractTest {

    @Autowired
    private ConnIdBundleManager connIdBundleManager;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    private DefaultConnectorManager connManager;

    @BeforeEach
    public void before() {
        connManager = new DefaultConnectorManager(connIdBundleManager, null, null, resourceDAO, null, null, null);

        // Remove any other connector instance bean set up by standard ConnectorManager.load()
        connManager.unload();
    }

    @Test
    public void load() {
        connManager.load();

        // only consider local connector bundles
        long expected = resourceDAO.findAll().stream().
                filter(resource -> resource.getConnector().getLocation().startsWith("file")).count();

        assertEquals(expected,
                ApplicationContextProvider.getBeanFactory().getBeanNamesForType(Connector.class, false, true).length);
    }
}
