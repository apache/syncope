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

import org.syncope.core.workflow.OSWorkflowComponent;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.query.FieldExpression;
import com.opensymphony.workflow.query.WorkflowExpressionQuery;
import com.opensymphony.workflow.spi.Step;
import com.opensymphony.workflow.spi.WorkflowEntry;
import java.util.List;
import java.util.Map;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.WorkflowInitException;

public class CheckIfUserIdAlreadyExists extends OSWorkflowComponent
        implements Validator {

    @Override
    public void validate(Map transientVars, Map args, PropertySet ps)
            throws InvalidInputException, WorkflowException {

        UserTO userTO = (UserTO) transientVars.get(Constants.USER_TO);
        String userId = Utils.getUserId(userTO);

        // Check if there is at least another workflow instance involving the
        // same user, using userId as matching value (stored as owner)
        Workflow workflow = getUserWorkflow();
        WorkflowExpressionQuery query = new WorkflowExpressionQuery(
                new FieldExpression(FieldExpression.CALLER,
                FieldExpression.CURRENT_STEPS, FieldExpression.EQUALS, userId));
        List<Long> entries = workflow.query(query);
        if (!entries.isEmpty()) {
            WorkflowInitException initException = new WorkflowInitException();
            initException.setWorkflowEntry((WorkflowEntry) transientVars.get(
                    Constants.ENTRY));

            // Find SyncopeUser involved in ther other worklfow instance
            SyncopeUserDAO syncopeUserDAO = (SyncopeUserDAO) context.getBean(
                    "syncopeUserDAOImpl");
            UserAttributeValue userIdValue = new UserAttributeValue();
            userIdValue.setStringValue(userId);
            List<SyncopeUser> matchingUsers =
                    syncopeUserDAO.findByAttributeValue(userIdValue);
            if (matchingUsers != null && !matchingUsers.isEmpty()) {
                initException.setSyncopeUserId(
                        matchingUsers.iterator().next().getId());
            }

            // Check if the user, in the other workflow instance, has status
            // "active" or "suspended" and report the corresponding action to
            // take
            List<Step> currentSteps =
                    workflow.getCurrentSteps(entries.iterator().next());
            if (currentSteps.size() > 1) {
                throw new WorkflowException(
                        "Unexpected number of current steps: "
                        + currentSteps.size());
            }
            Step currentStep = currentSteps.iterator().next();
            if (currentStep.getStatus().equals("active")
                    || currentStep.getStatus().equals("suspended")) {

                initException.setExceptionOperation(
                        WorkflowInitException.ExceptionOperation.REJECT);
            } else {
                initException.setExceptionOperation(
                        WorkflowInitException.ExceptionOperation.OVERWRITE);
            }

            throw initException;
        }
    }
}
