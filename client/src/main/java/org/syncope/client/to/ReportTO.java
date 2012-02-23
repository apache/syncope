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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.syncope.client.AbstractBaseBean;
import org.syncope.client.report.ReportletConf;

public class ReportTO extends AbstractBaseBean {

    private static final long serialVersionUID = 5274568072084814410L;

    private long id;

    private String name;

    private List<ReportletConf> reportletConfs;

    private String cronExpression;

    private List<ReportExecTO> executions;

    private String latestExecStatus;

    private Date lastExec;

    private Date nextExec;

    public ReportTO() {
        super();

        reportletConfs = new ArrayList<ReportletConf>();
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

    public boolean addReportletConf(ReportletConf reportlet) {
        return this.reportletConfs.add(reportlet);
    }

    public boolean removeReportletConf(ReportletConf reportlet) {
        return this.reportletConfs.remove(reportlet);
    }

    public List<ReportletConf> getReportletConfs() {
        return reportletConfs;
    }

    public void setReportletConfs(List<ReportletConf> reportlets) {
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
        return lastExec == null ? null : new Date(lastExec.getTime());
    }

    public void setLastExec(Date lastExec) {
        if (lastExec != null) {
            this.lastExec = new Date(lastExec.getTime());
        }
    }

    public Date getNextExec() {
        return nextExec == null ? null : new Date(nextExec.getTime());
    }

    public void setNextExec(Date nextExec) {
        if (nextExec != null) {
            this.nextExec = new Date(nextExec.getTime());
        }
    }
}
