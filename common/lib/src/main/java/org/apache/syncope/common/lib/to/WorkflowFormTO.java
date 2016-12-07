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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "workflowForm")
@XmlType
public class WorkflowFormTO extends AbstractBaseBean {

    private static final long serialVersionUID = -7044543391316529128L;

    private String username;

    private String taskId;

    private String key;

    private Date createTime;

    private Date dueDate;

    private String owner;

    private final List<WorkflowFormPropertyTO> properties = new ArrayList<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Date getCreateTime() {
        if (createTime != null) {
            return new Date(createTime.getTime());
        }
        return null;
    }

    public void setCreateTime(final Date createTime) {
        if (createTime != null) {
            this.createTime = new Date(createTime.getTime());
        } else {
            this.createTime = null;
        }
    }

    public Date getDueDate() {
        if (dueDate != null) {
            return new Date(dueDate.getTime());
        }
        return null;
    }

    public void setDueDate(final Date dueDate) {
        if (dueDate != null) {
            this.dueDate = new Date(dueDate.getTime());
        } else {
            this.dueDate = null;
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @XmlElementWrapper(name = "workflowFormProperties")
    @XmlElement(name = "workflowFormProperty")
    @JsonProperty("workflowFormProperties")
    public List<WorkflowFormPropertyTO> getProperties() {
        return properties;
    }

    public boolean addProperty(final WorkflowFormPropertyTO property) {
        return properties.contains(property)
                ? true
                : properties.add(property);
    }

    public boolean removeProperty(final WorkflowFormPropertyTO property) {
        return properties.remove(property);
    }

    @JsonIgnore
    public Map<String, WorkflowFormPropertyTO> getPropertyMap() {
        Map<String, WorkflowFormPropertyTO> result = new HashMap<>();
        for (WorkflowFormPropertyTO prop : getProperties()) {
            result.put(prop.getId(), prop);
        }

        return Collections.unmodifiableMap(result);
    }

    @JsonIgnore
    public Map<String, String> getPropertiesForSubmit() {
        Map<String, String> props = new HashMap<>();
        for (WorkflowFormPropertyTO prop : getProperties()) {
            if (prop.isWritable()) {
                props.put(prop.getId(), prop.getValue());
            }
        }

        return Collections.unmodifiableMap(props);
    }
}
