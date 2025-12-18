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
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.RelationshipTypeExtension;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;

public class AnyTypeClassRepoExtImpl implements AnyTypeClassRepoExt {

    protected final AnyTypeDAO anyTypeDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final GroupDAO groupDAO;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityManager entityManager;

    public AnyTypeClassRepoExtImpl(
            final AnyTypeDAO anyTypeDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final GroupDAO groupDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        this.anyTypeDAO = anyTypeDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.groupDAO = groupDAO;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.resourceDAO = resourceDAO;
        this.entityManager = entityManager;
    }

    @Override
    public AnyTypeClass save(final AnyTypeClass anyTypeClass) {
        AnyTypeClass merge = entityManager.merge(anyTypeClass);

        for (PlainSchema schema : merge.getPlainSchemas()) {
            schema.setAnyTypeClass(merge);
        }
        for (DerSchema schema : merge.getDerSchemas()) {
            schema.setAnyTypeClass(merge);
        }

        return merge;
    }

    @Override
    public void deleteById(final String key) {
        AnyTypeClass anyTypeClass = entityManager.find(JPAAnyTypeClass.class, key);
        if (anyTypeClass == null) {
            return;
        }

        for (PlainSchema schema : plainSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }
        for (DerSchema schema : derSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }

        for (AnyType type : anyTypeDAO.findByClassesContaining(anyTypeClass)) {
            type.getClasses().remove(anyTypeClass);
        }

        for (GroupTypeExtension typeExt : groupDAO.findTypeExtensions(anyTypeClass)) {
            typeExt.getAuxClasses().remove(anyTypeClass);

            if (typeExt.getAuxClasses().isEmpty()) {
                typeExt.getGroup().getTypeExtensions().remove(typeExt);
                typeExt.setGroup(null);
            }
        }
        for (RelationshipTypeExtension typeExt : relationshipTypeDAO.findTypeExtensions(anyTypeClass)) {
            typeExt.getAuxClasses().remove(anyTypeClass);

            if (typeExt.getAuxClasses().isEmpty()) {
                typeExt.getRelationshipType().getTypeExtensions().remove(typeExt);
                typeExt.setRelationshipType(null);
            }
        }

        resourceDAO.findAll().stream().
                flatMap(resource -> resource.getProvisions().stream()).
                filter(provision -> provision.getAuxClasses().contains(anyTypeClass.getKey())).
                forEach(provision -> provision.getAuxClasses().remove(anyTypeClass.getKey()));

        entityManager.remove(anyTypeClass);
    }
}
