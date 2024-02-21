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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.common.validation.ReportCheck;

@Entity
@Table(name = JPAReport.TABLE)
@ReportCheck
public class JPAReport extends AbstractGeneratedKeyEntity implements Report {

    private static final long serialVersionUID = -587652654964285834L;

    public static final String TABLE = "Report";

    @OneToOne(optional = false)
    private JPAImplementation jobDelegate;

    @Column(unique = true, nullable = false)
    private String name;

    @NotNull
    private String mimeType;

    @NotNull
    private String fileExt;

    private String cronExpression;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "report")
    private List<JPAReportExec> executions = new ArrayList<>();

    @NotNull
    private Boolean active = true;

    @Override
    public Implementation getJobDelegate() {
        return jobDelegate;
    }

    @Override
    public void setJobDelegate(final Implementation jobDelegate) {
        checkType(jobDelegate, JPAImplementation.class);
        checkImplementationType(jobDelegate, IdRepoImplementationType.REPORT_DELEGATE);
        this.jobDelegate = (JPAImplementation) jobDelegate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getFileExt() {
        return fileExt;
    }

    @Override
    public void setFileExt(final String fileExt) {
        this.fileExt = fileExt;
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
    public boolean add(final ReportExec exec) {
        checkType(exec, JPAReportExec.class);
        return exec != null && !executions.contains((JPAReportExec) exec) && executions.add((JPAReportExec) exec);
    }

    @Override
    public List<? extends ReportExec> getExecs() {
        return executions;
    }
}
