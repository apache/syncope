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

import static org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrValueDAO.getEntityReference;

import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;

public class PGJPAPlainAttrValueDAO extends AbstractDAO<PlainAttrValue> implements PlainAttrValueDAO {

    @Override
    public void deleteAll(final PlainAttr<?> attr, final AnyUtils anyUtils) {
        if (attr.getUniqueValue() == null) {
            attr.getValues().stream().map(Entity::getKey).collect(Collectors.toSet()).forEach(attrValueKey -> {
                PlainAttrValue attrValue = anyUtils.plainAttrValueClass().cast(
                        entityManager().find(getEntityReference(anyUtils.plainAttrValueClass()), attrValueKey));
                if (attrValue != null) {
                    attr.getValues().remove(attrValue);
                }
            });
        } else {
            attr.setUniqueValue(null);
        }
    }
}
