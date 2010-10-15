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
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.syncope.types.TaskExecutionStatus;

/**
 * An execution (with result) of a Task.
 * @see Task
 */
@Entity
public class TaskExecution extends AbstractBaseBean {

    /**
     * Id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
     * Did this execution completed with success?
     */
    @Enumerated(EnumType.STRING)
    private TaskExecutionStatus status;
    /**
     * Any information to be accompained to this execution's result.
     */
    @Lob
    private String message;
    /**
     * The referred task.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Task task;

    /**
     * Default constructor.
     */
    public TaskExecution() {
        super();

        status = TaskExecutionStatus.CREATED;
    }

    public Long getId() {
        return id;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public TaskExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(TaskExecutionStatus status) {
        this.status = status;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "TaskExecution{"
                + "id=" + id + ", "
                + "startDate=" + startDate + ", "
                + "endDate=" + endDate + ", "
                + "status=" + status + ", "
                + "task=" + task + ", "
                + "message=" + message + '}';
    }
}
