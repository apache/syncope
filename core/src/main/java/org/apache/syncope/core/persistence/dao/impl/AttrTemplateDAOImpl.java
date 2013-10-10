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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.dao.AttrTemplateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class AttrTemplateDAOImpl extends AbstractDAOImpl implements AttrTemplateDAO {

    @Override
    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> T find(
            final Long id, final Class<T> reference) {

        return entityManager.find(reference, id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> List<Number> findBySchemaName(
            final String schemaName, final Class<T> reference) {

        Query query = entityManager.createNativeQuery(
                "SELECT id FROM " + reference.getSimpleName() + " WHERE schema_name=?1");
        query.setParameter(1, schemaName);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> void delete(
            final Long id, final Class<T> reference) {

        T attrTemplate = find(id, reference);
        if (attrTemplate == null) {
            return;
        }

        delete(attrTemplate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> void delete(final T attrTemplate) {
        if (attrTemplate.getOwner() != null) {
            attrTemplate.getOwner().getAttrTemplates(attrTemplate.getClass()).remove(attrTemplate);
        }

        entityManager.remove(attrTemplate);
    }
}
