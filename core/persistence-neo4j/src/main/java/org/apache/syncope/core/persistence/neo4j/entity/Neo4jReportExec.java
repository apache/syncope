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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jReportExec.NODE)
public class Neo4jReportExec extends AbstractExec implements ReportExec {

    private static final long serialVersionUID = -6178274296037547769L;

    public static final String NODE = "ReportExec";

    /**
     * The referred report.
     */
    @NotNull
    @Relationship(type = Neo4jReport.REPORT_EXEC_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jReport report;

    /**
     * Report execution result.
     */
    private byte[] execResult;

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public void setReport(final Report report) {
        checkType(report, Neo4jReport.class);
        this.report = (Neo4jReport) report;
    }

    @Override
    public byte[] getExecResult() {
        return execResult;
    }

    @Override
    public void setExecResult(final byte[] execResult) {
        this.execResult = ArrayUtils.clone(execResult);
    }
}
