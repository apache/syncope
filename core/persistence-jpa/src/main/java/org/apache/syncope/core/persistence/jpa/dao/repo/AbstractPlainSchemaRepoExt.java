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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.AbstractSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALinkedAccount;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

abstract class AbstractPlainSchemaRepoExt extends AbstractSchemaRepoExt implements PlainSchemaRepoExt {

    protected static final List<String> TABLES = List.of(
            JPAUser.TABLE, JPAGroup.TABLE, JPAAnyObject.TABLE, JPALinkedAccount.TABLE);

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final ExternalResourceDAO resourceDAO;

    protected AbstractPlainSchemaRepoExt(
            final AnyUtilsFactory anyUtilsFactory,
            final ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        super(entityManager);
        this.anyUtilsFactory = anyUtilsFactory;
        this.resourceDAO = resourceDAO;
    }

    @Override
    public List<? extends PlainSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        return findByAnyTypeClasses(anyTypeClasses, JPAPlainSchema.class.getSimpleName(), PlainSchema.class);
    }

    @Override
    public PlainSchema save(final PlainSchema schema) {
        ((AbstractSchema) schema).map2json();
        PlainSchema merged = entityManager.merge(schema);
        ((AbstractSchema) merged).postSave();
        return merged;
    }

    @Override
    public void deleteById(final String key) {
        PlainSchema schema = entityManager.find(JPAPlainSchema.class, key);
        if (schema == null) {
            return;
        }

        resourceDAO.deleteMapping(key);

        Optional.ofNullable(schema.getAnyTypeClass()).ifPresent(c -> c.getPlainSchemas().remove(schema));

        entityManager.remove(schema);
    }
}
