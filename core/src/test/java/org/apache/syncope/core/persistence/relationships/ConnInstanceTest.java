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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ConnInstanceTest extends AbstractDAOTest {

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
        assertEquals(4, resources.size());
        assertTrue(
                "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(0).getName())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(1).getName())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(2).getName())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(3).getName()));

        connInstance.addCapability(ConnectorCapability.SEARCH);

        connInstance = connInstanceDAO.save(connInstance);
        assertNotNull(connInstance);
        assertFalse(connInstance.getCapabilities().isEmpty());

        resources = connInstance.getResources();
        assertNotNull(resources);
        assertEquals(4, resources.size());
        assertTrue(
                "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(0).getName())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(1).getName())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(2).getName())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(3).getName()));
    }
}
