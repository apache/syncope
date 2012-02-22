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
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.AbstractTest;
import org.syncope.types.ConnectorCapability;

@Transactional
public class ConnInstanceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public void deleteCascade() {
        ConnInstance connInstance = connInstanceDAO.find(103L);
        assertNotNull(connInstance);

        List<ExternalResource> resources = connInstance.getResources();
        assertNotNull(resources);
        assertFalse(resources.isEmpty());

        connInstanceDAO.delete(connInstance.getId());

        connInstanceDAO.flush();

        ConnInstance actual = connInstanceDAO.find(103L);
        assertNull(actual);

        for (ExternalResource resource : resources) {
            assertNull(resourceDAO.find(resource.getName()));
        }
    }

    /**
     * Connector change used to miss connector bean registration.
     * 
     * http://code.google.com/p/syncope/issues/detail?id=176
     */
    @Test
    public void issue176() {
        ConnInstance connInstance = connInstanceDAO.find(103L);
        assertNotNull(connInstance);
        assertTrue(connInstance.getCapabilities().isEmpty());

        List<ExternalResource> resources = connInstance.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals(
                "ws-target-resource-nopropagation", resources.get(0).getName());

        connInstance.addCapability(ConnectorCapability.SEARCH);

        connInstance = connInstanceDAO.save(connInstance);
        assertNotNull(connInstance);
        assertFalse(connInstance.getCapabilities().isEmpty());

        resources = connInstance.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals(
                "ws-target-resource-nopropagation", resources.get(0).getName());
    }
}
