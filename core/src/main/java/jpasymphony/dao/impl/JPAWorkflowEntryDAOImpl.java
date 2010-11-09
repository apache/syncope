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
package jpasymphony.dao.impl;

import java.util.List;
import javax.persistence.Query;
import jpasymphony.beans.JPACurrentStep;
import jpasymphony.beans.JPAPropertySetItem;
import jpasymphony.beans.JPAWorkflowEntry;
import jpasymphony.dao.JPAPropertySetItemDAO;
import jpasymphony.dao.JPAWorkflowEntryDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.dao.impl.AbstractDAOImpl;

@Repository
public class JPAWorkflowEntryDAOImpl extends AbstractDAOImpl
        implements JPAWorkflowEntryDAO {

    @Autowired
    private JPAPropertySetItemDAO propertySetItemDAO;

    @Override
    @Transactional(readOnly = true)
    public JPAWorkflowEntry find(final Long id) {
        return entityManager.find(JPAWorkflowEntry.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JPAWorkflowEntry> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM JPAWorkflowEntry e");
        return query.getResultList();
    }

    @Override
    public JPAWorkflowEntry save(final JPAWorkflowEntry entry) {
        return entityManager.merge(entry);
    }

    @Override
    public void delete(final Long id) {
        JPAWorkflowEntry entry = find(id);
        if (entry == null) {
            return;
        }

        List<JPAPropertySetItem> properties =
                propertySetItemDAO.findAll(entry.getId());
        if (properties != null) {
            for (JPAPropertySetItem property : properties) {
                propertySetItemDAO.delete(property.getId());
            }
        }

        entityManager.remove(entry);
    }

    @Override
    public void deleteCurrentStep(final Long stepId) {
        JPACurrentStep step = entityManager.find(JPACurrentStep.class, stepId);
        if (step != null) {
            entityManager.remove(step);
        }
    }
}
