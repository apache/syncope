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
package org.apache.syncope.to;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.AbstractBaseBean;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
@XmlType
public class WorkflowFormTO extends AbstractBaseBean {

    private static final long serialVersionUID = -7044543391316529128L;

    private String taskId;

    private String key;

    private String description;

    private Date createTime;

    private Date dueDate;

    private String owner;

    private final List<WorkflowFormPropertyTO> properties;

    public WorkflowFormTO() {
        properties = new ArrayList<WorkflowFormPropertyTO>();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @XmlElement(name="workflowFormPropertyTO")
    @XmlElementWrapper(name="properties")
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

    public void setProperties(final Collection<WorkflowFormPropertyTO> properties) {

        this.properties.clear();
        if (properties != null) {
            this.properties.addAll(properties);
        }
    }

    @JsonIgnore
    public Map<String, WorkflowFormPropertyTO> getPropertyMap() {
        Map<String, WorkflowFormPropertyTO> result;

        if (getProperties() == null) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<String, WorkflowFormPropertyTO>();
            for (WorkflowFormPropertyTO prop : getProperties()) {
                result.put(prop.getId(), prop);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }

    @JsonIgnore
    public Map<String, String> getPropertiesForSubmit() {
        Map<String, String> props = new HashMap<String, String>();
        for (WorkflowFormPropertyTO prop : getProperties()) {
            if (prop.isWritable()) {
                props.put(prop.getId(), prop.getValue());
            }
        }

        return props;
    }
}
