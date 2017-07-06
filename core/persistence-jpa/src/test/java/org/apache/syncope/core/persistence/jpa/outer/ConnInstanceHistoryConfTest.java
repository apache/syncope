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
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceHistoryConfDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ConnInstanceHistoryConf;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ConnInstanceHistoryConfTest extends AbstractTest {

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConnInstanceHistoryConfDAO connInstanceHistoryConfDAO;

    @Test
    public void createDelete() {
        ConnInstance ldapConnector = connInstanceDAO.find("74141a3b-0762-4720-a4aa-fc3e374ef3ef");
        assertNotNull(ldapConnector);

        ConnInstanceHistoryConf ldapHistory = entityFactory.newEntity(ConnInstanceHistoryConf.class);
        ldapHistory.setCreation(new Date());
        ldapHistory.setCreator("me");
        ldapHistory.setEntity(ldapConnector);
        ldapHistory.setConf(new ConnInstanceTO());

        ldapHistory = connInstanceHistoryConfDAO.save(ldapHistory);
        assertNotNull(ldapHistory.getKey());

        connInstanceHistoryConfDAO.flush();

        List<ConnInstanceHistoryConf> history = connInstanceHistoryConfDAO.findByEntity(ldapConnector);
        assertEquals(1, history.size());
        assertEquals(ldapHistory, history.get(0));

        connInstanceHistoryConfDAO.delete(ldapHistory.getKey());

        connInstanceHistoryConfDAO.flush();

        assertNull(connInstanceHistoryConfDAO.find(ldapHistory.getKey()));
        assertTrue(connInstanceHistoryConfDAO.findByEntity(ldapConnector).isEmpty());
    }
}
