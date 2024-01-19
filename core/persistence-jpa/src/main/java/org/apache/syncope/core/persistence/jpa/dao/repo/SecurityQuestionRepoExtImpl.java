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
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.jpa.entity.user.JPASecurityQuestion;

public class SecurityQuestionRepoExtImpl implements SecurityQuestionRepoExt {

    protected final UserDAO userDAO;

    protected final EntityManager entityManager;

    public SecurityQuestionRepoExtImpl(final UserDAO userDAO, final EntityManager entityManager) {
        this.userDAO = userDAO;
        this.entityManager = entityManager;
    }

    @Override
    public void deleteById(final String key) {
        SecurityQuestion securityQuestion = entityManager.find(JPASecurityQuestion.class, key);
        if (securityQuestion == null) {
            return;
        }

        userDAO.findBySecurityQuestion(securityQuestion).forEach(user -> {
            user.setSecurityQuestion(null);
            user.setSecurityAnswer(null);
            userDAO.save(user);
        });

        entityManager.remove(securityQuestion);
    }
}
