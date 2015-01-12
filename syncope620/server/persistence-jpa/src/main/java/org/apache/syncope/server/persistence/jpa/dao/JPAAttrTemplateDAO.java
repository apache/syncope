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
package org.apache.syncope.server.persistence.jpa.dao;

import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.apache.syncope.server.persistence.api.dao.AttrTemplateDAO;
import org.apache.syncope.server.persistence.api.entity.AttrTemplate;
import org.apache.syncope.server.persistence.api.entity.Schema;
import org.apache.syncope.server.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RDerAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RPlainAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.AbstractAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.membership.JPAMPlainAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.membership.JPAMVirAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.role.JPARDerAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.role.JPARPlainAttrTemplate;
import org.apache.syncope.server.persistence.jpa.entity.role.JPARVirAttrTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

@Repository
public class JPAAttrTemplateDAO<S extends Schema>
        extends AbstractDAO<AttrTemplate<S>, Long> implements AttrTemplateDAO<S> {

    private <T extends AttrTemplate<S>> Class<? extends AbstractAttrTemplate<? extends Schema>> getJPAEntityReference(
            final Class<T> reference) {

        return MPlainAttrTemplate.class.isAssignableFrom(reference)
                ? JPAMPlainAttrTemplate.class
                : MDerAttrTemplate.class.isAssignableFrom(reference)
                        ? JPAMDerAttrTemplate.class
                        : MVirAttrTemplate.class.isAssignableFrom(reference)
                                ? JPAMVirAttrTemplate.class
                                : RPlainAttrTemplate.class.isAssignableFrom(reference)
                                        ? JPARPlainAttrTemplate.class
                                        : RDerAttrTemplate.class.isAssignableFrom(reference)
                                                ? JPARDerAttrTemplate.class
                                                : RVirAttrTemplate.class.isAssignableFrom(reference)
                                                        ? JPARVirAttrTemplate.class
                                                        : null;
    }

    @Override
    public <T extends AttrTemplate<S>> T find(final Long key, final Class<T> reference) {
        return reference.cast(entityManager.find(getJPAEntityReference(reference), key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AttrTemplate<S>> List<Number> findBySchemaName(
            final String schemaName, final Class<T> reference) {

        Query query = null;
        try {
            query = entityManager.createNativeQuery("SELECT id FROM "
                    + ReflectionUtils.findField(getJPAEntityReference(reference), "TABLE").get(null).toString()
                    + " WHERE schema_name=?1");
            query.setParameter(1, schemaName);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }

        return query == null ? Collections.<Number>emptyList() : query.getResultList();
    }

    @Override
    public <T extends AttrTemplate<S>> void delete(final Long key, final Class<T> reference) {
        T attrTemplate = find(key, reference);
        if (attrTemplate == null) {
            return;
        }

        delete(attrTemplate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AttrTemplate<S>> void delete(final T attrTemplate) {
        if (attrTemplate.getOwner() != null) {
            attrTemplate.getOwner().getAttrTemplates(attrTemplate.getClass()).remove(attrTemplate);
        }

        entityManager.remove(attrTemplate);
    }
}
