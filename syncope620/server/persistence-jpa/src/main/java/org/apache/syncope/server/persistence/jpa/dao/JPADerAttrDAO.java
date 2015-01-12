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

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.server.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.server.persistence.api.entity.Attributable;
import org.apache.syncope.server.persistence.api.entity.DerAttr;
import org.apache.syncope.server.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.server.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.server.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.server.persistence.jpa.entity.AbstractDerAttr;
import org.apache.syncope.server.persistence.jpa.entity.membership.JPAMDerAttr;
import org.apache.syncope.server.persistence.jpa.entity.role.JPARDerAttr;
import org.apache.syncope.server.persistence.jpa.entity.user.JPAUDerAttr;
import org.springframework.stereotype.Repository;

@Repository
public class JPADerAttrDAO extends AbstractDAO<DerAttr, Long> implements DerAttrDAO {

    public <T extends DerAttr> Class<? extends AbstractDerAttr> getJPAEntityReference(
            final Class<T> reference) {

        return RDerAttr.class.isAssignableFrom(reference)
                ? JPARDerAttr.class
                : MDerAttr.class.isAssignableFrom(reference)
                        ? JPAMDerAttr.class
                        : UDerAttr.class.isAssignableFrom(reference)
                                ? JPAUDerAttr.class
                                : null;
    }

    @Override
    public <T extends DerAttr> T find(final Long key, final Class<T> reference) {
        return reference.cast(entityManager.find(getJPAEntityReference(reference), key));
    }

    @Override
    public <T extends DerAttr> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + getJPAEntityReference(reference).getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends DerAttr> T save(final T derAttr) {
        return entityManager.merge(derAttr);
    }

    @Override
    public <T extends DerAttr> void delete(final Long key, final Class<T> reference) {
        T derAttr = find(key, reference);
        if (derAttr == null) {
            return;
        }

        delete(derAttr);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DerAttr> void delete(final T derAttr) {
        if (derAttr.getOwner() != null) {
            ((Attributable<?, T, ?>) derAttr.getOwner()).removeDerAttr(derAttr);
        }

        entityManager.remove(derAttr);
    }
}
