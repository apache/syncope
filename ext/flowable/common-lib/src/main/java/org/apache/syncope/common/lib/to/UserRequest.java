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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.BaseBean;

@XmlRootElement(name = "userRequest")
@XmlType
public class UserRequest extends BaseBean {

    private static final long serialVersionUID = -8430826310789942133L;

    private String bpmnProcess;

    private Date startTime;

    private String username;

    private String executionId;

    private String activityId;

    private String taskId;

    private boolean hasForm;

    public String getBpmnProcess() {
        return bpmnProcess;
    }

    public void setBpmnProcess(final String bpmnProcess) {
        this.bpmnProcess = bpmnProcess;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
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

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(final String activityId) {
        this.activityId = activityId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public boolean getHasForm() {
        return hasForm;
    }

    public void setHasForm(final boolean hasForm) {
        this.hasForm = hasForm;
    }

}
