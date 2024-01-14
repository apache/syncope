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
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.springframework.context.ApplicationEventPublisher;

public class GroupRepoExtJSONImpl extends GroupRepoExtImpl {

    protected final JPAJSONAnyDAO anyDAO;

    public GroupRepoExtJSONImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final AnyMatchDAO anyMatchDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final JPAJSONAnyDAO anyDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager) {

        super(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                anyMatchDAO,
                userDAO,
                anyObjectDAO,
                searchDAO,
                searchCondVisitor,
                entityManager);
        this.anyDAO = anyDAO;
    }

    @Override
    public List<Group> findByPlainAttrValue(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByPlainAttrValue(
                JPAGroup.TABLE, anyUtils, schema, attrValue, ignoreCaseMatch);
    }

    @Override
    public Optional<Group> findByPlainAttrUniqueValue(
            final PlainSchema schema,
            final PlainAttrUniqueValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByPlainAttrUniqueValue(
                JPAGroup.TABLE, anyUtils, schema, attrUniqueValue, ignoreCaseMatch);
    }

    @Override
    public List<Group> findByDerAttrValue(
            final DerSchema schema,
            final String value,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByDerAttrValue(JPAGroup.TABLE, anyUtils, schema, value, ignoreCaseMatch);
    }

    @Override
    public <S extends Group> S save(final S group) {
        anyDAO.checkBeforeSave(JPAGroup.TABLE, anyUtils, group);
        return entityManager.merge(group);
    }
}
