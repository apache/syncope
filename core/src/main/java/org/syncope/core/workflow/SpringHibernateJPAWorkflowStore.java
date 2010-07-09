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
package org.syncope.core.workflow;

import com.opensymphony.workflow.StoreException;
import com.opensymphony.workflow.spi.hibernate.HibernateWorkflowEntry;
import com.opensymphony.workflow.spi.hibernate3.AbstractHibernateWorkflowStore;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class SpringHibernateJPAWorkflowStore
        extends AbstractHibernateWorkflowStore {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @Override
    public void init(Map props) throws StoreException {
    }

    @Override
    protected Object execute(InternalCallback action) throws StoreException {
        return action.doInHibernate((Session) entityManager.getDelegate());
    }

    public void delete(Long entryId) {
        HibernateWorkflowEntry entry =
                entityManager.find(HibernateWorkflowEntry.class, entryId);
        if (entry != null) {
            entityManager.remove(entry);
        }
    }
}
