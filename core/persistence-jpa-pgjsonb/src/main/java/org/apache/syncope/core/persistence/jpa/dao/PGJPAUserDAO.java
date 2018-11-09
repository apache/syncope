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
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.PGAnyDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.PGJPAUser;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.transaction.annotation.Transactional;

public class PGJPAUserDAO extends JPAUserDAO {

    private PGAnyDAO anyDAO;

    private PGAnyDAO anyDAO() {
        if (anyDAO == null) {
            anyDAO = ApplicationContextProvider.getApplicationContext().getBean(PGAnyDAO.class);
        }
        return anyDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public User find(final String key) {
        return entityManager().find(PGJPAUser.class, key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> findByPlainAttrValue(
            final String schemaKey,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByPlainAttrValue(PGJPAUser.TABLE, anyUtils(), schemaKey, attrValue, ignoreCaseMatch);
    }

    @Override
    public User findByPlainAttrUniqueValue(
            final String schemaKey,
            final PlainAttrValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByPlainAttrUniqueValue(PGJPAUser.TABLE, anyUtils(),
                schemaKey, attrUniqueValue, ignoreCaseMatch);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<User> findByDerAttrValue(
            final String schemaKey,
            final String value,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByDerAttrValue(PGJPAUser.TABLE, anyUtils(), schemaKey, value, ignoreCaseMatch);
    }

    @Override
    public User save(final User user) {
        anyDAO().checkBeforeSave(PGJPAUser.TABLE, anyUtils(), user);
        return super.save(user);
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final User user) {
        anyDAO().checkBeforeSave(PGJPAUser.TABLE, anyUtils(), user);
        return super.saveAndGetDynGroupMembs(user);
    }
}
