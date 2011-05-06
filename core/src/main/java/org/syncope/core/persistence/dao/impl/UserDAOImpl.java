/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;

@Repository
public class UserDAOImpl extends AbstractDAOImpl
        implements UserDAO {

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Override
    public SyncopeUser find(final Long id) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeUser.class.getSimpleName() + " e "
                + "WHERE e.id = :id");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);
        query.setParameter("id", id);

        try {
            return (SyncopeUser) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public SyncopeUser findByWorkflowId(final Long workflowId) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeUser.class.getSimpleName() + " e "
                + "WHERE e.workflowId = :workflowId");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);
        query.setParameter("workflowId", workflowId);

        return (SyncopeUser) query.getSingleResult();
    }

    @Override
    public List<SyncopeUser> findByAttrValue(final String schemaName,
            final UAttrValue attrValue) {

        USchema schema = schemaDAO.find(schemaName, USchema.class);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.EMPTY_LIST;
        }

        final String entityName = schema.isUniqueConstraint()
                ? UAttrUniqueValue.class.getName() : UAttrValue.class.getName();

        Query query = entityManager.createQuery(
                "SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.name = :schemaName "
                + " AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL"
                + " AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL"
                + " AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL"
                + " AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL"
                + " AND e.doubleValue = :doubleValue)");

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue",
                attrValue.getBooleanValue() == null
                ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        query.setParameter("dateValue", attrValue.getDateValue(),
                TemporalType.TIMESTAMP);
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<SyncopeUser> result = new ArrayList<SyncopeUser>();
        SyncopeUser user;
        for (AbstractAttrValue value :
                (List<AbstractAttrValue>) query.getResultList()) {

            user = (SyncopeUser) value.getAttribute().getOwner();
            if (!result.contains(user)) {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public SyncopeUser findByAttrUniqueValue(final String schemaName,
            final UAttrValue attrUniqueValue) {

        USchema schema = schemaDAO.find(schemaName, USchema.class);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return null;
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'",
                    schemaName);
            return null;
        }

        List<SyncopeUser> result = findByAttrValue(schemaName, attrUniqueValue);
        return result.isEmpty() ? null : result.iterator().next();
    }

    private StringBuilder getFindAllQuery(final Set<Long> adminRoles) {
        final StringBuilder queryString = new StringBuilder(
                "SELECT id FROM SyncopeUser WHERE id NOT IN (");

        if (adminRoles == null || adminRoles.isEmpty()) {
            queryString.append("SELECT syncopeUser_id AS id FROM Membership");
        } else {
            queryString.append("SELECT syncopeUser_id FROM Membership M1 ").
                    append("WHERE syncopeRole_id IN (");
            queryString.append("SELECT syncopeRole_id FROM Membership M2 ").
                    append("WHERE M2.syncopeUser_id=M1.syncopeUser_id ").
                    append("AND syncopeRole_id NOT IN (");

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

    @Override
    public final List<SyncopeUser> findAll(final Set<Long> adminRoles,
            final int page, final int itemsPerPage) {

        final Query query = entityManager.createNativeQuery(
                getFindAllQuery(adminRoles).toString());

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        List<Number> userIds = new ArrayList<Number>();
        userIds.addAll(query.getResultList());

        List<SyncopeUser> result =
                new ArrayList<SyncopeUser>(userIds.size());

        SyncopeUser user;
        for (Number userId : userIds) {
            user = find(userId.longValue());
            if (user == null) {
                LOG.error("Could not find user with id {}, "
                        + "even though returned by the native query", userId);
            } else {
                result.add(user);
            }
        }

        return result;
    }

    @Override
    public final Integer count(final Set<Long> adminRoles) {
        StringBuilder queryString = getFindAllQuery(adminRoles);
        queryString.insert(0, "SELECT COUNT(id) FROM (");
        queryString.append(") count_user_id");

        Query countQuery =
                entityManager.createNativeQuery(queryString.toString());

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public SyncopeUser save(final SyncopeUser user) {
        return entityManager.merge(user);
    }

    @Override
    public void delete(final Long id) {
        SyncopeUser user = find(id);
        if (user == null) {
            return;
        }

        delete(user);
    }

    @Override
    public void delete(final SyncopeUser user) {
        // Not calling membershipDAO.delete() here because it would try
        // to save this user as well, thus going into
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
