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
package org.apache.syncope.core.workflow.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DefaultUserWorkflowAdapterTest extends AbstractTest {

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = IdRepoEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterAll
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void createInvalidPassword() {
        UserCR userCR = new UserCR();
        userCR.setUsername("username");
        userCR.setRealm("/even/two");
        userCR.setPassword("pass");

        assertThrows(InvalidEntityException.class, () -> uwfAdapter.create(userCR, "admin", "test"));
    }

    @Test
    public void createInvalidUsername() {
        UserCR userCR = new UserCR();
        userCR.setUsername("username!");
        userCR.setRealm("/even/two");
        userCR.setPassword("password123");

        assertThrows(InvalidEntityException.class, () -> uwfAdapter.create(userCR, "admin", "test"));
    }

    @Test
    public void passwordHistory() {
        UserCR userCR = new UserCR();
        userCR.setUsername("username");
        userCR.setRealm("/even/two");
        userCR.setPassword("password123");

        UserWorkflowResult<Pair<String, Boolean>> result = uwfAdapter.create(userCR, "admin", "test");

        User user = userDAO.findById(result.getResult().getLeft()).orElseThrow();
        assertEquals(1, user.getPasswordHistory().size());
    }
}
