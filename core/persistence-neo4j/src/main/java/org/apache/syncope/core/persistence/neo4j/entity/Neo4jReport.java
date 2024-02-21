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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.common.validation.ReportCheck;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jReport.NODE)
@ReportCheck
public class Neo4jReport extends AbstractGeneratedKeyNode implements Report {

    private static final long serialVersionUID = -587652654964285834L;

    public static final String NODE = "Report";

    public static final String REPORT_EXEC_REL = "REPORT_EXEC";

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation jobDelegate;

    @NotNull
    private String name;

    @NotNull
    private String mimeType;

    @NotNull
    private String fileExt;

    private String cronExpression;

    @Relationship(type = REPORT_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jReportExec> executions = new ArrayList<>();

    @NotNull
    private Boolean active = true;

    @Override
    public Implementation getJobDelegate() {
        return jobDelegate;
    }

    @Override
    public void setJobDelegate(final Implementation jobDelegate) {
        checkType(jobDelegate, Neo4jImplementation.class);
        checkImplementationType(jobDelegate, IdRepoImplementationType.REPORT_DELEGATE);
        this.jobDelegate = (Neo4jImplementation) jobDelegate;
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
        checkType(exec, Neo4jReportExec.class);
        return exec != null && !executions.contains((Neo4jReportExec) exec) && executions.add((Neo4jReportExec) exec);
    }

    @Override
    public List<? extends ReportExec> getExecs() {
        return executions;
    }
}
