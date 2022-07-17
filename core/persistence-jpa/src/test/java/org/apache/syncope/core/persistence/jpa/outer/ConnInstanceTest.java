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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ConnInstanceTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public void deleteCascade() {
        ConnInstance connInstance = connInstanceDAO.find("fcf9f2b0-f7d6-42c9-84a6-61b28255a42b");
        assertNotNull(connInstance);

        List<? extends ExternalResource> resources = connInstance.getResources();
        assertNotNull(resources);
        assertFalse(resources.isEmpty());

        connInstanceDAO.delete(connInstance.getKey());

        entityManager().flush();

        ConnInstance actual = connInstanceDAO.find("fcf9f2b0-f7d6-42c9-84a6-61b28255a42b");
        assertNull(actual);

        for (ExternalResource resource : resources) {
            assertNull(resourceDAO.find(resource.getKey()));
        }
    }

    @Test
    public void issue176() {
        ConnInstance connInstance = connInstanceDAO.find("fcf9f2b0-f7d6-42c9-84a6-61b28255a42b");
        assertNotNull(connInstance);
        assertTrue(connInstance.getCapabilities().isEmpty());

        List<? extends ExternalResource> resources = connInstance.getResources();
        assertNotNull(resources);
        assertEquals(4, resources.size());
        assertTrue(
                "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(0).getKey())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(1).getKey())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(2).getKey())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(3).getKey()));

        connInstance.getCapabilities().add(ConnectorCapability.SEARCH);

        connInstance = connInstanceDAO.save(connInstance);
        assertNotNull(connInstance);
        assertFalse(connInstance.getCapabilities().isEmpty());

        resources = connInstance.getResources();
        assertNotNull(resources);
        assertEquals(4, resources.size());
        assertTrue(
                "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(0).getKey())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(1).getKey())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(2).getKey())
                || "ws-target-resource-nopropagation".equalsIgnoreCase(resources.get(3).getKey()));
    }
}
