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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.springframework.transaction.annotation.Transactional;

public class AnyTypeRepoExtImpl implements AnyTypeRepoExt {

    protected final RemediationDAO remediationDAO;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final EntityManager entityManager;

    public AnyTypeRepoExtImpl(
            final RemediationDAO remediationDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final EntityManager entityManager) {

        this.remediationDAO = remediationDAO;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType getUser() {
        return entityManager.find(JPAAnyType.class, AnyTypeKind.USER.name());
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType getGroup() {
        return entityManager.find(JPAAnyType.class, AnyTypeKind.GROUP.name());
    }

    @Override
    public void deleteById(final String key) {
        AnyType anyType = entityManager.find(JPAAnyType.class, key);
        if (anyType == null) {
            return;
        }

        if (anyType.equals(getUser()) || anyType.equals(getGroup())) {
            throw new IllegalArgumentException(key + " cannot be deleted");
        }

        remediationDAO.findByAnyType(anyType).forEach(remediation -> {
            remediation.setAnyType(null);
            remediationDAO.delete(remediation);
        });

        relationshipTypeDAO.findByEndAnyType(anyType).forEach(relationshipTypeDAO::deleteById);

        entityManager.remove(anyType);
    }
}
