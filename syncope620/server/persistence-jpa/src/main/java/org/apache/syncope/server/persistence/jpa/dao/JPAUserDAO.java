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
package org.apache.syncope.server.persistence.jpa.dao;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.server.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.server.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.server.persistence.api.dao.search.SearchCond;
import org.apache.syncope.server.persistence.api.dao.search.SubjectCond;
import org.apache.syncope.server.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.server.persistence.api.entity.ExternalResource;
import org.apache.syncope.server.persistence.api.entity.Subject;
import org.apache.syncope.server.persistence.api.entity.VirAttr;
import org.apache.syncope.server.persistence.api.entity.membership.Membership;
import org.apache.syncope.server.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.server.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.server.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.server.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.server.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.server.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.server.misc.security.AuthContextUtil;
import org.apache.syncope.server.misc.security.UnauthorizedRoleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAUserDAO extends AbstractSubjectDAO<UPlainAttr, UDerAttr, UVirAttr> implements UserDAO {

    @Autowired
    private SubjectSearchDAO searchDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

    @Override
    protected Subject<UPlainAttr, UDerAttr, UVirAttr> findInternal(Long key) {
        return find(key);
    }

    @Override
    public User find(final Long key) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE e.id = :id", User.class);
        query.setParameter("id", key);

        User result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with id {}", key, e);
        }

        return result;
    }

    @Override
    public User find(final String username) {
        TypedQuery<User> query = entityManager.createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
                + " e WHERE e.username = :username", User.class);
        query.setParameter("username", username);

        User result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with username {}", username, e);
        }

        return result;
    }

    @Override
    public User findByWorkflowId(final String workflowId) {
        TypedQuery<User> query = entityManager.createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
                + " e WHERE e.workflowId = :workflowId", User.class);
        query.setParameter("workflowId", workflowId);

        User result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with workflow id {}", workflowId, e);
        }

        return result;
    }

    @Override
    public User findByToken(final String token) {
        TypedQuery<User> query = entityManager.createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
                + " e WHERE e.token = :token", User.class);
        query.setParameter("token", token);

        User result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with token {}", token, e);
        }

        return result;
    }

    @Override
    public List<User> findBySecurityQuestion(final SecurityQuestion securityQuestion) {
        TypedQuery<User> query = entityManager.createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
                + " e WHERE e.securityQuestion = :securityQuestion", User.class);
        query.setParameter("securityQuestion", securityQuestion);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findByAttrValue(final String schemaName, final UPlainAttrValue attrValue) {
        return (List<User>) findByAttrValue(
                schemaName, attrValue, attrUtilFactory.getInstance(AttributableType.USER));
    }

    @SuppressWarnings("unchecked")
    @Override
    public User findByAttrUniqueValue(final String schemaName, final UPlainAttrValue attrUniqueValue) {
        return (User) findByAttrUniqueValue(schemaName, attrUniqueValue,
                attrUtilFactory.getInstance(AttributableType.USER));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findByDerAttrValue(final String schemaName, final String value) {
        return (List<User>) findByDerAttrValue(
                schemaName, value, attrUtilFactory.getInstance(AttributableType.USER));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findByResource(final ExternalResource resource) {
        return (List<User>) findByResource(resource, attrUtilFactory.getInstance(AttributableType.USER));
    }

    @Override
    public final List<User> findAll(final Set<Long> adminRoles, final int page, final int itemsPerPage) {
        return findAll(adminRoles, page, itemsPerPage, Collections.<OrderByClause>emptyList());
    }

    private SearchCond getAllMatchingCond() {
        SubjectCond idCond = new SubjectCond(AttributeCond.Type.ISNOTNULL);
        idCond.setSchema("id");
        return SearchCond.getLeafCond(idCond);
    }

    @Override
    public List<User> findAll(final Set<Long> adminRoles,
            final int page, final int itemsPerPage, final List<OrderByClause> orderBy) {

        return searchDAO.search(
                adminRoles, getAllMatchingCond(), page, itemsPerPage, orderBy, SubjectType.USER);
    }

    @Override
    public final int count(final Set<Long> adminRoles) {
        return searchDAO.count(adminRoles, getAllMatchingCond(), SubjectType.USER);
    }

    @Override
    public User save(final User user) {
        final User merged = entityManager.merge(user);
        for (VirAttr virAttr : merged.getVirAttrs()) {
            virAttr.getValues().clear();
            virAttr.getValues().addAll(user.getVirAttr(virAttr.getSchema().getKey()).getValues());
        }

        return merged;
    }

    @Override
    public void delete(final Long key) {
        User user = (User) findInternal(key);
        if (user == null) {
            return;
        }

        delete(user);
    }

    @Override
    public void delete(final User user) {
        // Not calling membershipDAO.delete() here because it would try to save this user as well, thus going into
        // ConcurrentModificationException
        for (Membership membership : user.getMemberships()) {
            membership.setUser(null);

            roleDAO.save(membership.getRole());
            membership.setRole(null);

            entityManager.remove(membership);
        }
        user.getMemberships().clear();

        entityManager.remove(user);
    }

    private void securityChecks(final User user) {
        // Allows anonymous (during self-registration) and self (during self-update) to read own SyncopeUser,
        // otherwise goes thorugh security checks to see if needed role entitlements are owned
        if (!AuthContextUtil.getAuthenticatedUsername().equals(anonymousUser)
                && !AuthContextUtil.getAuthenticatedUsername().equals(user.getUsername())) {

            Set<Long> roleIds = user.getRoleIds();
            Set<Long> adminRoleIds = RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames());
            roleIds.removeAll(adminRoleIds);
            if (!roleIds.isEmpty()) {
                throw new UnauthorizedRoleException(roleIds);
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public User authFetch(final Long key) {
        if (key == null) {
            throw new NotFoundException("Null user id");
        }

        User user = find(key);
        if (user == null) {
            throw new NotFoundException("User " + key);
        }

        securityChecks(user);

        return user;
    }

    @Transactional(readOnly = true)
    @Override
    public User authFetch(final String username) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        User user = find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        securityChecks(user);

        return user;
    }

}
