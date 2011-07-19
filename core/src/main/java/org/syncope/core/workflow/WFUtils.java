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

import com.opensymphony.workflow.InvalidActionException;
import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import java.util.Collections;
import java.util.Map;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WFUtils {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(WFUtils.class);

    private WFUtils() {
    }

    public static Integer findWorkflowAction(
            final Workflow workflow,
            final String workflowName,
            final Long workflowId,
            final String actionName) {

        int[] actions = workflow.getAvailableActions(workflowId, null);

        Integer actionId = null;
        for (int i = 0; i < actions.length && actionId == null; i++) {
            if (actionName.equals(
                    workflow.getWorkflowDescriptor(workflowName).
                    getAction(actions[i]).getName())) {

                actionId = actions[i];
            }
        }

        if (actionId == null) {
            Map<Integer, ActionDescriptor> commonActions = workflow.
                    getWorkflowDescriptor(workflowName).getCommonActions();
            for (Map.Entry<Integer, ActionDescriptor> action :
                    commonActions.entrySet()) {
                if (actionName.equals(action.getValue().getName())) {
                    actionId = action.getKey();
                }
            }
        }

        return actionId;
    }

    public static void doExecuteAction(
            final Workflow workflow,
            final String workflowName,
            final String actionName,
            final Long workflowId,
            final Map<String, Object> moreInputs)
            throws WorkflowException, NotFoundException {

        Integer actionId = findWorkflowAction(
                workflow, workflowName, workflowId, actionName);
        if (actionId == null) {
            throw new NotFoundException("Workflow action '" + actionName + "'");
        }

        try {
            workflow.doAction(workflowId, actionId, moreInputs == null
                    ? Collections.EMPTY_MAP : moreInputs);
        } catch (InvalidActionException e) {
            throw new WorkflowException(e);
        }
    }
}
