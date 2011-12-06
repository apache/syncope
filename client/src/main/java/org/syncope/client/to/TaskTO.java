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

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.AbstractBaseBean;

public class TaskTO extends AbstractBaseBean {

    private static final long serialVersionUID = 386450127003321197L;

    private long id;

    private String latestExecStatus;

    private List<TaskExecTO> executions;

    public TaskTO() {
        super();

        executions = new ArrayList<TaskExecTO>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLatestExecStatus() {
        return latestExecStatus;
    }

    public void setLatestExecStatus(String latestExecStatus) {
        this.latestExecStatus = latestExecStatus;
    }

    public boolean addExecution(TaskExecTO execution) {
        return executions.add(execution);
    }

    public boolean removeExecution(TaskExecTO execution) {
        return executions.remove(execution);
    }

    public List<TaskExecTO> getExecutions() {
        return executions;
    }

    public void setExecutions(List<TaskExecTO> executions) {
        this.executions = executions;
    }
}
