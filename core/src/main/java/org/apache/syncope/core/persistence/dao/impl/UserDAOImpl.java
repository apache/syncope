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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.AttributableCond;
import org.apache.syncope.core.persistence.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAOImpl extends AbstractAttributableDAOImpl implements UserDAO {

    @Autowired
    private AttributableSearchDAO searchDAO;

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
    public List<SyncopeUser> findByDerAttrValue(final String schemaName, final String value) {
        return findByDerAttrValue(schemaName, value, AttributableUtil.getInstance(AttributableType.USER));
    }

    @Override
    public List<SyncopeUser> findByResource(final ExternalResource resource) {
        return findByResource(resource, SyncopeUser.class);
    }

    @Override
    public final List<SyncopeUser> findAll(final Set<Long> adminRoles, final int page, final int itemsPerPage) {
        return findAll(adminRoles, page, itemsPerPage, Collections.<OrderByClause>emptyList());
    }

    private SearchCond getAllMatchingCond() {
        AttributableCond idCond = new AttributableCond(AttributeCond.Type.ISNOTNULL);
        idCond.setSchema("id");
        return SearchCond.getLeafCond(idCond);
    }

    @Override
    public List<SyncopeUser> findAll(final Set<Long> adminRoles,
            final int page, final int itemsPerPage, final List<OrderByClause> orderBy) {

        return searchDAO.search(adminRoles, getAllMatchingCond(), page, itemsPerPage, orderBy,
                AttributableUtil.getInstance(AttributableType.USER));
    }

    @Override
    public final int count(final Set<Long> adminRoles) {
        return searchDAO.count(adminRoles, getAllMatchingCond(), AttributableUtil.getInstance(AttributableType.USER));
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
