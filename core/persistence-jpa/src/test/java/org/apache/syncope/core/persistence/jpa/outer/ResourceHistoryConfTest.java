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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceHistoryConfDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResourceHistoryConf;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ResourceHistoryConfTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ExternalResourceHistoryConfDAO resourceHistoryConfDAO;

    @Test
    public void createDelete() {
        ExternalResource ldapResource = resourceDAO.find("resource-ldap");
        assertNotNull(ldapResource);

        ExternalResourceHistoryConf ldapHistory = entityFactory.newEntity(ExternalResourceHistoryConf.class);
        ldapHistory.setCreation(new Date());
        ldapHistory.setCreator("me");
        ldapHistory.setEntity(ldapResource);
        ldapHistory.setConf(new ResourceTO());

        ldapHistory = resourceHistoryConfDAO.save(ldapHistory);
        assertNotNull(ldapHistory.getKey());

        resourceHistoryConfDAO.flush();

        List<ExternalResourceHistoryConf> history = resourceHistoryConfDAO.findByEntity(ldapResource);
        assertEquals(1, history.size());
        assertEquals(ldapHistory, history.get(0));

        resourceHistoryConfDAO.delete(ldapHistory.getKey());

        resourceHistoryConfDAO.flush();

        assertNull(resourceHistoryConfDAO.find(ldapHistory.getKey()));
        assertTrue(resourceHistoryConfDAO.findByEntity(ldapResource).isEmpty());
    }
}
