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
package org.syncope.core.workflow.prcsiam;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.spi.WorkflowEntry;
import com.opensymphony.workflow.spi.hibernate.HibernateCurrentStep;
import java.util.List;
import java.util.Map;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.OSWorkflowComponent;

public class StoreUserIdAsCaller extends OSWorkflowComponent
        implements FunctionProvider {

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        UserTO userTO = (UserTO) transientVars.get("userTO");
        String userId = null;
        if (userTO == null) {
            SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                    Constants.SYNCOPE_USER);
            userId = Utils.getUserId(syncopeUser);
        } else {
            userId = Utils.getUserId(userTO);
        }

        WorkflowEntry entry = (WorkflowEntry) transientVars.get("entry");
        Workflow workflow = getUserWorkflow();
        List<HibernateCurrentStep> currentSteps = workflow.getCurrentSteps(
                entry.getId());
        for (HibernateCurrentStep currentStep : currentSteps) {
            currentStep.setCaller(userId);
        }
    }
}
