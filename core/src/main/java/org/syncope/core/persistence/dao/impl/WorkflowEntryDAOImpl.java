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

import com.opensymphony.workflow.spi.hibernate.HibernateWorkflowEntry;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.dao.WorkflowEntryDAO;

@Repository
public class WorkflowEntryDAOImpl extends AbstractDAOImpl
        implements WorkflowEntryDAO {

    @Override
    public HibernateWorkflowEntry find(Long id) {
        return entityManager.find(HibernateWorkflowEntry.class, id);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        HibernateWorkflowEntry workflowEntry = find(id);
        if (workflowEntry != null) {
            entityManager.remove(workflowEntry);
        }
    }
}
