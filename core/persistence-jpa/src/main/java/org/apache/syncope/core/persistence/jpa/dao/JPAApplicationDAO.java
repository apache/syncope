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
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.jpa.entity.JPAApplication;
import org.apache.syncope.core.persistence.jpa.entity.JPAPrivilege;

public class JPAApplicationDAO extends AbstractDAO<Application> implements ApplicationDAO {

    protected final RoleDAO roleDAO;

    protected final UserDAO userDAO;

    public JPAApplicationDAO(final RoleDAO roleDAO, final UserDAO userDAO) {
        this.roleDAO = roleDAO;
        this.userDAO = userDAO;
    }

    @Override
    public Application find(final String key) {
        return entityManager().find(JPAApplication.class, key);
    }

    @Override
    public Privilege findPrivilege(final String key) {
        return entityManager().find(JPAPrivilege.class, key);
    }

    @Override
    public List<Application> findAll() {
        TypedQuery<Application> query = entityManager().createQuery(
                "SELECT e FROM " + JPAApplication.class.getSimpleName() + " e ", Application.class);
        return query.getResultList();
    }

    @Override
    public Application save(final Application application) {
        return entityManager().merge(application);
    }

    @Override
    public void delete(final Application application) {
        application.getPrivileges().forEach(privilege -> {
            roleDAO.findByPrivilege(privilege).
                    forEach(role -> role.getPrivileges().remove(privilege));
            userDAO.findLinkedAccountsByPrivilege(privilege).
                    forEach(account -> account.getPrivileges().remove(privilege));

            privilege.setApplication(null);
        });
        application.getPrivileges().clear();

        entityManager().remove(application);
    }

    @Override
    public void delete(final String key) {
        Application application = find(key);
        if (application == null) {
            return;
        }

        delete(application);
    }
}
