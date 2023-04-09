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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class UserDataBinderTest extends AbstractTest {

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = StandardEntitlement.values().stream().
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
    private UserDataBinder dataBinder;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntityFactory entityFactory;

    private String userKey;

    @BeforeEach
    public void createUser() {
        UserTO userTO = new UserTO();
        userTO.setRealm("/even");
        userTO.setUsername("test");
        userTO.setPassword("password123");
        userTO.getPlainAttrs().add(new AttrTO.Builder().schema("surname").value("test").build());
        userTO.getPlainAttrs().add(new AttrTO.Builder().schema("fullname").value("test").build());
        userTO.getPlainAttrs().add(new AttrTO.Builder().schema("userId").value("test@test.it").build());

        User user = entityFactory.newEntity(User.class);

        dataBinder.create(user, userTO, true);
        user = userDAO.save(user);

        userKey = user.getKey();
    }

    @Test
    public void membershipWithAttrNotAllowed() {
        UserPatch patch = new UserPatch();
        patch.setKey(userKey);

        // add 'obscure' to user (no membership): works because 'obscure' is from 'other', default class for USER
        patch.getPlainAttrs().add(new AttrPatch.Builder().
                attrTO(new AttrTO.Builder().schema("obscure").value("testvalue").build()).build());

        // add 'obscure' to user (via 'artDirector' membership): does not work because 'obscure' is from 'other'
        // but 'artDirector' defines no type extension
        MembershipPatch mp = new MembershipPatch.Builder().group("ece66293-8f31-4a84-8e8d-23da36e70846").build();
        mp.getPlainAttrs().add(new AttrTO.Builder().schema("obscure").value("testvalue2").build());
        patch.getMemberships().add(mp);

        assertThrows(InvalidEntityException.class, () -> dataBinder.update(userDAO.find(patch.getKey()), patch));
    }

    @Test
    public void membershipWithAttr() {
        UserPatch patch = new UserPatch();
        patch.setKey(userKey);

        // add 'obscure' (no membership): works because 'obscure' is from 'other', default class for USER
        patch.getPlainAttrs().add(new AttrPatch.Builder().
                attrTO(new AttrTO.Builder().schema("obscure").value("testvalue").build()).build());

        // add 'obscure' (via 'additional' membership): that group defines type extension with classes 'other' and 'csv'
        MembershipPatch mp = new MembershipPatch.Builder().group("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        mp.getPlainAttrs().add(new AttrTO.Builder().schema("obscure").value("testvalue2").build());
        patch.getMemberships().add(mp);

        dataBinder.update(userDAO.find(patch.getKey()), patch);

        User user = userDAO.find(patch.getKey());
        UMembership newM = user.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").get();
        assertEquals(1, user.getPlainAttrs(newM).size());

        assertNull(user.getPlainAttr("obscure").get().getMembership());
        assertEquals(2, user.getPlainAttrs("obscure").size());
        assertTrue(user.getPlainAttrs("obscure").contains(user.getPlainAttr("obscure").get()));
        assertTrue(user.getPlainAttrs("obscure").stream().anyMatch(a -> a.getMembership() == null));
        assertTrue(user.getPlainAttrs("obscure").stream().anyMatch(a -> newM.equals(a.getMembership())));
    }
}
