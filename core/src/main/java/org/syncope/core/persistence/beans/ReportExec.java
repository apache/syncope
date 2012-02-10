/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.persistence.beans;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.syncope.types.ReportExecStatus;

@Entity
public class ReportExec extends AbstractExec {

    private static final long serialVersionUID = -6178274296037547769L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The referred report.
     */
    @ManyToOne(optional = false)
    private Report report;

    /**
     * Report execution result, stored as an XML stream.
     */
    @Basic(fetch = FetchType.LAZY)
    private byte[] execResult;

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
        return execResult;
    }

    public void setExecResult(byte[] execResult) {
        this.execResult = execResult;
    }

    public void setStatus(ReportExecStatus status) {
        super.setStatus(status.name());
    }
}
