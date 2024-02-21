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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jSecurityQuestion;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class SecurityQuestionRepoExtImpl implements SecurityQuestionRepoExt {

    protected final UserDAO userDAO;

    protected final Neo4jTemplate neo4jTemplate;

    public SecurityQuestionRepoExtImpl(final UserDAO userDAO, final Neo4jTemplate neo4jTemplate) {
        this.userDAO = userDAO;
        this.neo4jTemplate = neo4jTemplate;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jSecurityQuestion.class).ifPresent(securityQuestion -> {

            userDAO.findBySecurityQuestion(securityQuestion).forEach(user -> {
                user.setSecurityQuestion(null);
                user.setSecurityAnswer(null);
                userDAO.save(user);
            });

            neo4jTemplate.deleteById(key, Neo4jSecurityQuestion.class);
        });
    }
}
