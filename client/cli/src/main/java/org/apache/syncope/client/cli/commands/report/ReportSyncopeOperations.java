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
package org.apache.syncope.client.cli.commands.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.util.XMLUtils;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.xml.sax.SAXException;

public class ReportSyncopeOperations {

    private final ReportService reportService = SyncopeServices.get(ReportService.class);

    public ReportTO read(final String reportKey) {
        return reportService.read(reportKey);
    }

    public List<JobTO> listJobs() {
        return reportService.listJobs();
    }

    public List<ReportTO> list() {
        return reportService.list();
    }

    public String exportExecutionResult(final String executionKey, final String reportExecExportFormat)
            throws TransformerException, SAXException, IOException, ParserConfigurationException {

        ReportExecExportFormat format = ReportExecExportFormat.valueOf(reportExecExportFormat);
        SequenceInputStream report = (SequenceInputStream) reportService.exportExecutionResult(executionKey, format).
                getEntity();

        String fileName = "export_" + executionKey;
        OutputStream os = null;
        switch (format) {
            case XML:
                fileName += ".xml";
                XMLUtils.createXMLFile(report, fileName);
                break;

            case CSV:
                fileName += ".csv";
                os = Files.newOutputStream(Paths.get(fileName));
                IOUtils.copyAndCloseInput(report, os);
                break;

            case PDF:
                fileName += ".pdf";
                os = Files.newOutputStream(Paths.get(fileName));
                IOUtils.copyAndCloseInput(report, os);
                break;

            case HTML:
                fileName += ".html";
                os = Files.newOutputStream(Paths.get(fileName));
                IOUtils.copyAndCloseInput(report, os);
                break;

            case RTF:
                fileName += ".rtf";
                os = Files.newOutputStream(Paths.get(fileName));
                IOUtils.copyAndCloseInput(report, os);
                break;

            default:
                return format + " not supported";
        }
        if (os != null) {
            os.close();
        }

        return fileName;
    }

    public void execute(final String reportKey) {
        reportService.execute(new ExecuteQuery.Builder().key(reportKey).build());
    }

    public void deleteExecution(final String executionKey) {
        reportService.deleteExecution(executionKey);
    }

    public void delete(final String reportKey) {
        reportService.delete(reportKey);
    }
}
