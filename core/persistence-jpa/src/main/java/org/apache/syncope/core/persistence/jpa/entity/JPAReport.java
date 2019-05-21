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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Reportlet",
            joinColumns =
            @JoinColumn(name = "report_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "report_id", "implementation_id" }))
    private List<JPAImplementation> reportlets = new ArrayList<>();

    private String cronExpression;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "report")
    private List<JPAReportExec> executions = new ArrayList<>();

    @NotNull
    private Boolean active = true;

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
    public boolean add(final Implementation reportlet) {
        checkType(reportlet, JPAImplementation.class);
        checkImplementationType(reportlet, IdRepoImplementationType.REPORTLET);
        return reportlets.contains((JPAImplementation) reportlet) || reportlets.add((JPAImplementation) reportlet);
    }

    @Override
    public List<? extends Implementation> getReportlets() {
        return reportlets;
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
        return active;
    }

    @Override
    public void setActive(final boolean active) {
        this.active = active;
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
