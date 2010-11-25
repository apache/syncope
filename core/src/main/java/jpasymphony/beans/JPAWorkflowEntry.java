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
package jpasymphony.beans;

import com.opensymphony.workflow.spi.WorkflowEntry;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.syncope.core.persistence.beans.AbstractBaseBean;

@Entity
@Table(name = "os_wfentry")
public class JPAWorkflowEntry extends AbstractBaseBean
        implements WorkflowEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String workflowName;

    private Integer workflowState;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workflowEntry")
    private List<JPACurrentStep> currentSteps;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workflowEntry")
    private List<JPAHistoryStep> historySteps;

    public JPAWorkflowEntry() {
        currentSteps = new ArrayList<JPACurrentStep>();
        historySteps = new ArrayList<JPAHistoryStep>();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public int getState() {
        return workflowState;
    }

    public Integer getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(final Integer workflowState) {
        this.workflowState = workflowState;
    }

    @Override
    public boolean isInitialized() {
        return workflowState > 0;
    }

    @Override
    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }

    public boolean addCurrentStep(final JPACurrentStep currentStep) {
        currentStep.setWorkflowEntry(this);
        return currentSteps.contains(currentStep)
                ? false : currentSteps.add(currentStep);
    }

    public boolean removeCurrentStep(final JPACurrentStep currentStep) {
        boolean result = currentSteps.remove(currentStep);
        currentStep.setWorkflowEntry(null);
        return result;
    }

    public List<JPACurrentStep> getCurrentSteps() {
        return currentSteps;
    }

    public void setCurrentSteps(final List<JPACurrentStep> currentSteps) {
        for (JPACurrentStep step : this.currentSteps) {
            step.setWorkflowEntry(null);
        }
        this.currentSteps.clear();

        if (currentSteps != null && !currentSteps.isEmpty()) {
            this.currentSteps.addAll(currentSteps);
        }
        for (JPACurrentStep step : this.currentSteps) {
            step.setWorkflowEntry(this);
        }
    }

    public boolean addHistoryStep(final JPAHistoryStep historyStep) {
        historyStep.setWorkflowEntry(this);
        return historySteps.contains(historyStep)
                ? false : historySteps.add(historyStep);
    }

    public boolean removeHistoryStep(final JPAHistoryStep historyStep) {
        boolean result = historySteps.remove(historyStep);
        historyStep.setWorkflowEntry(null);
        return result;
    }

    public List<JPAHistoryStep> getHistorySteps() {
        return historySteps;
    }

    public void setHistorySteps(final List<JPAHistoryStep> historySteps) {
        for (JPAHistoryStep step : this.historySteps) {
            step.setWorkflowEntry(null);
        }
        this.historySteps.clear();

        if (historySteps != null && !historySteps.isEmpty()) {
            this.historySteps.addAll(historySteps);
        }
        for (JPAHistoryStep step : this.historySteps) {
            step.setWorkflowEntry(this);
        }
    }
}
