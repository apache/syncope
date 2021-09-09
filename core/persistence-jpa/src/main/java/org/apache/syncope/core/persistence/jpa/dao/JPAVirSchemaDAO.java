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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.JPAVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPullPolicy;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAMapping;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;

public class JPAVirSchemaDAO extends AbstractDAO<VirSchema> implements VirSchemaDAO {

    protected final ExternalResourceDAO resourceDAO;

    public JPAVirSchemaDAO(final ExternalResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    @Override
    public VirSchema find(final String key) {
        return entityManager().find(JPAVirSchema.class, key);
    }

    @Override
    public List<VirSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAVirSchema.class.getSimpleName()).
                append(" e WHERE ");
        anyTypeClasses.forEach(anyTypeClass -> queryString.
                append("e.anyTypeClass.id='").
                append(anyTypeClass.getKey()).append("' OR "));

        TypedQuery<VirSchema> query = entityManager().createQuery(
                queryString.substring(0, queryString.length() - 4), VirSchema.class);

        return query.getResultList();
    }

    @Override
    public List<VirSchema> findByProvision(final Provision provision) {
        Query query = entityManager().createNativeQuery(
                "SELECT t0.id FROM VirSchema t0 "
                + "LEFT OUTER JOIN " + JPAAnyTypeClass.TABLE + " t1 ON t0.ANYTYPECLASS_ID = t1.id "
                + "LEFT OUTER JOIN " + JPAProvision.TABLE + " t2 ON t0.PROVISION_ID = t2.id "
                + "LEFT OUTER JOIN " + JPAAnyType.TABLE + " t3 ON t2.ANYTYPE_ID = t3.id "
                + "LEFT OUTER JOIN " + JPAMapping.TABLE + " t4 ON t2.id = t4.PROVISION_ID "
                + "LEFT OUTER JOIN " + JPAExternalResource.TABLE + " t5 ON t2.RESOURCE_ID = t5.id "
                + "LEFT OUTER JOIN " + JPAAccountPolicy.TABLE + " t6 ON t5.ACCOUNTPOLICY_ID = t6.id "
                + "LEFT OUTER JOIN " + JPAConnInstance.TABLE + " t7 ON t5.CONNECTOR_ID = t7.id "
                + "LEFT OUTER JOIN " + JPAPasswordPolicy.TABLE + " t8 ON t5.PASSWORDPOLICY_ID = t8.id "
                + "LEFT OUTER JOIN " + JPAPullPolicy.TABLE + " t9 ON t5.PULLPOLICY_ID = t9.id "
                + "WHERE t0.PROVISION_ID = ?1");
        query.setParameter(1, provision.getKey());

        List<VirSchema> result = new ArrayList<>();
        for (Object key : query.getResultList()) {
            String actualKey = key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key);

            VirSchema virSchema = find(actualKey);
            if (virSchema == null) {
                LOG.error("Could not find schema with id {}, even though returned by the native query", actualKey);
            } else if (!result.contains(virSchema)) {
                result.add(virSchema);
            }
        }
        return result;
    }

    @Override
    public List<VirSchema> findByKeyword(final String keyword) {
        TypedQuery<VirSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAVirSchema.class.getSimpleName() + " e"
                + " WHERE e.id LIKE :keyword", VirSchema.class);
        query.setParameter("keyword", keyword);
        return query.getResultList();
    }

    @Override
    public List<VirSchema> findAll() {
        TypedQuery<VirSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAVirSchema.class.getSimpleName() + " e", VirSchema.class);
        return query.getResultList();
    }

    @Override
    public VirSchema save(final VirSchema virSchema) {
        return entityManager().merge(virSchema);
    }

    @Override
    public void delete(final String key) {
        VirSchema schema = find(key);
        if (schema == null) {
            return;
        }

        schema.getLabels().forEach(label -> label.setSchema(null));

        resourceDAO.deleteMapping(key);

        if (schema.getAnyTypeClass() != null) {
            schema.getAnyTypeClass().getVirSchemas().remove(schema);
        }

        entityManager().remove(schema);
    }
}
