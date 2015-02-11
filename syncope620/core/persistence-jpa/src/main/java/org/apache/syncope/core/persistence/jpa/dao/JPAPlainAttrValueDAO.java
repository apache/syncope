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
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPlainAttrValueDAO extends AbstractDAO<PlainAttrValue, Long> implements PlainAttrValueDAO {

    private <T extends PlainAttrValue> Class<? extends AbstractPlainAttrValue> getJPAEntityReference(
            final Class<T> reference) {

        return reference.equals(CPlainAttrValue.class)
                ? JPACPlainAttrValue.class
                : reference.equals(CPlainAttrUniqueValue.class)
                        ? JPACPlainAttrUniqueValue.class
                        : reference.equals(RPlainAttrValue.class)
                                ? JPARPlainAttrValue.class
                                : reference.equals(RPlainAttrUniqueValue.class)
                                        ? JPARPlainAttrUniqueValue.class
                                        : reference.equals(MPlainAttrValue.class)
                                                ? JPAMPlainAttrValue.class
                                                : reference.equals(MPlainAttrUniqueValue.class)
                                                        ? JPAMPlainAttrUniqueValue.class
                                                        : reference.equals(UPlainAttrValue.class)
                                                                ? JPAUPlainAttrValue.class
                                                                : reference.equals(UPlainAttrUniqueValue.class)
                                                                        ? JPAUPlainAttrUniqueValue.class
                                                                        : null;
    }

    @Override
    public <T extends PlainAttrValue> T find(final Long key, final Class<T> reference) {
        return reference.cast(entityManager.find(getJPAEntityReference(reference), key));
    }

    @Override
    public <T extends PlainAttrValue> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + getJPAEntityReference(reference).getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends PlainAttrValue> T save(final T attributeValue) {
        return entityManager.merge(attributeValue);
    }

    @Override
    public <T extends PlainAttrValue> void delete(final Long id, final Class<T> reference) {
        T attributeValue = find(id, reference);
        if (attributeValue == null) {
            return;
        }

        delete(attributeValue);
    }

    @Override
    public <T extends PlainAttrValue> void delete(final T attrValue) {
        if (attrValue.getAttr() != null) {
            attrValue.getAttr().removeValue(attrValue);
        }

        entityManager.remove(attrValue);
    }
}
