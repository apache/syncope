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
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.ArrayUtils;
import org.apache.syncope.common.types.ReportExecStatus;

@Entity
public class ReportExec extends AbstractExec {

    private static final long serialVersionUID = -6178274296037547769L;

    @Id
    private Long id;

    /**
     * The referred report.
     */
    @ManyToOne(optional = false)
    private Report report;

    /**
     * Report execution result, stored as an XML stream.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private Byte[] execResult;

    public Long getId() {
        return id;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public byte[] getExecResult() {
        return execResult == null ? null : ArrayUtils.toPrimitive(execResult);
    }

    public void setExecResult(byte[] execResult) {
        this.execResult = execResult == null ? null : ArrayUtils.toObject(execResult);
    }

    public void setStatus(ReportExecStatus status) {
        super.setStatus(status.name());
    }
}
