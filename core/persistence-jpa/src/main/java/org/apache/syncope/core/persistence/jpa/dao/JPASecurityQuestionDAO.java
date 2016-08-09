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
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPASecurityQuestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPASecurityQuestionDAO extends AbstractDAO<SecurityQuestion> implements SecurityQuestionDAO {

    @Autowired
    private UserDAO userDAO;

    @Override
    public SecurityQuestion find(final String key) {
        return entityManager().find(JPASecurityQuestion.class, key);
    }

    @Override
    public List<SecurityQuestion> findAll() {
        final TypedQuery<SecurityQuestion> query = entityManager().createQuery(
                "SELECT e FROM " + JPASecurityQuestion.class.getSimpleName() + " e ", SecurityQuestion.class);
        return query.getResultList();
    }

    @Override
    public SecurityQuestion save(final SecurityQuestion securityQuestion) {
        return entityManager().merge(securityQuestion);
    }

    @Override
    public void delete(final String key) {
        SecurityQuestion securityQuestion = find(key);
        if (securityQuestion == null) {
            return;
        }

        for (User user : userDAO.findBySecurityQuestion(securityQuestion)) {
            user.setSecurityQuestion(null);
            user.setSecurityAnswer(null);
            userDAO.save(user);
        }

        entityManager().remove(securityQuestion);
    }

}
