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
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.NodeSearchCondition;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Repository
public class SyncopeUserDAOImpl extends AbstractDAOImpl
        implements SyncopeUserDAO {

    @Override
    @Transactional(readOnly = true)
    public SyncopeUser find(Long id) {
        return entityManager.find(SyncopeUser.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncopeUser> findByAttributeValue(
            UserAttributeValue attributeValue) {

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
    public List<SyncopeUser> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeUser e");
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncopeUser> search(NodeSearchCondition searchCondition) {
        String queryString = QueryUtils.getUserSearchQuery(searchCondition);
        if (log.isDebugEnabled()) {
            log.debug("About to execute query\n\t" + queryString + "\n");
        }

        List<SyncopeUser> result = Collections.EMPTY_LIST;
        try {
            Query query = entityManager.createQuery(queryString);
            result = query.getResultList();
        } catch (Throwable t) {
            log.error("While executing query\n\t" + queryString + "\n", t);
        }

        return result;
    }

    @Override
    public SyncopeUser save(SyncopeUser syncopeUser) {
        return entityManager.merge(syncopeUser);
    }

    @Override
    public void delete(Long id) {
        SyncopeUser user = find(id);
        if (id == null) {
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
