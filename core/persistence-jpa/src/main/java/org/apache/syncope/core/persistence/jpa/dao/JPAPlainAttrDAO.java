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

import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;

public class JPAPlainAttrDAO extends AbstractDAO<PlainAttr<?>> implements PlainAttrDAO {

    public static <T extends PlainAttr<?>> Class<? extends AbstractPlainAttr<?>> getEntityReference(
            final Class<T> reference) {

        return GPlainAttr.class.isAssignableFrom(reference)
                ? JPAGPlainAttr.class
                : APlainAttr.class.isAssignableFrom(reference)
                ? JPAAPlainAttr.class
                : UPlainAttr.class.isAssignableFrom(reference)
                ? JPAUPlainAttr.class
                : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlainAttr<?>> void delete(final T plainAttr) {
        if (plainAttr.getOwner() != null) {
            ((Attributable<T>) plainAttr.getOwner()).remove(plainAttr);
        }

        entityManager().remove(plainAttr);
    }
}
