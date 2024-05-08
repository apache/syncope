/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.lib.to;

import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.common.lib.request.UserUR;

public class UserRequestForm extends SyncopeForm {

    private static final long serialVersionUID = -7044543391316529128L;

    private String bpmnProcess;

    private String username;

    private String executionId;

    private String taskId;

    private String formKey;

    private Date createTime;

    private Date dueDate;

    private String assignee;

    private UserTO userTO;

    private UserUR userUR;

    public String getBpmnProcess() {
        return bpmnProcess;
    }

    public void setBpmnProcess(final String bpmnProcess) {
        this.bpmnProcess = bpmnProcess;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(final String executionId) {
        this.executionId = executionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(final String formKey) {
        this.formKey = formKey;
    }

    public Date getCreateTime() {
        return Optional.ofNullable(createTime).map(date -> new Date(date.getTime())).orElse(null);
    }

    public void setCreateTime(final Date createTime) {
        this.createTime = Optional.ofNullable(createTime).map(date -> new Date(date.getTime())).orElse(null);
    }

    public Date getDueDate() {
        return Optional.ofNullable(dueDate).map(date -> new Date(date.getTime())).orElse(null);
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = Optional.ofNullable(dueDate).map(date -> new Date(date.getTime())).orElse(null);
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(final String assignee) {
        this.assignee = assignee;
    }

    public UserTO getUserTO() {
        return userTO;
    }

    public void setUserTO(final UserTO userTO) {
        this.userTO = userTO;
    }

    public UserUR getUserUR() {
        return userUR;
    }

    public void setUserUR(final UserUR userUR) {
        this.userUR = userUR;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(bpmnProcess).
                append(username).
                append(executionId).
                append(taskId).
                append(formKey).
                append(createTime).
                append(dueDate).
                append(assignee).
                append(userTO).
                append(userUR).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserRequestForm other = (UserRequestForm) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(bpmnProcess, other.bpmnProcess).
                append(username, other.username).
                append(executionId, other.executionId).
                append(taskId, other.taskId).
                append(formKey, other.formKey).
                append(createTime, other.createTime).
                append(dueDate, other.dueDate).
                append(assignee, other.assignee).
                append(userTO, other.userTO).
                append(userUR, other.userUR).
                build();
    }
}
