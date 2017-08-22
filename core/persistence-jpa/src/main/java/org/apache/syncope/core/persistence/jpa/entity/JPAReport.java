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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.apache.syncope.core.persistence.jpa.validation.entity.ReportCheck;

@Entity
@Table(name = JPAReport.TABLE)
@ReportCheck
public class JPAReport extends AbstractGeneratedKeyEntity implements Report {

    private static final long serialVersionUID = -587652654964285834L;

    public static final String TABLE = "Report";

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "report")
    private List<JPAReportletConfInstance> reportletConfs = new ArrayList<>();

    private String cronExpression;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "report")
    private List<JPAReportExec> executions = new ArrayList<>();

    @NotNull
    @Basic
    @Min(0)
    @Max(1)
    private Integer active;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "template_id")
    private JPAReportTemplate template;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean add(final ReportExec exec) {
        checkType(exec, JPAReportExec.class);
        return exec != null && !executions.contains((JPAReportExec) exec) && executions.add((JPAReportExec) exec);
    }

    @Override
    public List<? extends ReportExec> getExecs() {
        return executions;
    }

    @Override
    public boolean add(final ReportletConf reportletConf) {
        if (reportletConf == null) {
            return false;
        }

        JPAReportletConfInstance instance = new JPAReportletConfInstance();
        instance.setReport(this);
        instance.setInstance(reportletConf);

        return reportletConfs.add(instance);
    }

    @Override
    public void removeAllReportletConfs() {
        reportletConfs.clear();
    }

    @Override
    public List<ReportletConf> getReportletConfs() {
        return reportletConfs.stream().map(input -> input.getInstance()).collect(Collectors.toList());
    }

    @Override
    public String getCronExpression() {
        return cronExpression;
    }

    @Override
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Override
    public boolean isActive() {
        return isBooleanAsInteger(active);
    }

    @Override
    public void setActive(final boolean active) {
        this.active = getBooleanAsInteger(active);
    }

    @Override
    public ReportTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        checkType(template, JPAReportTemplate.class);
        this.template = (JPAReportTemplate) template;
    }
}
