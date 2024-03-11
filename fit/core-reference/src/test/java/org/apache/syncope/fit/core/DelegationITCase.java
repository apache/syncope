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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.DelegationService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class DelegationITCase extends AbstractITCase {

    private DelegationTO create(final DelegationService ds, final DelegationTO delegation) {
        Response response = ds.create(delegation);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), DelegationService.class, DelegationTO.class);
    }

    @Test
    public void crudAsAdmin() {
        // 1. create users        
        UserCR delegatingCR = UserITCase.getUniqueSample("delegating@syncope.apache.org");
        delegatingCR.getRoles().add("User reviewer");
        UserTO delegating = createUser(delegatingCR).getEntity();
        assertNotNull(delegating.getKey());

        UserCR delegatedCR = UserITCase.getUniqueSample("delegated@syncope.apache.org");
        UserTO delegated = createUser(delegatedCR).getEntity();
        assertNotNull(delegated.getKey());

        DelegationTO delegation = new DelegationTO();
        delegation.setDelegating(delegating.getKey());
        delegation.setDelegated(delegated.getKey());

        // no dates set -> FAIL
        try {
            DELEGATION_SERVICE.create(delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
        }

        delegation.setStart(OffsetDateTime.now());
        delegation.setEnd(OffsetDateTime.now().minusSeconds(1));

        // end before start -> FAIL
        try {
            DELEGATION_SERVICE.create(delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
        }

        delegation.setEnd(OffsetDateTime.now());

        // 2. create delegation
        delegation = create(DELEGATION_SERVICE, delegation);
        assertNotNull(delegation.getKey());
        assertNotNull(delegation.getEnd());

        // 3. verify delegation is reported for users
        delegating = USER_SERVICE.read(delegating.getKey());
        assertEquals(List.of(delegation.getKey()), delegating.getDelegatingDelegations());
        assertEquals(List.of(), delegating.getDelegatedDelegations());

        delegated = USER_SERVICE.read(delegated.getKey());
        assertEquals(List.of(), delegated.getDelegatingDelegations());
        assertEquals(List.of(delegation.getKey()), delegated.getDelegatedDelegations());

        // 4. update and read delegation
        delegation.setEnd(null);
        DELEGATION_SERVICE.update(delegation);

        delegation = DELEGATION_SERVICE.read(delegation.getKey());
        assertNull(delegation.getEnd());

        // 5. delete delegation
        DELEGATION_SERVICE.delete(delegation.getKey());

        try {
            DELEGATION_SERVICE.read(delegation.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 6. verify delegation is not reported for users
        delegating = USER_SERVICE.read(delegating.getKey());
        assertEquals(List.of(), delegating.getDelegatingDelegations());
        assertEquals(List.of(), delegating.getDelegatedDelegations());

        delegated = USER_SERVICE.read(delegated.getKey());
        assertEquals(List.of(), delegated.getDelegatingDelegations());
        assertEquals(List.of(), delegated.getDelegatedDelegations());
    }

    @Test
    public void crudAsUser() {
        // 1. create users        
        UserCR delegatingCR = UserITCase.getUniqueSample("delegating@syncope.apache.org");
        delegatingCR.getRoles().add("User reviewer");
        UserTO delegating = createUser(delegatingCR).getEntity();
        assertNotNull(delegating.getKey());

        UserCR delegatedCR = UserITCase.getUniqueSample("delegated@syncope.apache.org");
        UserTO delegated = createUser(delegatedCR).getEntity();
        assertNotNull(delegated.getKey());

        DelegationTO delegation = new DelegationTO();
        delegation.setDelegating("c9b2dec2-00a7-4855-97c0-d854842b4b24");
        delegation.setDelegated(delegated.getKey());
        delegation.setStart(OffsetDateTime.now());

        DelegationService uds = CLIENT_FACTORY.create(delegating.getUsername(), "password123").
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

        // 3. update and read delegation
        delegation.setEnd(OffsetDateTime.now());
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

    @Test
    public void operations() {
        // 0. enable audit
        OpEvent authLoginSuccess = new OpEvent(
                OpEvent.CategoryType.LOGIC,
                UserLogic.class.getSimpleName(),
                null,
                "search",
                OpEvent.Outcome.SUCCESS);
        AuditConfTO authLogin = new AuditConfTO();
        authLogin.setKey(authLoginSuccess.toString());
        authLogin.setActive(true);
        AUDIT_SERVICE.setConf(authLogin);

        // 1. bellini delegates rossini
        DelegationTO delegation = new DelegationTO();
        delegation.setDelegating("bellini");
        delegation.setDelegated("rossini");
        delegation.setStart(OffsetDateTime.now());
        delegation = create(DELEGATION_SERVICE, delegation);
        assertNotNull(delegation.getKey());

        // 2. search users as bellini
        SyncopeClient bellini = CLIENT_FACTORY.create("bellini", "password");
        long forBellini = bellini.getService(UserService.class).search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build()).getTotalCount();

        SyncopeClient rossini = CLIENT_FACTORY.create("rossini", "password");

        // 3. search users as rossini
        Triple<Map<String, Set<String>>, List<String>, UserTO> self = rossini.self();
        assertEquals(List.of("bellini"), self.getMiddle());

        // 3a. search users as rossini without delegation -> FAIL
        try {
            rossini.getService(UserService.class).search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
            fail();
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        // 3b. search users as rossini with delegation -> SUCCESS
        long forRossini = rossini.delegatedBy("bellini").getService(UserService.class).search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build()).getTotalCount();
        if (!IS_EXT_SEARCH_ENABLED) {
            assertEquals(forBellini, forRossini);
        }

        // 4. delete delegation: searching users as rossini does not work, even with delegation
        DELEGATION_SERVICE.delete(delegation.getKey());

        try {
            rossini.getService(UserService.class).search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
            fail();
        } catch (NotAuthorizedException e) {
            assertNotNull(e);
        }

        // 5. query audit events
        AuditQuery query = new AuditQuery.Builder().
                type(authLoginSuccess.getType()).
                category(authLoginSuccess.getCategory()).
                op(authLoginSuccess.getOp()).
                outcome(authLoginSuccess.getOutcome()).
                build();
        List<AuditEventTO> events = query(query, MAX_WAIT_SECONDS);
        assertTrue(events.stream().anyMatch(event -> "rossini [delegated by bellini]".equals(event.getWho())));

        // 6. disable audit
        authLogin.setActive(false);
        AUDIT_SERVICE.setConf(authLogin);
    }
}
