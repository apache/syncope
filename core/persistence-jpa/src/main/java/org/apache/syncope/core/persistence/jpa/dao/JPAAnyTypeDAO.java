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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAAnyTypeDAO extends AbstractDAO<AnyType, String> implements AnyTypeDAO {

    @Override
    public AnyType find(final String key) {
        return entityManager.find(JPAAnyType.class, key);
    }

    private AnyType find(final AnyTypeKind typeKind) {
        AnyType anyType = find(typeKind.name());
        if (anyType == null) {
            anyType = new JPAAnyType();
            anyType.setKey(typeKind.name());
            anyType.setKind(typeKind);
            anyType = save(anyType);
        }
        return anyType;
    }

    @Transactional(readOnly = false)
    @Override
    public AnyType findUser() {
        return find(AnyTypeKind.USER);
    }

    @Transactional(readOnly = false)
    @Override
    public AnyType findGroup() {
        return find(AnyTypeKind.GROUP);
    }

    @Override
    public List<AnyType> findAll() {
        TypedQuery<AnyType> query = entityManager.createQuery(
                "SELECT e FROM " + JPAAnyType.class.getSimpleName() + " e ", AnyType.class);
        return query.getResultList();
    }

    @Override
    public AnyType save(final AnyType anyType) {
        return entityManager.merge(anyType);
    }

    @Override
    public void delete(final String key) {
        AnyType anyType = find(key);
        if (anyType == null) {
            return;
        }

        entityManager.remove(anyType);
    }

}
