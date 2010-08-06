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

import com.opensymphony.workflow.InvalidInputException;

public class WorkflowInitException extends InvalidInputException {

    public enum ExceptionOperation {

        REJECT, OVERWRITE;
    };
    private ExceptionOperation exceptionOperation;
    private Long syncopeUserId;
    private Long workflowId;
    private Long workflowEntryId;

    public WorkflowInitException() {
        super();
    }

    public WorkflowInitException(ExceptionOperation exceptionOperation,
            Long syncopeUserId, Long workflowEntryId) {

        super();

        this.exceptionOperation = exceptionOperation;
        this.syncopeUserId = syncopeUserId;
        this.workflowEntryId = workflowEntryId;
    }

    public ExceptionOperation getExceptionOperation() {
        return exceptionOperation;
    }

    public void setExceptionOperation(ExceptionOperation exceptionOperation) {
        this.exceptionOperation = exceptionOperation;
    }

    public Long getSyncopeUserId() {
        return syncopeUserId;
    }

    public void setSyncopeUserId(Long syncopeUserId) {
        this.syncopeUserId = syncopeUserId;
    }

    public Long getWorkflowEntryId() {
        return workflowEntryId;
    }

    public void setWorkflowEntryId(Long workflowEntryId) {
        this.workflowEntryId = workflowEntryId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "WorkflowInitException{"
                + "exceptionOperation=" + exceptionOperation + ","
                + "syncopeUserId=" + syncopeUserId + ","
                + "workflowId=" + workflowId + ","
                + "workflowEntry=" + workflowEntryId + '}';
    }
}
