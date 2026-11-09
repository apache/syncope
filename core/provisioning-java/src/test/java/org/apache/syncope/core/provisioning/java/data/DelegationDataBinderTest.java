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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DelegationDataBinderTest extends AbstractDataBinderTest {

    private static final String DELEGATING = "1417acbe-cbf6-4277-9372-e75e04f97000";

    private static final String DELEGATED = "74cd8ece-715a-44a4-a736-e17b46c4e7e6";

    @Autowired
    private DelegationDataBinder binder;

    @Autowired
    private DelegationDAO delegationDAO;

    @Test
    void createUpdate() {
        DelegationTO delegationTO = new DelegationTO();
        delegationTO.setDelegating(DELEGATING);
        delegationTO.setDelegated(DELEGATED);
        delegationTO.setStart(OffsetDateTime.now());
        delegationTO.setEnd(OffsetDateTime.now().plusDays(1));
        delegationTO.getRoles().add("Other");

        Delegation delegation = delegationDAO.save(binder.create(delegationTO));

        DelegationTO created = binder.getDelegationTO(delegation);
        assertEquals(DELEGATING, created.getDelegating());
        assertEquals(DELEGATED, created.getDelegated());
        assertEquals(List.of("Other"), List.copyOf(created.getRoles()));
        assertEquals(delegationTO.getStart(), created.getStart());
        assertEquals(delegationTO.getEnd(), created.getEnd());

        created.setEnd(OffsetDateTime.now().plusDays(2));
        created.setStart(OffsetDateTime.now().minusDays(1));
        binder.update(delegation, created);

        DelegationTO updated = binder.getDelegationTO(delegation);
        assertEquals(created.getStart(), updated.getStart());
        assertEquals(created.getEnd(), updated.getEnd());
        assertTrue(updated.getRoles().contains("Other"));
    }

    @Test
    void invalidRoles() {
        DelegationTO notOwned = new DelegationTO();
        notOwned.setDelegating(DELEGATING);
        notOwned.setDelegated(DELEGATED);
        notOwned.getRoles().add("User reviewer");
        assertThrows(SyncopeClientException.class, () -> binder.create(notOwned));

        DelegationTO notExisting = new DelegationTO();
        notExisting.setDelegating(DELEGATING);
        notExisting.setDelegated(DELEGATED);
        notExisting.getRoles().add("not-existing");
        assertThrows(SyncopeClientException.class, () -> binder.create(notExisting));
    }

    @Test
    void invalidUsers() {
        DelegationTO noDelegating = new DelegationTO();
        noDelegating.setDelegating("not-existing");
        noDelegating.setDelegated(DELEGATED);
        assertThrows(NotFoundException.class, () -> binder.create(noDelegating));

        DelegationTO noDelegated = new DelegationTO();
        noDelegated.setDelegating(DELEGATING);
        noDelegated.setDelegated("not-existing");
        assertThrows(NotFoundException.class, () -> binder.create(noDelegated));
    }
}
