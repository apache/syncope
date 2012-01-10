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
package org.syncope.core.persistence.beans;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.OneToMany;

@Entity
@Inheritance
@DiscriminatorColumn(name = "DTYPE")
public class Task extends AbstractBaseBean {

    private static final long serialVersionUID = 5837401178128177511L;

    /**
     * Id.
     */
    @Id
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true,
    mappedBy = "task")
    private List<TaskExec> executions;

    public Task() {
        super();

        executions = new ArrayList<TaskExec>();
    }

    public Long getId() {
        return id;
    }

    public boolean addExec(TaskExec exec) {
        return exec != null && !executions.contains(exec)
                && executions.add(exec);
    }

    public boolean removeExec(TaskExec exec) {
        return exec != null && executions.remove(exec);
    }

    public List<TaskExec> getExecs() {
        return executions;
    }

    public void setExecs(List<TaskExec> executions) {
        this.executions.clear();
        if (executions != null && !executions.isEmpty()) {
            this.executions.addAll(executions);
        }
    }
}
