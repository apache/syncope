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

import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;

public class JPAPlainAttrValueDAO extends AbstractDAO<PlainAttrValue> implements PlainAttrValueDAO {

    @SuppressWarnings("unchecked")
    public static <T extends PlainAttrValue> Class<? extends AbstractPlainAttrValue> getEntityReference(
            final Class<T> reference) {

        return AbstractPlainAttrValue.class.isAssignableFrom(reference)
                ? (Class<? extends AbstractPlainAttrValue>) reference
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
    public void deleteAll(final PlainAttr<?> attr, final AnyUtils anyUtils) {
        if (attr.getUniqueValue() == null) {
            attr.getValues().stream().map(Entity::getKey).collect(Collectors.toSet()).forEach(attrValueKey -> {
                PlainAttrValue attrValue = anyUtils.plainAttrValueClass().cast(
                        entityManager().find(getEntityReference(anyUtils.plainAttrValueClass()), attrValueKey));
                if (attrValue != null) {
                    entityManager().remove(attrValue);
                    attr.getValues().remove(attrValue);
                }
            });
        } else {
            entityManager().remove(attr.getUniqueValue());
            attr.setUniqueValue(null);
        }
    }
}
