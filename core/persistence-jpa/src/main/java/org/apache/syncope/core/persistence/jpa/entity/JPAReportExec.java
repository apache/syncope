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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.ReportExecStatus;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;

import java.util.Optional;

@Entity
@Table(name = JPAReportExec.TABLE)
public class JPAReportExec extends AbstractExec implements ReportExec {

    private static final long serialVersionUID = -6178274296037547769L;

    public static final String TABLE = "ReportExec";

    /**
     * The referred report.
     */
    @ManyToOne(optional = false)
    private JPAReport report;

    /**
     * Report execution result, stored as an XML stream.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private Byte[] execResult;

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public void setReport(final Report report) {
        checkType(report, JPAReport.class);
        this.report = (JPAReport) report;
    }

    @Override
    public byte[] getExecResult() {
        return Optional.ofNullable(execResult).map(ArrayUtils::toPrimitive).orElse(null);
    }

    @Override
    public void setExecResult(final byte[] execResult) {
        this.execResult = Optional.ofNullable(execResult).map(ArrayUtils::toObject).orElse(null);
    }

    @Override
    public void setStatus(final ReportExecStatus status) {
        super.setStatus(status.name());
    }
}
