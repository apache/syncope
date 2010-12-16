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
package jpasymphony.relationships;

import com.opensymphony.workflow.spi.WorkflowEntry;
import com.opensymphony.workflow.spi.WorkflowStore;
import static org.junit.Assert.*;

import com.opensymphony.workflow.StoreException;
import com.opensymphony.workflow.config.SpringConfiguration;
import com.opensymphony.workflow.spi.Step;
import java.util.Date;
import jpasymphony.beans.JPACurrentStep;
import jpasymphony.dao.JPAWorkflowEntryDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:syncopeContext.xml",
    "classpath:persistenceContext.xml",
    "classpath:workflowContext.xml"
})
public class JPAWorkflowEntryTest {

    @Autowired
    private SpringConfiguration springConfiguration;

    @Autowired
    private JPAWorkflowEntryDAO workflowEntryDAO;

    @Test
    public final void delete()
            throws StoreException {

        WorkflowStore workflowStore = springConfiguration.getWorkflowStore();
        WorkflowEntry entry = workflowStore.createEntry("userWorkflow");
        assertNotNull(entry);
        Long entryId = entry.getId();

        Step step = workflowStore.createCurrentStep(entry.getId(), 11,
                "owner", new Date(), new Date(), "status", new long[0]);
        assertNotNull(step);

        ((JPACurrentStep) step).setActionId(999);
        workflowStore.moveToHistory(step);

        step = workflowStore.createCurrentStep(entry.getId(), 12,
                "owner", new Date(), new Date(), "status", new long[0]);

        workflowEntryDAO.delete(entryId);

        workflowEntryDAO.flush();

        assertNull(workflowEntryDAO.find(entryId));
    }
}
