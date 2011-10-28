/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.syncope.client.AbstractBaseBean;

public class WorkflowFormTO extends AbstractBaseBean {

    private static final long serialVersionUID = -7044543391316529128L;

    private String taskId;

    private String key;

    private String description;

    private Date createTime;

    private Date dueDate;

    private String owner;

    private List<WorkflowFormPropertyTO> properties;

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

    public List<WorkflowFormPropertyTO> getProperties() {
        return properties;
    }

    public boolean addProperty(final WorkflowFormPropertyTO property) {
        return properties.contains(property)
                ? true : properties.add(property);
    }

    public boolean removeProperty(final WorkflowFormPropertyTO property) {
        return properties.remove(property);
    }

    public void setProperties(final List<WorkflowFormPropertyTO> properties) {
        if (properties == null) {
            this.properties.clear();
        } else {
            this.properties = properties;
        }
    }

    @JsonIgnore
    public Map<String, WorkflowFormPropertyTO> getPropertiesAsMap() {
        Map<String, WorkflowFormPropertyTO> props =
                new HashMap<String, WorkflowFormPropertyTO>();
        for (WorkflowFormPropertyTO prop : getProperties()) {
            props.put(prop.getId(), prop);
        }

        return props;
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
