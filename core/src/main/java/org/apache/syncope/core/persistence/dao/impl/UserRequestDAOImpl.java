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
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.beans.UserRequest;
import org.apache.syncope.core.persistence.dao.UserRequestDAO;
import org.apache.syncope.validation.InvalidEntityException;

@Repository
@Transactional(noRollbackFor = { Throwable.class })
public class UserRequestDAOImpl extends AbstractDAOImpl implements UserRequestDAO {

    @Override
    @Transactional(readOnly = true)
    public UserRequest find(Long id) {
        return entityManager.find(UserRequest.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRequest> findAll() {
        Query query = entityManager.createQuery("SELECT e " + "FROM " + UserRequest.class.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public UserRequest save(UserRequest userRequest) throws InvalidEntityException {

        return entityManager.merge(userRequest);
    }

    @Override
    public void delete(Long id) {
        entityManager.remove(find(id));
    }
}
