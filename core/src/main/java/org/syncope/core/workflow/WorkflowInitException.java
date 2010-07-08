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
import com.opensymphony.workflow.spi.WorkflowEntry;

public class WorkflowInitException extends InvalidInputException {

    public enum ExceptionOperation {

        REJECT, OVERWRITE;
    };
    private ExceptionOperation exceptionOperation;
    private Long syncopeUserId;
    private WorkflowEntry workflowEntry;

    public WorkflowInitException() {
        super();
    }

    public WorkflowInitException(ExceptionOperation exceptionOperation,
            Long syncopeUserId, WorkflowEntry workflowEntry) {

        super();

        this.exceptionOperation = exceptionOperation;
        this.syncopeUserId = syncopeUserId;
        this.workflowEntry = workflowEntry;
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

    public WorkflowEntry getWorkflowEntry() {
        return workflowEntry;
    }

    public void setWorkflowEntry(WorkflowEntry workflowEntry) {
        this.workflowEntry = workflowEntry;
    }

    @Override
    public String toString() {
        return "WorkflowInitException{"
                + "exceptionOperation=" + exceptionOperation + ","
                + "syncopeUserId=" + syncopeUserId + ","
                + "workflowEntry=" + workflowEntry.getId() + '}';
    }
}
