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
import java.util.List;
import org.syncope.client.AbstractBaseBean;
import org.syncope.client.report.Reportlet;

public class ReportTO extends AbstractBaseBean {

    private static final long serialVersionUID = 5274568072084814410L;

    private long id;

    private String name;

    private List<Reportlet> reportlets;

    private String cronExpression;

    private List<ReportExecTO> executions;

    public ReportTO() {
        super();

        reportlets = new ArrayList<Reportlet>();
        executions = new ArrayList<ReportExecTO>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean addReportlet(Reportlet reportlet) {
        return this.reportlets.add(reportlet);
    }

    public boolean removeReportlet(Reportlet reportlet) {
        return this.reportlets.remove(reportlet);
    }

    public List<Reportlet> getReportlets() {
        return reportlets;
    }

    public void setReportlets(List<Reportlet> reportlets) {
        this.reportlets = reportlets;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean addExec(ReportExecTO execution) {
        return executions.add(execution);
    }

    public boolean removeExec(ReportExecTO execution) {
        return executions.remove(execution);
    }

    public List<ReportExecTO> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ReportExecTO> executions) {
        this.executions = executions;
    }
}
