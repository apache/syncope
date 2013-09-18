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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.beans.UserRequest;
import org.apache.syncope.core.persistence.dao.UserRequestDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(noRollbackFor = {Throwable.class})
public class UserRequestDAOImpl extends AbstractDAOImpl implements UserRequestDAO {

    @Override
    @Transactional(readOnly = true)
    public UserRequest find(Long id) {
        return entityManager.find(UserRequest.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRequest> findAll() {
        TypedQuery<UserRequest> query = entityManager.createQuery(
                "SELECT e " + "FROM " + UserRequest.class.getSimpleName() + " e", UserRequest.class);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRequest> findAll(final boolean executed) {
        TypedQuery<UserRequest> query = entityManager.createQuery(
                "SELECT e " + "FROM " + UserRequest.class.getSimpleName() + " e WHERE e.executed = :executed",
                UserRequest.class);
        query.setParameter("executed", executed ? 1 : 0);

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRequest> findAll(final String username) {
        TypedQuery<UserRequest> query = entityManager.createQuery(
                "SELECT e " + "FROM " + UserRequest.class.getSimpleName() + " e WHERE e.username = :username",
                UserRequest.class);
        query.setParameter("username", username);

        return query.getResultList();
    }

    @Override
    public UserRequest save(final UserRequest userRequest) throws InvalidEntityException {
        return entityManager.merge(userRequest);
    }

    @Override
    public void delete(final Long id) {
        entityManager.remove(find(id));
    }
}
