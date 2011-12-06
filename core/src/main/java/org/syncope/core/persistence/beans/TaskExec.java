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
package org.syncope.core.persistence.beans;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Type;

/**
 * An execution (with result) of a Task.
 * @see PropagationTask
 */
@Entity
public class TaskExec extends AbstractBaseBean {

    private static final long serialVersionUID = 1909033231464074554L;

    /**
     * Id.
     */
    @Id
    private Long id;

    /**
     * Start instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    /**
     * End instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    /**
     * Any information to be accompained to this execution's result.
     */
    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String message;

    /**
     * The referred task.
     */
    @ManyToOne(optional = false)
    private Task task;

    @Column(nullable = false)
    private String status;

    public Long getId() {
        return id;
    }

    public Date getEndDate() {
        return endDate == null ? null : new Date(endDate.getTime());
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartDate() {
        return startDate == null ? null : new Date(startDate.getTime());
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate == null
                ? null : new Date(startDate.getTime());
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                + "id=" + getId() + ", "
                + "startDate=" + startDate + ", "
                + "endDate=" + endDate + ", "
                + "task=" + task + ", "
                + "status=" + status + ", "
                + "message=" + message + '}';
    }
}
