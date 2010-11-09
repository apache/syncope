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
package jpasymphony.beans;

import com.opensymphony.workflow.spi.Step;
import java.util.Date;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.syncope.core.persistence.beans.AbstractBaseBean;

@MappedSuperclass
public abstract class AbstractJPAStep extends AbstractBaseBean implements Step {

    protected Integer actionId;

    protected Integer stepId;

    protected String caller;

    @Temporal(TemporalType.TIMESTAMP)
    protected Date finishDate;

    @Temporal(TemporalType.TIMESTAMP)
    protected Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    protected Date dueDate;

    protected String owner;

    protected String status;

    @ManyToOne
    protected JPAWorkflowEntry workflowEntry;

    public JPAWorkflowEntry getWorkflowEntry() {
        return workflowEntry;
    }

    public void setWorkflowEntry(JPAWorkflowEntry workflowEntry) {
        this.workflowEntry = workflowEntry;
    }

    public void setActionId(Integer actionId) {
        this.actionId = actionId;
    }

    @Override
    public int getActionId() {
        return actionId;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    @Override
    public String getCaller() {
        return caller;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    @Override
    public long getEntryId() {
        return workflowEntry.getId();
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    @Override
    public Date getFinishDate() {
        return finishDate;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public void setStepId(Integer stepId) {
        this.stepId = stepId;
    }

    @Override
    public int getStepId() {
        return stepId;
    }

    /**
     * This is for backward compatibility, but current Store doesn't
     * persist this collection, nor is such property visibile outside
     * OSWF internal classes.
     * @return an empty long[]
     */
    @Override
    public final long[] getPreviousStepIds() {
        return new long[0];
    }
}
