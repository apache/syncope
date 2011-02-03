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
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.spi.Step;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.types.TaskExecutionStatus;

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

        WorkflowDescriptor workflowDescriptor =
                workflow.getWorkflowDescriptor(workflowName);

        int[] actions = workflow.getAvailableActions(workflowId, null);

        Integer actionId = null;
        for (int i = 0; i < actions.length && actionId == null; i++) {
            if (actionName.equals(
                    workflowDescriptor.getAction(actions[i]).getName())) {

                actionId = actions[i];
            }
        }

        Map<Integer, ActionDescriptor> commonActions =
                workflowDescriptor.getCommonActions();
        for (Integer actionNumber : commonActions.keySet()) {
            if (actionName.equals(commonActions.get(actionNumber).getName())) {
                actionId = actionNumber;
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

        Map<String, Object> inputs = new HashMap<String, Object>();
        if (moreInputs != null && !moreInputs.isEmpty()) {
            inputs.putAll(moreInputs);
        }

        Integer actionId = findWorkflowAction(
                workflow, workflowName, workflowId, actionName);
        if (actionId == null) {
            throw new NotFoundException("Workflow action '" + actionName + "'");
        }

        try {
            workflow.doAction(workflowId, actionId, inputs);
        } catch (InvalidActionException e) {
            throw new WorkflowException(e);
        }
    }

    public static TaskExecutionStatus getTaskExecutionStatus(
            final Workflow workflow,
            final TaskExecution execution) {

        TaskExecutionStatus result = TaskExecutionStatus.FAILURE;

        try {
            List<Step> steps =
                    workflow.getCurrentSteps(execution.getWorkflowId());
            if (steps != null && !steps.isEmpty()) {
                result = TaskExecutionStatus.valueOf(
                        steps.iterator().next().getStatus());
            }
        } catch (Throwable t) {
            LOG.error("While getting status of {}", execution, t);
        }

        return result;
    }
}
