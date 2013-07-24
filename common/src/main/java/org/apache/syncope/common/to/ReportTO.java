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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.report.AbstractReportletConf;
import org.apache.syncope.common.report.StaticReportletConf;
import org.apache.syncope.common.report.UserReportletConf;

@XmlRootElement(name = "report")
@XmlType
@XmlSeeAlso({UserReportletConf.class, StaticReportletConf.class})
public class ReportTO extends AbstractBaseBean {

    private static final long serialVersionUID = 5274568072084814410L;

    private long id;

    private String name;

    private List<AbstractReportletConf> reportletConfs;

    private String cronExpression;

    private List<ReportExecTO> executions;

    private String latestExecStatus;

    private Date lastExec;

    private Date nextExec;

    private Date startDate;

    private Date endDate;

    public ReportTO() {
        super();

        reportletConfs = new ArrayList<AbstractReportletConf>();
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

    public boolean addReportletConf(AbstractReportletConf reportlet) {
        return this.reportletConfs.add(reportlet);
    }

    public boolean removeReportletConf(AbstractReportletConf reportlet) {
        return this.reportletConfs.remove(reportlet);
    }

    @XmlElementWrapper(name = "reportletConfs")
    @XmlElement(name = "reportletConf")
    @JsonProperty("reportletConfs")
    public List<AbstractReportletConf> getReportletConfs() {
        return reportletConfs;
    }

    public void setReportletConfs(List<AbstractReportletConf> reportlets) {
        this.reportletConfs = reportlets;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean addExecution(ReportExecTO execution) {
        return executions.add(execution);
    }

    public boolean removeExecution(ReportExecTO execution) {
        return executions.remove(execution);
    }

    @XmlElementWrapper(name = "executions")
    @XmlElement(name = "execution")
    @JsonProperty("executions")
    public List<ReportExecTO> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ReportExecTO> executions) {
        this.executions = executions;
    }

    public String getLatestExecStatus() {
        return latestExecStatus;
    }

    public void setLatestExecStatus(String latestExecStatus) {
        this.latestExecStatus = latestExecStatus;
    }

    public Date getLastExec() {
        return lastExec == null
                ? null
                : new Date(lastExec.getTime());
    }

    public void setLastExec(Date lastExec) {
        if (lastExec != null) {
            this.lastExec = new Date(lastExec.getTime());
        }
    }

    public Date getNextExec() {
        return nextExec == null
                ? null
                : new Date(nextExec.getTime());
    }

    public void setNextExec(Date nextExec) {
        if (nextExec != null) {
            this.nextExec = new Date(nextExec.getTime());
        }
    }

    public Date getStartDate() {
        return startDate == null
                ? null
                : new Date(startDate.getTime());
    }

    public void setStartDate(Date startDate) {
        if (startDate != null) {
            this.startDate = new Date(startDate.getTime());
        }
    }

    public Date getEndDate() {
        return endDate == null
                ? null
                : new Date(endDate.getTime());
    }

    public void setEndDate(Date endDate) {
        if (endDate != null) {
            this.endDate = new Date(endDate.getTime());
        }
    }
}
