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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportDataBinderTest extends AbstractDataBinderTest {

    private static final String REPORT_KEY = "0062ea9c-924d-4ecf-9961-4492a8cc6d1b";

    @Autowired
    private ReportDataBinder binder;

    @Autowired
    private ReportDAO reportDAO;

    @Test
    void get() {
        ReportTO report = binder.getReportTO(reportDAO.findById(REPORT_KEY).orElseThrow());
        assertEquals(REPORT_KEY, report.getKey());
        assertEquals("test", report.getName());
        assertEquals("application/pdf", report.getMimeType());
        assertEquals("pdf", report.getFileExt());
        assertEquals("SampleReportJobDelegate", report.getJobDelegate());
        assertTrue(report.isActive());
        assertEquals("SUCCESS", report.getLatestExecStatus());
        assertEquals(1, report.getExecutions().size());
    }

    @Test
    void getExec() {
        Report report = reportDAO.findById(REPORT_KEY).orElseThrow();
        ExecTO exec = binder.getExecTO(report.getExecs().getFirst());
        assertEquals(report.getExecs().getFirst().getKey(), exec.getKey());
        assertEquals(REPORT_KEY, exec.getRefKey());
        assertEquals(binder.buildRefDesc(report), exec.getRefDesc());
        assertEquals("SUCCESS", exec.getStatus());
    }

    @Test
    void getReport() {
        Report report = reportDAO.findById(REPORT_KEY).orElseThrow();
        ReportTO reportTO = binder.getReportTO(report);

        reportTO.setName("updated");
        reportTO.setMimeType("text/plain");
        reportTO.setFileExt("txt");
        reportTO.setCronExpression("0 0/10 * * * ?");
        reportTO.setActive(false);

        binder.getReport(report, reportTO);

        assertEquals("updated", report.getName());
        assertEquals("text/plain", report.getMimeType());
        assertEquals("txt", report.getFileExt());
        assertEquals("0 0/10 * * * ?", report.getCronExpression());
        assertEquals("SampleReportJobDelegate", report.getJobDelegate().getKey());
        assertFalse(report.isActive());
    }

    @Test
    void invalidJobDelegate() {
        Report report = reportDAO.findById(REPORT_KEY).orElseThrow();
        ReportTO reportTO = binder.getReportTO(report);

        reportTO.setJobDelegate("not-existing");
        assertThrows(SyncopeClientException.class, () -> binder.getReport(report, reportTO));

        reportTO.setJobDelegate("EmailAddressValidator");
        assertThrows(SyncopeClientException.class, () -> binder.getReport(report, reportTO));
    }
}
