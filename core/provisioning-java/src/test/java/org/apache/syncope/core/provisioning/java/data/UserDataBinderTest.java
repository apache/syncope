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
import java.util.stream.Stream;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserDataBinderTest extends AbstractTest {

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = Stream.concat(
                IdRepoEntitlement.values().stream(),
                Stream.of(AnyEntitlement.READ.getFor("PRINTER"))).
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
    private UserDataBinder binder;

    @Autowired
    private RelationshipTypeDataBinder relationshipTypeDataBinder;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private EncryptorManager encryptorManager;

    @Test
    public void membershipWithAttrNotAllowed() {
        UserUR userUR = new UserUR.Builder("1417acbe-cbf6-4277-9372-e75e04f97000").build();

        // add 'obscure' to user (no membership): works because 'obscure' is from 'other', default class for USER
        userUR.getPlainAttrs().
                add(new AttrPatch.Builder(new Attr.Builder("obscure").value("testvalue").build()).build());

        // add 'obscure' to user (via 'artDirector' membership): does not work because 'obscure' is from 'other'
        // but 'artDirector' defines no type extension
        userUR.getMemberships().add(new MembershipUR.Builder("ece66293-8f31-4a84-8e8d-23da36e70846").
                plainAttr(new Attr.Builder("obscure").value("testvalue2").build()).build());

        assertThrows(
                InvalidEntityException.class,
                () -> binder.update(userDAO.findById(userUR.getKey()).orElseThrow(), userUR));
    }

    @Test
    public void membershipWithAttr() {
        UserUR userUR = new UserUR.Builder("1417acbe-cbf6-4277-9372-e75e04f97000").build();

        // add 'obscure' (no membership): works because 'obscure' is from 'other', default class for USER
        userUR.getPlainAttrs().
                add(new AttrPatch.Builder(new Attr.Builder("obscure").value("testvalue").build()).build());

        // add 'obscure' (via 'additional' membership): that group defines type extension with classes 'other' and 'csv'
        userUR.getMemberships().add(new MembershipUR.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").
                plainAttr(new Attr.Builder("obscure").value("testvalue2").build()).build());

        binder.update(userDAO.findById(userUR.getKey()).orElseThrow(), userUR);

        User user = userDAO.findById(userUR.getKey()).orElseThrow();
        UMembership newM = user.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").orElseThrow();
        assertEquals(1, user.getPlainAttrs(newM).size());

        assertNull(user.getPlainAttr("obscure").orElseThrow().getMembership());
        assertTrue(encryptorManager.getInstance().verify(
                "testvalue2",
                user.getCipherAlgorithm(),
                user.getPlainAttr("obscure", newM).orElseThrow().getValuesAsStrings().getFirst()));
    }

    @Test
    public void relationshipWithAttr() {
        // first add type extension to neighborhood
        RelationshipType relType = relationshipTypeDAO.findById("neighborhood").orElseThrow();
        RelationshipTypeTO relTypeTO = relationshipTypeDataBinder.getRelationshipTypeTO(relType);

        TypeExtensionTO typeExt = new TypeExtensionTO();
        typeExt.setAnyType(AnyTypeKind.USER.name());
        typeExt.getAuxClasses().add("other");
        relTypeTO.getTypeExtensions().add(typeExt);

        relType = relationshipTypeDAO.save(relationshipTypeDataBinder.update(relType, relTypeTO));
        relTypeTO = relationshipTypeDataBinder.getRelationshipTypeTO(relType);
        assertEquals("other", relTypeTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().getFirst());

        // then add relationship with attribute
        UserUR userUR = new UserUR.Builder("1417acbe-cbf6-4277-9372-e75e04f97000").
                relationship(new RelationshipUR.Builder("neighborhood").
                        otherEnd("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").
                        plainAttr(new Attr.Builder("obscure").value("testvalue3").build()).
                        build()).
                build();

        binder.update(userDAO.findById(userUR.getKey()).orElseThrow(), userUR);

        User user = userDAO.findById(userUR.getKey()).orElseThrow();
        URelationship newR = user.getRelationship(relType, "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        assertEquals(1, user.getPlainAttrs(newR).size());

        assertTrue(encryptorManager.getInstance().verify(
                "testvalue3",
                user.getCipherAlgorithm(),
                user.getPlainAttr("obscure", newR).orElseThrow().getValuesAsStrings().getFirst()));
    }
}
