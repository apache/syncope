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
import org.apache.syncope.core.persistence.api.dao.PGAnyDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.group.PGJPAGroup;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.transaction.annotation.Transactional;

public class PGJPAGroupDAO extends JPAGroupDAO {

    private PGAnyDAO anyDAO;

    private PGAnyDAO anyDAO() {
        if (anyDAO == null) {
            anyDAO = ApplicationContextProvider.getApplicationContext().getBean(PGAnyDAO.class);
        }
        return anyDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public Group find(final String key) {
        return entityManager().find(PGJPAGroup.class, key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Group> findByPlainAttrValue(
            final String schemaKey,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByPlainAttrValue(PGJPAGroup.TABLE, anyUtils(), schemaKey, attrValue, ignoreCaseMatch);
    }

    @Override
    public Group findByPlainAttrUniqueValue(
            final String schemaKey,
            final PlainAttrValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByPlainAttrUniqueValue(PGJPAGroup.TABLE, anyUtils(),
                schemaKey, attrUniqueValue, ignoreCaseMatch);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Group> findByDerAttrValue(
            final String schemaKey,
            final String value,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByDerAttrValue(PGJPAGroup.TABLE, anyUtils(), schemaKey, value, ignoreCaseMatch);
    }

    @Override
    public Group save(final Group group) {
        anyDAO().checkBeforeSave(PGJPAGroup.TABLE, anyUtils(), group);
        return super.save(group);
    }
}
