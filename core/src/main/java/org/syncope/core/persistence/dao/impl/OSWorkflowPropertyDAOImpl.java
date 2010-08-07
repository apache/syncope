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

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.OSWorkflowProperty;
import org.syncope.core.persistence.dao.OSWorkflowPropertyDAO;

@Repository
public class OSWorkflowPropertyDAOImpl extends AbstractDAOImpl
        implements OSWorkflowPropertyDAO {

    @Override
    @Transactional(readOnly = true)
    public OSWorkflowProperty find(Long id) {
        return entityManager.find(OSWorkflowProperty.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public OSWorkflowProperty find(Long workflowEntryId, String propertyKey) {
        Query query = entityManager.createQuery(
                "SELECT e FROM OSWorkflowProperty e "
                + "WHERE e.workflowEntryId=:workflowEntryId "
                + "AND e.propertyKey=:propertyKey");
        query.setParameter("workflowEntryId", workflowEntryId);
        query.setParameter("propertyKey", propertyKey);

        OSWorkflowProperty result = null;
        try {
            result = (OSWorkflowProperty) query.getSingleResult();
        } catch (NoResultException e) {
        } catch (Throwable t) {
            log.error("Unexpected exception", t);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OSWorkflowProperty> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM OSWorkflowProperty e");
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OSWorkflowProperty> findAll(Long workflowEntryId) {
        Query query = entityManager.createQuery(
                "SELECT e FROM OSWorkflowProperty e "
                + "WHERE e.workflowEntryId=:workflowEntryId");
        query.setParameter("workflowEntryId", workflowEntryId);
        return query.getResultList();
    }

    @Override
    public OSWorkflowProperty save(OSWorkflowProperty property) {
        return entityManager.merge(property);
    }

    @Override
    public void delete(Long id) {
        OSWorkflowProperty osWorkflowProperty = find(id);
        if (osWorkflowProperty == null) {
            return;
        }

        entityManager.remove(osWorkflowProperty);
    }

    @Override
    public void delete(Long workflowEntryId, String propertyKey) {
        OSWorkflowProperty osWorkflowProperty =
                find(workflowEntryId, propertyKey);
        if (osWorkflowProperty == null) {
            return;
        }

        entityManager.remove(osWorkflowProperty);
    }
}
