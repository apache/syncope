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
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class JPAJSONGroupDAO extends JPAGroupDAO {

    private JPAJSONAnyDAO anyDAO;

    private JPAJSONAnyDAO anyDAO() {
        if (anyDAO == null) {
            anyDAO = ApplicationContextProvider.getApplicationContext().getBean(JPAJSONAnyDAO.class);
        }
        return anyDAO;
    }

    @Override
    public List<Group> findByPlainAttrValue(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByPlainAttrValue(
                JPAGroup.TABLE, anyUtils(), schema, attrValue, ignoreCaseMatch);
    }

    @Override
    public Optional<Group> findByPlainAttrUniqueValue(
            final PlainSchema schema,
            final PlainAttrUniqueValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByPlainAttrUniqueValue(
                JPAGroup.TABLE, anyUtils(), schema, attrUniqueValue, ignoreCaseMatch);
    }

    @Override
    public List<Group> findByDerAttrValue(
            final DerSchema schema,
            final String value,
            final boolean ignoreCaseMatch) {

        return anyDAO().findByDerAttrValue(JPAGroup.TABLE, anyUtils(), schema, value, ignoreCaseMatch);
    }

    @Override
    public Group save(final Group group) {
        anyDAO().checkBeforeSave(JPAGroup.TABLE, anyUtils(), group);
        return super.save(group);
    }
}
