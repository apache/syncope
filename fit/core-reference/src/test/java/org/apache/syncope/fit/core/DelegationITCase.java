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

import java.security.AccessControlException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
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
            delegationService.create(delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
        }

        delegation.setStart(OffsetDateTime.now());
        delegation.setEnd(OffsetDateTime.now().minusSeconds(1));

        // end before start -> FAIL
        try {
            delegationService.create(delegation);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
        }

        delegation.setEnd(OffsetDateTime.now());

        // 2. create delegation
        delegation = create(delegationService, delegation);
        assertNotNull(delegation.getKey());
        assertNotNull(delegation.getEnd());

        // 3. verify delegation is reported for users
        delegating = userService.read(delegating.getKey());
        assertEquals(List.of(delegation.getKey()), delegating.getDelegatingDelegations());
        assertEquals(List.of(), delegating.getDelegatedDelegations());

        delegated = userService.read(delegated.getKey());
        assertEquals(List.of(), delegated.getDelegatingDelegations());
        assertEquals(List.of(delegation.getKey()), delegated.getDelegatedDelegations());

        // 4. update and read delegation
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
        assertEquals(List.of(), delegating.getDelegatingDelegations());
        assertEquals(List.of(), delegating.getDelegatedDelegations());

        delegated = userService.read(delegated.getKey());
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
        AuditLoggerName authLoginSuccess = new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                UserLogic.class.getSimpleName(),
                null,
                "search",
                AuditElements.Result.SUCCESS);
        AuditConfTO authLogin = new AuditConfTO();
        authLogin.setKey(authLoginSuccess.toAuditKey());
        authLogin.setActive(true);
        auditService.create(authLogin);

        // 1. bellini delegates rossini
        DelegationTO delegation = new DelegationTO();
        delegation.setDelegating("bellini");
        delegation.setDelegated("rossini");
        delegation.setStart(OffsetDateTime.now());
        delegation = create(delegationService, delegation);
        assertNotNull(delegation.getKey());

        // 2. search users as bellini
        SyncopeClient bellini = clientFactory.create("bellini", "password");
        int forBellini = bellini.getService(UserService.class).search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build()).getTotalCount();

        SyncopeClient rossini = clientFactory.create("rossini", "password");

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
        int forRossini = rossini.delegatedBy("bellini").getService(UserService.class).search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build()).getTotalCount();
        assertEquals(forBellini, forRossini);

        // 4. delete delegation: searching users as rossini does not work, even with delegation
        delegationService.delete(delegation.getKey());

        try {
            rossini.getService(UserService.class).search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 5. query audit entries
        AuditQuery query = new AuditQuery.Builder().
                type(authLoginSuccess.getType()).
                category(authLoginSuccess.getCategory()).
                event(authLoginSuccess.getEvent()).
                result(authLoginSuccess.getResult()).
                build();
        List<AuditEntry> entries = query(query, MAX_WAIT_SECONDS);
        assertTrue(entries.stream().anyMatch(entry -> "rossini [delegated by bellini]".equals(entry.getWho())));

        // 6. disable audit
        authLogin.setActive(false);
        auditService.update(authLogin);
    }
}
