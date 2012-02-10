/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.report.UserReportletConf;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.Report;

@Transactional
public class ReportTest extends AbstractTest {

    @Autowired
    private ReportDAO reportDAO;

    @Test
    public void find() {
        Report report = reportDAO.find(1L);
        assertNotNull(report);

        report = reportDAO.find(10L);
        assertNull(report);
    }

    @Test
    public void findAll() {
        List<Report> reports = reportDAO.findAll();
        assertNotNull(reports);
        assertEquals(1, reports.size());
    }

    @Test
    public void save() {
        int beforeCount = reportDAO.count();

        Report report = new Report();
        report.setName("new report");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        report = reportDAO.save(report);
        assertNotNull(report);
        assertNotNull(report.getId());

        int afterCount = reportDAO.count();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void delete() {
        Report report = reportDAO.find(1L);
        assertNotNull(report);

        reportDAO.delete(1L);

        report = reportDAO.find(1L);
        assertNull(report);
    }
}
