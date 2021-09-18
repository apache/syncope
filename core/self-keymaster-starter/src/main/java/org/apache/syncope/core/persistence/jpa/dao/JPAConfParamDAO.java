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
import org.apache.syncope.core.persistence.api.dao.ConfParamDAO;
import org.apache.syncope.core.persistence.api.entity.ConfParam;
import org.apache.syncope.core.persistence.jpa.entity.JPAConfParam;
import org.springframework.transaction.annotation.Transactional;

public class JPAConfParamDAO extends AbstractDAO<ConfParam> implements ConfParamDAO {

    @Transactional(readOnly = true)
    @Override
    public List<ConfParam> findAll() {
        TypedQuery<ConfParam> query = entityManager().createQuery(
                "SELECT e FROM " + JPAConfParam.class.getSimpleName() + " e ORDER BY e.id ASC", ConfParam.class);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public ConfParam find(final String key) {
        return entityManager().find(JPAConfParam.class, key);
    }

    @Override
    public ConfParam save(final ConfParam confParam) {
        return entityManager().merge(confParam);
    }

    @Override
    public void delete(final String key) {
        ConfParam param = find(key);
        if (param != null) {
            entityManager().remove(param);
        }
    }
}
