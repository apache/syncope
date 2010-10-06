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

import java.util.Collections;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Repository
public class SyncopeUserDAOImpl extends AbstractDAOImpl
        implements SyncopeUserDAO {

    @Autowired
    private SearchUtils searchUtils;

    @Override
    @Transactional(readOnly = true)
    public final SyncopeUser find(final Long id) {
        Query query = entityManager.createNamedQuery("SyncopeUser.find");
        query.setParameter("id", id);

        try {
            return (SyncopeUser) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public final SyncopeUser findByWorkflowId(final Long workflowId) {
        Query query = entityManager.createNamedQuery(
                "SyncopeUser.findByWorkflowId");
        query.setParameter("workflowId", workflowId);

        return (SyncopeUser) query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public final List<SyncopeUser> findByAttributeValue(
            final UserAttributeValue attributeValue) {

        Query query = entityManager.createQuery(
                "SELECT u"
                + " FROM SyncopeUser u, UserAttribute ua, UserAttributeValue e "
                + " WHERE e.attribute = ua AND ua.owner = u"
                + " AND ((e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL"
                + " AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL"
                + " AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL"
                + " AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL"
                + " AND e.doubleValue = :doubleValue))");
        query.setParameter("stringValue", attributeValue.getStringValue());
        query.setParameter("booleanValue", attributeValue.getBooleanValue());
        query.setParameter("dateValue", attributeValue.getDateValue());
        query.setParameter("longValue", attributeValue.getLongValue());
        query.setParameter("doubleValue", attributeValue.getDoubleValue());

        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public final List<SyncopeUser> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeUser e");
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public final List<SyncopeUser> search(final NodeCond searchCondition) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Search condition:\n" + searchCondition);
        }

        Session hibernateSess = ((Session) entityManager.getDelegate());

        List<SyncopeUser> result = Collections.EMPTY_LIST;
        try {
            Criteria userCriteria = searchUtils.buildUserCriteria(
                    hibernateSess, searchCondition);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Criteria to be performed:\n" + userCriteria);
            }

            result = userCriteria.list();
        } catch (Throwable t) {
            LOG.error("While searching users", t);
        }

        return result;
    }

    @Override
    public final SyncopeUser save(final SyncopeUser syncopeUser) {
        return entityManager.merge(syncopeUser);
    }

    @Override
    public final void delete(final Long id) {
        SyncopeUser user = find(id);
        if (user == null) {
            return;
        }

        for (Membership membership : user.getMemberships()) {
            membership.setSyncopeUser(null);
            membership.getSyncopeRole().removeMembership(membership);
            membership.setSyncopeRole(null);

            entityManager.remove(membership);
        }
        user.setMemberships(Collections.EMPTY_LIST);

        entityManager.remove(user);
    }
}
