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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AttrRepoDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private AttrRepoDataBinder binder;

    @Autowired
    private AttrRepoDAO attrRepoDAO;

    @Test
    void get() {
        AttrRepoTO attrRepo = binder.getAttrRepoTO(
                attrRepoDAO.findById("DefaultStubAttrRepo").orElseThrow());
        assertEquals("DefaultStubAttrRepo", attrRepo.getKey());
        assertEquals("Stub attr repo", attrRepo.getDescription());
        assertEquals(AttrRepoState.ACTIVE, attrRepo.getState());
        assertEquals(1, attrRepo.getItems().size());
        assertEquals("attr1", attrRepo.getItems().getFirst().getIntAttrName());
        assertEquals(MappingPurpose.NONE, attrRepo.getItems().getFirst().getPurpose());
    }

    @Test
    void createUpdate() {
        AttrRepoTO attrRepoTO = new AttrRepoTO();
        attrRepoTO.setKey("testAttrRepo");
        attrRepoTO.setDescription("a test attr repo");
        attrRepoTO.setState(AttrRepoState.STANDBY);
        attrRepoTO.setOrder(5);
        attrRepoTO.setConf(binder.getAttrRepoTO(
                attrRepoDAO.findById("DefaultStubAttrRepo").orElseThrow()).getConf());

        Item item = new Item();
        item.setIntAttrName("email");
        item.setExtAttrName("mail");
        item.setMandatoryCondition("false");
        attrRepoTO.getItems().add(item);

        AttrRepo attrRepo = attrRepoDAO.save(binder.create(attrRepoTO));

        AttrRepoTO created = binder.getAttrRepoTO(attrRepo);
        assertEquals("testAttrRepo", created.getKey());
        assertEquals("a test attr repo", created.getDescription());
        assertEquals(AttrRepoState.STANDBY, created.getState());
        assertEquals(5, created.getOrder());
        assertEquals(1, created.getItems().size());

        created.setDescription("an updated test attr repo");
        created.getItems().getFirst().setExtAttrName("email");
        binder.update(attrRepo, created);

        AttrRepoTO updated = binder.getAttrRepoTO(attrRepo);
        assertEquals("an updated test attr repo", updated.getDescription());
        assertEquals("email", updated.getItems().getFirst().getExtAttrName());
    }

    @Test
    void invalidItems() {
        AttrRepoTO noIntAttrName = new AttrRepoTO();
        noIntAttrName.setKey("testAttrRepo");
        noIntAttrName.getItems().add(new Item());
        assertThrows(SyncopeClientException.class, () -> binder.create(noIntAttrName));

        AttrRepoTO invalidMandatoryCondition = new AttrRepoTO();
        invalidMandatoryCondition.setKey("testAttrRepo");
        Item item = new Item();
        item.setIntAttrName("email");
        item.setMandatoryCondition("1 +");
        invalidMandatoryCondition.getItems().add(item);
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidMandatoryCondition));

        AttrRepoTO nullItem = new AttrRepoTO();
        nullItem.setKey("testAttrRepo");
        nullItem.getItems().add(null);
        assertThrows(SyncopeClientException.class, () -> binder.create(nullItem));
    }
}
