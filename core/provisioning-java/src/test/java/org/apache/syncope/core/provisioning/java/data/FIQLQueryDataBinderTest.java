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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.provisioning.api.data.FIQLQueryDataBinder;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class FIQLQueryDataBinderTest extends AbstractDataBinderTest {

    private static final String OWNER = "rossini";

    @Autowired
    private FIQLQueryDataBinder binder;

    @Autowired
    private FIQLQueryDAO fiqlQueryDAO;

    protected void authenticateAs(final String username) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new User(username, "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    protected void authenticateAsAdmin() {
        authenticateAs("admin");
    }

    @Test
    void createGet() {
        authenticateAs(OWNER);
        try {
            FIQLQueryTO fiqlQueryTO = new FIQLQueryTO();
            fiqlQueryTO.setName("my query");
            fiqlQueryTO.setTarget(AnyTypeKind.USER.name());
            fiqlQueryTO.setFiql("username==rossini");

            FIQLQuery fiqlQuery = binder.create(fiqlQueryTO);
            fiqlQuery = fiqlQueryDAO.save(fiqlQuery);
            entityManager.flush();

            assertNotNull(fiqlQuery.getKey());
            assertEquals(OWNER, fiqlQuery.getOwner().getUsername());

            FIQLQueryTO actual = binder.getFIQLQueryTO(
                    fiqlQueryDAO.findById(fiqlQuery.getKey()).orElseThrow());
            assertEquals(fiqlQuery.getKey(), actual.getKey());
            assertEquals("my query", actual.getName());
            assertEquals(AnyTypeKind.USER.name(), actual.getTarget());
            assertEquals("username==rossini", actual.getFiql());
        } finally {
            authenticateAsAdmin();
        }
    }

    @Test
    void createBlankFiqlInvalid() {
        authenticateAs(OWNER);
        try {
            FIQLQueryTO fiqlQueryTO = new FIQLQueryTO();
            fiqlQueryTO.setName("blank query");
            fiqlQueryTO.setTarget(AnyTypeKind.GROUP.name());
            fiqlQueryTO.setFiql("   ");

            assertThrows(SyncopeClientException.class, () -> binder.create(fiqlQueryTO));
        } finally {
            authenticateAsAdmin();
        }
    }

    @Test
    void update() {
        authenticateAs(OWNER);
        try {
            FIQLQueryTO fiqlQueryTO = new FIQLQueryTO();
            fiqlQueryTO.setName("to update");
            fiqlQueryTO.setTarget(AnyTypeKind.USER.name());
            fiqlQueryTO.setFiql("username==verdi");

            FIQLQuery fiqlQuery = binder.create(fiqlQueryTO);

            FIQLQueryTO update = new FIQLQueryTO();
            update.setName("updated query");
            update.setTarget("PRINTER");
            update.setFiql("model==Canon");

            binder.update(fiqlQuery, update);
            fiqlQuery = fiqlQueryDAO.save(fiqlQuery);
            entityManager.flush();

            FIQLQueryTO actual = binder.getFIQLQueryTO(
                    fiqlQueryDAO.findById(fiqlQuery.getKey()).orElseThrow());
            assertEquals("updated query", actual.getName());
            assertEquals("PRINTER", actual.getTarget());
            assertEquals("model==Canon", actual.getFiql());
        } finally {
            authenticateAsAdmin();
        }
    }

    @Test
    void invalidFiql() {
        authenticateAs(OWNER);
        try {
            FIQLQueryTO fiqlQueryTO = new FIQLQueryTO();
            fiqlQueryTO.setName("invalid query");
            fiqlQueryTO.setTarget(AnyTypeKind.USER.name());
            fiqlQueryTO.setFiql("username==((");

            assertThrows(SyncopeClientException.class, () -> binder.create(fiqlQueryTO));
        } finally {
            authenticateAsAdmin();
        }
    }
}
