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
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.AbstractVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUVirAttr;
import org.springframework.stereotype.Repository;

@Repository
public class JPAVirAttrDAO extends AbstractDAO<VirAttr<?>, Long> implements VirAttrDAO {

    public <T extends VirAttr<?>> Class<? extends AbstractVirAttr<?>> getJPAEntityReference(
            final Class<T> reference) {

        return GVirAttr.class.isAssignableFrom(reference)
                ? JPAGVirAttr.class
                : AVirAttr.class.isAssignableFrom(reference)
                        ? JPAAVirAttr.class
                        : UVirAttr.class.isAssignableFrom(reference)
                                ? JPAUVirAttr.class
                                : null;
    }

    @Override
    public <T extends VirAttr<?>> T find(final Long key, final Class<T> reference) {
        return reference.cast(entityManager().find(getJPAEntityReference(reference), key));
    }

    @Override
    public <T extends VirAttr<?>> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager().createQuery(
                "SELECT e FROM " + getJPAEntityReference(reference).getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends VirAttr<?>> T save(final T virAttr) {
        return entityManager().merge(virAttr);
    }

    @Override
    public <T extends VirAttr<?>> void delete(final Long key, final Class<T> reference) {
        T virAttr = find(key, reference);
        if (virAttr == null) {
            return;
        }

        delete(virAttr);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends VirAttr<?>> void delete(final T virAttr) {
        if (virAttr.getOwner() != null) {
            ((Any<?, ?, T>) virAttr.getOwner()).remove(virAttr);
        }

        entityManager().remove(virAttr);
    }
}
