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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jApplication;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPrivilege;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class ApplicationRepoExtImpl implements ApplicationRepoExt {

    protected final RoleDAO roleDAO;

    protected final UserDAO userDAO;

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public ApplicationRepoExtImpl(
            final RoleDAO roleDAO,
            final UserDAO userDAO,
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        this.roleDAO = roleDAO;
        this.userDAO = userDAO;
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public Optional<? extends Privilege> findPrivilege(final String key) {
        return neo4jTemplate.findById(key, Neo4jPrivilege.class);
    }

    @Override
    public Application save(final Application application) {
        // delete all privileges formerly associated to the application being saved
        neo4jTemplate.findById(application.getKey(), Neo4jApplication.class).ifPresent(before -> {
            Set<String> bp = before.getPrivileges().stream().map(Privilege::getKey).collect(Collectors.toSet());
            Set<String> ap = application.getPrivileges().stream().map(Privilege::getKey).collect(Collectors.toSet());
            bp.removeAll(ap);
            bp.forEach(k -> neo4jTemplate.deleteById(k, Neo4jPrivilege.class));
        });
        return neo4jTemplate.save(nodeValidator.validate(application));
    }

    @Override
    public void delete(final Application application) {
        application.getPrivileges().forEach(privilege -> {
            roleDAO.findByPrivileges(privilege).
                    forEach(role -> role.getPrivileges().remove(privilege));
            userDAO.findLinkedAccountsByPrivilege(privilege).
                    forEach(account -> account.getPrivileges().remove(privilege));

            privilege.setApplication(null);
            neo4jTemplate.deleteById(privilege.getKey(), Neo4jPrivilege.class);
        });
        application.getPrivileges().clear();

        neo4jTemplate.deleteById(application.getKey(), Neo4jApplication.class);
    }
}
