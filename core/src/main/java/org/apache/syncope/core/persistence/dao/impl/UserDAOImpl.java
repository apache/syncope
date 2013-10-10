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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAOImpl extends AbstractAttributableDAOImpl implements UserDAO {

    @Autowired
    private RoleDAO roleDAO;

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends AbstractAttributable> T findInternal(final Long id) {
        return (T) find(id);
    }

    @Override
    public SyncopeUser find(final Long id) {
        TypedQuery<SyncopeUser> query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeUser.class.getSimpleName() + " e WHERE e.id = :id", SyncopeUser.class);
        query.setParameter("id", id);

        SyncopeUser result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with id {}", id, e);
        }

        return result;
    }

    @Override
    public SyncopeUser find(final String username) {
        TypedQuery<SyncopeUser> query = entityManager.createQuery("SELECT e FROM " + SyncopeUser.class.getSimpleName()
                + " e " + "WHERE e.username = :username", SyncopeUser.class);
        query.setParameter("username", username);

        SyncopeUser result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with username {}", username, e);
        }

        return result;
    }

    @Override
    public SyncopeUser findByWorkflowId(final String workflowId) {
        TypedQuery<SyncopeUser> query = entityManager.createQuery("SELECT e FROM " + SyncopeUser.class.getSimpleName()
                + " e " + "WHERE e.workflowId = :workflowId", SyncopeUser.class);
        query.setParameter("workflowId", workflowId);

        SyncopeUser result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with workflow id {}", workflowId, e);
        }

        return result;
    }

    @Override
    protected TypedQuery<AbstractAttrValue> findByAttrValueQuery(final String entityName) {
        return entityManager.createQuery("SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.name = :schemaName AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL AND e.doubleValue = :doubleValue)",
                AbstractAttrValue.class);
    }

    @Override
    public List<SyncopeUser> findByAttrValue(final String schemaName, final UAttrValue attrValue) {
        return findByAttrValue(schemaName, attrValue, AttributableUtil.getInstance(AttributableType.USER));
    }

    @Override
    public SyncopeUser findByAttrUniqueValue(final String schemaName, final UAttrValue attrUniqueValue) {
        return (SyncopeUser) findByAttrUniqueValue(schemaName, attrUniqueValue,
                AttributableUtil.getInstance(AttributableType.USER));
    }

    @Override
    public List<SyncopeUser> findByDerAttrValue(final String schemaName, final String value)
            throws InvalidSearchConditionException {

        return findByDerAttrValue(schemaName, value, AttributableUtil.getInstance(AttributableType.USER));
    }

    @Override
    public List<SyncopeUser> findByResource(final ExternalResource resource) {
        return findByResource(resource, SyncopeUser.class);
    }

    private StringBuilder getFindAllQuery(final Set<Long> adminRoles) {
        final StringBuilder queryString = new StringBuilder("SELECT id FROM SyncopeUser WHERE id NOT IN (");

        if (adminRoles == null || adminRoles.isEmpty()) {
            queryString.append("SELECT syncopeUser_id AS id FROM Membership");
        } else {
            queryString.append("SELECT syncopeUser_id FROM Membership M1 ").append("WHERE syncopeRole_id IN (");
            queryString.append("SELECT syncopeRole_id FROM Membership M2 ").append(
                    "WHERE M2.syncopeUser_id=M1.syncopeUser_id ").append("AND syncopeRole_id NOT IN (");

            queryString.append("SELECT id AS syncopeRole_id FROM SyncopeRole");
            boolean firstRole = true;
            for (Long adminRoleId : adminRoles) {
                if (firstRole) {
                    queryString.append(" WHERE");
                    firstRole = false;
                } else {
                    queryString.append(" OR");
                }

                queryString.append(" id=").append(adminRoleId);
            }

            queryString.append("))");
        }
        queryString.append(")");

        return queryString;
    }

    @Override
    public final List<SyncopeUser> findAll(final Set<Long> adminRoles) {
        return findAll(adminRoles, -1, -1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final List<SyncopeUser> findAll(final Set<Long> adminRoles, final int page, final int itemsPerPage) {
        final Query query = entityManager.createNativeQuery(getFindAllQuery(adminRoles).toString());

        query.setFirstResult(itemsPerPage * (page <= 0
                ? 0
                : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        List<Number> userIds = new ArrayList<Number>();
        List<Object> resultList = query.getResultList();

        // fix for HHH-5902 - bug hibernate
        if (resultList != null) {
            for (Object userId : resultList) {
                if (userId instanceof Object[]) {
                    userIds.add((Number) ((Object[]) userId)[0]);
                } else {
                    userIds.add((Number) userId);
                }
            }
        }

        List<SyncopeUser> result = new ArrayList<SyncopeUser>(userIds.size());

        for (Number userId : userIds) {
            SyncopeUser user = findInternal(userId.longValue());
            if (user == null) {
                LOG.error("Could not find user with id {}, even though returned by the native query", userId);
            } else {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public final int count(final Set<Long> adminRoles) {
        StringBuilder queryString = getFindAllQuery(adminRoles);
        queryString.insert(0, "SELECT COUNT(id) FROM (");
        queryString.append(") count_user_id");

        Query countQuery = entityManager.createNativeQuery(queryString.toString());

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public SyncopeUser save(final SyncopeUser user) {
        final SyncopeUser merged = entityManager.merge(user);
        for (AbstractVirAttr virtual : merged.getVirAttrs()) {
            virtual.setValues(user.getVirAttr(virtual.getSchema().getName()).getValues());
        }

        return merged;
    }

    @Override
    public void delete(final Long id) {
        SyncopeUser user = findInternal(id);
        if (user == null) {
            return;
        }

        delete(user);
    }

    @Override
    public void delete(final SyncopeUser user) {
        // Not calling membershipDAO.delete() here because it would try to save this user as well, thus going into
        // ConcurrentModificationException
        for (Membership membership : user.getMemberships()) {
            membership.setSyncopeUser(null);

            roleDAO.save(membership.getSyncopeRole());
            membership.setSyncopeRole(null);

            entityManager.remove(membership);
        }
        user.getMemberships().clear();

        entityManager.remove(user);
    }
}
