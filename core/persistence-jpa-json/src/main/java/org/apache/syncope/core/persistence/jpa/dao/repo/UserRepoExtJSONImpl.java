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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONUser;
import org.apache.syncope.core.spring.security.SecurityProperties;

public class UserRepoExtJSONImpl extends UserRepoExtImpl {

    protected final JPAJSONAnyDAO anyDAO;

    public UserRepoExtJSONImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final JPAJSONAnyDAO anyDAO,
            final SecurityProperties securityProperties,
            final EntityManager entityManager) {

        super(
                anyUtilsFactory,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                roleDAO,
                accessTokenDAO,
                groupDAO,
                delegationDAO,
                fiqlQueryDAO,
                securityProperties,
                entityManager);
        this.anyDAO = anyDAO;
    }

    @Override
    public List<User> findByPlainAttrValue(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByPlainAttrValue(
                JPAJSONUser.TABLE, anyUtils, schema, attrValue, ignoreCaseMatch);
    }

    @Override
    public Optional<User> findByPlainAttrUniqueValue(
            final PlainSchema schema,
            final PlainAttrUniqueValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByPlainAttrUniqueValue(
                JPAJSONUser.TABLE, anyUtils, schema, attrUniqueValue, ignoreCaseMatch);
    }

    @Override
    public List<User> findByDerAttrValue(
            final DerSchema schema,
            final String value,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByDerAttrValue(JPAJSONUser.TABLE, anyUtils, schema, value, ignoreCaseMatch);
    }

    @Override
    protected Pair<User, Pair<Set<String>, Set<String>>> doSave(final User user) {
        entityManager.flush();
        User merged = entityManager.merge(user);

        // ensure that entity listeners are invoked at this point
        entityManager.flush();

        roleDAO.refreshDynMemberships(merged);
        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    public <S extends User> S save(final S user) {
        anyDAO.checkBeforeSave(JPAJSONUser.TABLE, anyUtils, user);
        return super.save(user);
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final User user) {
        anyDAO.checkBeforeSave(JPAJSONUser.TABLE, anyUtils, user);
        return super.saveAndGetDynGroupMembs(user);
    }
}
