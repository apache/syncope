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
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.springframework.transaction.annotation.Transactional;

public class JPAAnyTypeDAO extends AbstractDAO<AnyType> implements AnyTypeDAO {

    protected final RemediationDAO remediationDAO;

    public JPAAnyTypeDAO(final RemediationDAO remediationDAO) {
        this.remediationDAO = remediationDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType find(final String key) {
        return entityManager().find(JPAAnyType.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType findUser() {
        return find(AnyTypeKind.USER.name());
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType findGroup() {
        return find(AnyTypeKind.GROUP.name());
    }

    @Override
    public List<AnyType> findByTypeClass(final AnyTypeClass anyTypeClass) {
        TypedQuery<AnyType> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAnyType.class.getSimpleName() + " e WHERE :anyTypeClass MEMBER OF e.classes",
                AnyType.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Override
    public List<AnyType> findAll() {
        TypedQuery<AnyType> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAnyType.class.getSimpleName() + " e ", AnyType.class);
        return query.getResultList();
    }

    @Override
    public AnyType save(final AnyType anyType) {
        return entityManager().merge(anyType);
    }

    @Override
    public void delete(final String key) {
        AnyType anyType = find(key);
        if (anyType == null) {
            return;
        }

        if (anyType.equals(findUser()) || anyType.equals(findGroup())) {
            throw new IllegalArgumentException(key + " cannot be deleted");
        }

        remediationDAO.findByAnyType(anyType).forEach(remediation -> {
            remediation.setAnyType(null);
            remediationDAO.delete(remediation);
        });

        entityManager().remove(anyType);
    }

}
