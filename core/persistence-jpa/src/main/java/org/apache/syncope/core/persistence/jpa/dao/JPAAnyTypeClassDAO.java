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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;

public class JPAAnyTypeClassDAO extends AbstractDAO<AnyTypeClass> implements AnyTypeClassDAO {

    protected final AnyTypeDAO anyTypeDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final GroupDAO groupDAO;

    protected final ExternalResourceDAO resourceDAO;

    public JPAAnyTypeClassDAO(
            final AnyTypeDAO anyTypeDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final GroupDAO groupDAO,
            final ExternalResourceDAO resourceDAO) {

        this.anyTypeDAO = anyTypeDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.groupDAO = groupDAO;
        this.resourceDAO = resourceDAO;
    }

    @Override
    public AnyTypeClass find(final String key) {
        return entityManager().find(JPAAnyTypeClass.class, key);
    }

    @Override
    public List<AnyTypeClass> findAll() {
        TypedQuery<AnyTypeClass> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAnyTypeClass.class.getSimpleName() + " e ", AnyTypeClass.class);
        return query.getResultList();
    }

    @Override
    public AnyTypeClass save(final AnyTypeClass anyTypeClass) {
        AnyTypeClass merge = entityManager().merge(anyTypeClass);

        for (PlainSchema schema : merge.getPlainSchemas()) {
            schema.setAnyTypeClass(merge);
        }
        for (DerSchema schema : merge.getDerSchemas()) {
            schema.setAnyTypeClass(merge);
        }
        for (VirSchema schema : merge.getVirSchemas()) {
            schema.setAnyTypeClass(merge);
        }

        return merge;
    }

    @Override
    public void delete(final String key) {
        AnyTypeClass anyTypeClass = find(key);
        if (anyTypeClass == null) {
            return;
        }

        for (PlainSchema schema : plainSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }
        for (DerSchema schema : derSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }
        for (VirSchema schema : virSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }

        for (AnyType type : anyTypeDAO.findByTypeClass(anyTypeClass)) {
            type.getClasses().remove(anyTypeClass);
        }

        for (TypeExtension typeExt : groupDAO.findTypeExtensions(anyTypeClass)) {
            typeExt.getAuxClasses().remove(anyTypeClass);

            if (typeExt.getAuxClasses().isEmpty()) {
                typeExt.getGroup().getTypeExtensions().remove(typeExt);
                typeExt.setGroup(null);
            }
        }

        for (Provision provision : resourceDAO.findProvisionsByAuxClass(anyTypeClass)) {
            provision.getAuxClasses().remove(anyTypeClass);
        }

        entityManager().remove(anyTypeClass);
    }

}
