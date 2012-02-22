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
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;

import java.util.Date;
import javax.persistence.EntityExistsException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.beans.ReportExec;
import org.syncope.core.persistence.dao.ReportDAO;
import org.syncope.core.persistence.dao.ReportExecDAO;
import org.syncope.types.ReportExecStatus;

@Transactional
public class ReportTest extends AbstractTest {

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Test
    public void find() {
        Report report = reportDAO.find(1L);
        assertNotNull(report);

        assertNotNull(report.getExecs());
        assertFalse(report.getExecs().isEmpty());
        assertEquals(1, report.getExecs().size());
    }

    @Test(expected = EntityExistsException.class)
    public void saveWithExistingName() {
        Report report = reportDAO.find(1L);
        assertNotNull(report);

        String name = report.getName();

        report = new Report();
        report.setName(name);

        reportDAO.save(report);
        reportDAO.flush();
    }

    @Test
    public void save() {
        Report report = reportDAO.find(1L);
        assertNotNull(report);
        assertEquals(1, report.getExecs().size());

        ReportExec reportExec = new ReportExec();
        reportExec.setReport(report);
        reportExec.setStartDate(new Date());
        reportExec.setEndDate(new Date());
        reportExec.setStatus(ReportExecStatus.SUCCESS);

        report.addExec(reportExec);

        reportExec = reportExecDAO.save(reportExec);
        assertNotNull(reportExec);
        assertNotNull(reportExec.getId());

        reportExecDAO.flush();

        report = reportDAO.find(1L);
        assertNotNull(report);
        assertEquals(2, report.getExecs().size());
    }

    @Test
    public void deleteReport() {
        reportDAO.delete(1L);

        reportDAO.flush();

        assertNull(reportDAO.find(1L));
        assertNull(reportExecDAO.find(1L));
    }

    @Test
    public void deleteReportExecution() {
        ReportExec execution = reportExecDAO.find(1L);
        int executionNumber = execution.getReport().getExecs().size();

        reportExecDAO.delete(1L);

        reportExecDAO.flush();

        assertNull(reportExecDAO.find(1L));

        Report report = reportDAO.find(1L);
        assertEquals(report.getExecs().size(), executionNumber - 1);
    }
}
