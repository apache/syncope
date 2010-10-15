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
package org.syncope.client.to;

import java.util.List;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;

public class TaskTO extends AbstractBaseBean {

    private long id;
    private PropagationMode propagationMode;
    private ResourceOperationType resourceOperationType;
    private String accountId;
    private String oldAccountId;
    private String xmlAttributes;
    private String resource;
    private List<Long> executions;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean addExecution(Long execution) {
        return executions.add(execution);
    }

    public boolean removeExecution(Long execution) {
        return executions.add(execution);
    }

    public List<Long> getExecutions() {
        return executions;
    }

    public void setExecutions(List<Long> executions) {
        this.executions = executions;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public ResourceOperationType getResourceOperationType() {
        return resourceOperationType;
    }

    public void setResourceOperationType(
            ResourceOperationType resourceOperationType) {

        this.resourceOperationType = resourceOperationType;
    }

    public String getXmlAttributes() {
        return xmlAttributes;
    }

    public void setXmlAttributes(String xmlAttributes) {
        this.xmlAttributes = xmlAttributes;
    }
}
