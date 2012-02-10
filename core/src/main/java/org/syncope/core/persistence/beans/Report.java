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
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import org.syncope.client.report.Reportlet;
import org.syncope.core.persistence.validation.entity.ReportCheck;
import org.syncope.core.util.XMLSerializer;

@Entity
@ReportCheck
public class Report extends AbstractBaseBean {

    private static final long serialVersionUID = -587652654964285834L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "reportlets")
    private List<String> reportlets;

    private String cronExpression;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true,
    mappedBy = "report")
    private List<ReportExec> executions;

    public Report() {
        super();

        reportlets = new ArrayList<String>();
        executions = new ArrayList<ReportExec>();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean addExec(ReportExec exec) {
        return exec != null && !executions.contains(exec)
                && executions.add(exec);
    }

    public boolean removeExec(ReportExec exec) {
        return exec != null && executions.remove(exec);
    }

    public List<ReportExec> getExecs() {
        return executions;
    }

    public void setExecs(List<ReportExec> executions) {
        this.executions.clear();
        if (executions != null && !executions.isEmpty()) {
            this.executions.addAll(executions);
        }
    }

    public boolean addReportlet(Reportlet reportlet) {
        if (reportlet == null) {
            return false;
        }

        String xmlReportlet = XMLSerializer.serialize(reportlet);
        return !reportlets.contains(xmlReportlet)
                && reportlets.add(xmlReportlet);
    }

    public boolean removeReportlet(Reportlet reportlet) {
        if (reportlet == null) {
            return false;
        }

        String xmlReportlet = XMLSerializer.serialize(reportlet);
        return reportlets.remove(xmlReportlet);
    }

    public List<Reportlet> getReportlets() {
        List<Reportlet> result = new ArrayList<Reportlet>(reportlets.size());
        for (String xmlReportlet : reportlets) {
            result.add(XMLSerializer.<Reportlet>deserialize(xmlReportlet));
        }

        return result;
    }

    public void setReportlets(List<Reportlet> reportlets) {
        this.reportlets.clear();
        if (reportlets != null && !reportlets.isEmpty()) {
            for (Reportlet reportlet : reportlets) {
                addReportlet(reportlet);
            }
        }
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
