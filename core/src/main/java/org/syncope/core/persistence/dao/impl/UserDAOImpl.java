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
                "SELECT e FROM SyncopeUser e WHERE e.id = :id");
        query.setHint("org.hibernate.cacheable", true);
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
                "SELECT e FROM SyncopeUser e WHERE e.workflowId = :workflowId");
        query.setHint("org.hibernate.cacheable", true);
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

    @Override
    public final List<SyncopeUser> findAll() {
        return findAll(-1, -1);
    }

    @Override
    public final List<SyncopeUser> findAll(
            final int page, final int itemsPerPage) {

        final Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeUser e ORDER BY e.id");

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public final Integer count() {
        final Query query = entityManager.createQuery(
                "SELECT count(e.id) FROM SyncopeUser e");

        return ((Long) query.getSingleResult()).intValue();
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
