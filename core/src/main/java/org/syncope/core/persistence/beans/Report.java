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
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import org.syncope.client.report.ReportletConf;
import org.syncope.core.persistence.validation.entity.ReportCheck;
import org.syncope.client.util.XMLSerializer;

@Entity
@ReportCheck
public class Report extends AbstractBaseBean {

    private static final long serialVersionUID = -587652654964285834L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Lob
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "reportletConfs", columnDefinition = "CLOB")
    private List<String> reportletConfs;

    private String cronExpression;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true,
    mappedBy = "report")
    private List<ReportExec> executions;

    public Report() {
        super();

        reportletConfs = new ArrayList<String>();
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

    public boolean addReportletConf(ReportletConf reportletConf) {
        if (reportletConf == null) {
            return false;
        }

        String xmlReportlet = XMLSerializer.serialize(reportletConf);
        return !reportletConfs.contains(xmlReportlet)
                && reportletConfs.add(xmlReportlet);
    }

    public boolean removeReportletConf(ReportletConf reportletConf) {
        if (reportletConf == null) {
            return false;
        }

        String xmlReportlet = XMLSerializer.serialize(reportletConf);
        return reportletConfs.remove(xmlReportlet);
    }

    public List<ReportletConf> getReportletConfs() {
        List<ReportletConf> result =
                new ArrayList<ReportletConf>(reportletConfs.size());
        for (String xmlReportletConf : reportletConfs) {
            result.add(
                    XMLSerializer.<ReportletConf>deserialize(xmlReportletConf));
        }

        return result;
    }

    public void setReportlets(List<ReportletConf> reportletConfs) {
        this.reportletConfs.clear();
        if (reportletConfs != null && !reportletConfs.isEmpty()) {
            for (ReportletConf reportlet : reportletConfs) {
                addReportletConf(reportlet);
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
