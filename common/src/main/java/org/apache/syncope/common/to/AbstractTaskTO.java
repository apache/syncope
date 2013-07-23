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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement(name = "task")
@XmlType
@XmlSeeAlso({SyncTaskTO.class, NotificationTaskTO.class, SyncTaskTO.class, SchedTaskTO.class, PropagationTaskTO.class})
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "@class")
public abstract class AbstractTaskTO extends AbstractBaseBean {

    private static final long serialVersionUID = 386450127003321197L;

    private long id;

    private String latestExecStatus;

    private List<TaskExecTO> executions = new ArrayList<TaskExecTO>();

    private Date startDate;

    private Date endDate;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getLatestExecStatus() {
        return latestExecStatus;
    }

    public void setLatestExecStatus(final String latestExecStatus) {
        this.latestExecStatus = latestExecStatus;
    }

    public boolean addExecution(final TaskExecTO execution) {
        return executions.add(execution);
    }

    public boolean removeExecution(final TaskExecTO execution) {
        return executions.remove(execution);
    }

    @XmlElementWrapper(name = "excecutions")
    @XmlElement(name = "excecution")
    public List<TaskExecTO> getExecutions() {
        return executions;
    }

    public void setExecutions(final List<TaskExecTO> executions) {
        this.executions = executions;
    }

    public Date getStartDate() {
        return startDate == null
                ? null
                : new Date(startDate.getTime());
    }

    public void setStartDate(final Date startDate) {
        if (startDate != null) {
            this.startDate = new Date(startDate.getTime());
        }
    }

    public Date getEndDate() {
        return endDate == null
                ? null
                : new Date(endDate.getTime());
    }

    public void setEndDate(final Date endDate) {
        if (endDate != null) {
            this.endDate = new Date(endDate.getTime());
        }
    }
}
