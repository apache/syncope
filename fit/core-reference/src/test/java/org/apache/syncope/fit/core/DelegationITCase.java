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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Date;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.DelegationService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class DelegationITCase extends AbstractITCase {

    private DelegationTO create(final DelegationService ds, final DelegationTO delegation) {
        Response response = ds.create(delegation);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), DelegationService.class, DelegationTO.class);
    }

    @Test
    public void crudAsAdmin() {
        // 1. create users        
        UserTO delegating = UserITCase.getUniqueSampleTO("delegating@syncope.apache.org");
        delegating.getRoles().add("User reviewer");
        delegating = createUser(delegating).getEntity();
        assertNotNull(delegating.getKey());

        UserTO delegated = UserITCase.getUniqueSampleTO("delegated@syncope.apache.org");
        delegated = createUser(delegated).getEntity();
        assertNotNull(delegated.getKey());

        DelegationTO delegation = new DelegationTO();
        delegation.setDelegating(delegating.getKey());
        delegation.setDelegated(delegated.getKey());

        // no dates set -> FAIL
        try {
            delegationService.create(delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
        }

        delegation.setStart(new Date());
        delegation.setEnd(new Date(System.currentTimeMillis() - 1000));

        // end before start -> FAIL
        try {
            delegationService.create(delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
        }

        delegation.setEnd(new Date());

        // 2. create delegation
        delegation = create(delegationService, delegation);
        assertNotNull(delegation.getKey());
        assertNotNull(delegation.getEnd());

        // 3. verify delegation is reported for users
        delegating = userService.read(delegating.getKey());
        assertEquals(Collections.singletonList(delegation.getKey()), delegating.getDelegatingDelegations());
        assertEquals(Collections.emptyList(), delegating.getDelegatedDelegations());

        delegated = userService.read(delegated.getKey());
        assertEquals(Collections.emptyList(), delegated.getDelegatingDelegations());
        assertEquals(Collections.singletonList(delegation.getKey()), delegated.getDelegatedDelegations());

        // 4. update delegation
        delegation.setEnd(null);
        delegationService.update(delegation);

        delegation = delegationService.read(delegation.getKey());
        assertNull(delegation.getEnd());

        // 5. delete delegation
        delegationService.delete(delegation.getKey());

        try {
            delegationService.read(delegation.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 6. verify delegation is not reported for users
        delegating = userService.read(delegating.getKey());
        assertEquals(Collections.emptyList(), delegating.getDelegatingDelegations());
        assertEquals(Collections.emptyList(), delegating.getDelegatedDelegations());

        delegated = userService.read(delegated.getKey());
        assertEquals(Collections.emptyList(), delegated.getDelegatingDelegations());
        assertEquals(Collections.emptyList(), delegated.getDelegatedDelegations());
    }

    @Test
    public void crudAsUser() {
        // 1. create users        
        UserTO delegating = UserITCase.getUniqueSampleTO("delegating@syncope.apache.org");
        delegating.getRoles().add("User reviewer");
        delegating = createUser(delegating).getEntity();
        assertNotNull(delegating.getKey());

        UserTO delegated = UserITCase.getUniqueSampleTO("delegated@syncope.apache.org");
        delegated = createUser(delegated).getEntity();
        assertNotNull(delegated.getKey());

        DelegationTO delegation = new DelegationTO();
        delegation.setDelegating("c9b2dec2-00a7-4855-97c0-d854842b4b24");
        delegation.setDelegated(delegated.getKey());
        delegation.setStart(new Date());

        DelegationService uds = clientFactory.create(delegating.getUsername(), "password123").
                getService(DelegationService.class);

        // delegating user is not requesting user -> FAIL
        try {
            create(uds, delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 2. create delegation
        delegation.setDelegating(delegating.getKey());

        delegation = create(uds, delegation);
        assertNotNull(delegation.getKey());
        assertNull(delegation.getEnd());

        // 3. update delegation
        delegation.setEnd(new Date());
        uds.update(delegation);

        delegation = uds.read(delegation.getKey());
        assertNotNull(delegation.getEnd());

        // 4. delete delegation
        uds.delete(delegation.getKey());

        try {
            uds.read(delegation.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }
}
