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
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPlainAttrValueDAO extends AbstractDAO<PlainAttrValue> implements PlainAttrValueDAO {

    @SuppressWarnings("unchecked")
    private <T extends PlainAttrValue> Class<? extends AbstractPlainAttrValue> getEntityReference(
            final Class<T> reference) {

        return AbstractPlainAttrValue.class.isAssignableFrom(reference)
                ? (Class<? extends AbstractPlainAttrValue>) reference
                : reference.equals(CPlainAttrValue.class)
                ? JPACPlainAttrValue.class
                : reference.equals(CPlainAttrUniqueValue.class)
                ? JPACPlainAttrUniqueValue.class
                : reference.equals(GPlainAttrValue.class)
                ? JPAGPlainAttrValue.class
                : reference.equals(GPlainAttrUniqueValue.class)
                ? JPAGPlainAttrUniqueValue.class
                : reference.equals(APlainAttrValue.class)
                ? JPAAPlainAttrValue.class
                : reference.equals(APlainAttrUniqueValue.class)
                ? JPAAPlainAttrUniqueValue.class
                : reference.equals(UPlainAttrValue.class)
                ? JPAUPlainAttrValue.class
                : reference.equals(UPlainAttrUniqueValue.class)
                ? JPAUPlainAttrUniqueValue.class
                : null;
    }

    @Override
    public <T extends PlainAttrValue> T find(final String key, final Class<T> reference) {
        return reference.cast(entityManager().find(getEntityReference(reference), key));
    }

    @Override
    public <T extends PlainAttrValue> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager().createQuery(
                "SELECT e FROM " + getEntityReference(reference).getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends PlainAttrValue> T save(final T attributeValue) {
        return entityManager().merge(attributeValue);
    }

    @Override
    public <T extends PlainAttrValue> void delete(final String key, final Class<T> reference) {
        T attributeValue = find(key, reference);
        if (attributeValue == null) {
            return;
        }

        delete(attributeValue);
    }

    @Override
    public <T extends PlainAttrValue> void delete(final T attrValue) {
        if (attrValue.getAttr() != null) {
            if (attrValue instanceof PlainAttrUniqueValue) {
                attrValue.getAttr().setUniqueValue(null);
            } else {
                attrValue.getAttr().getValues().remove(attrValue);
            }
        }

        entityManager().remove(attrValue);
    }
}
