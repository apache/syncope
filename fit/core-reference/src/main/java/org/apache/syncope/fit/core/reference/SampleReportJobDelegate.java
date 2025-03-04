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
package org.apache.syncope.fit.core.reference;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.report.ReportConfClass;
import org.apache.syncope.core.provisioning.java.job.report.AbstractReportJobDelegate;
import org.springframework.http.MediaType;

@ReportConfClass(SampleReportConf.class)
public class SampleReportJobDelegate extends AbstractReportJobDelegate {

    private SampleReportConf sampleReportConf;

    private void generateSamplePdfContent(final OutputStream os) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage myPage = new PDPage();
            doc.addPage(myPage);

            try (PDPageContentStream cont = new PDPageContentStream(doc, myPage)) {
                cont.beginText();

                cont.setFont(new PDType1Font(FontName.HELVETICA_BOLD), 12);
                cont.setLeading(14.5f);

                cont.newLineAtOffset(25, 700);
                String line1 = "World War II (often abbreviated to WWII or WW2), "
                        + "also known as the Second World War,";
                cont.showText(line1);

                cont.newLine();

                String line2 = "was a global war that lasted from 1939 to 1945, "
                        + "although related conflicts began earlier.";
                cont.showText(line2);
                cont.newLine();

                String line3 = "It involved the vast majority of the world's "
                        + "countries—including all of the great powers—";
                cont.showText(line3);
                cont.newLine();

                String line4 = "eventually forming two opposing military "
                        + "alliances: the Allies and the Axis.";
                cont.showText(line4);
                cont.newLine();

                cont.endText();
            }

            doc.save(os);
        }
    }

    private void generateSampleCsvContent(final OutputStream os) throws IOException {
        CsvMapper mapper = new CsvMapper();
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

        CsvSchema schema = CsvSchema.builder().
                setUseHeader(true).
                addColumn("stringValue").
                addColumn("intValue").
                addColumn("longValue").
                addColumn("floatValue").
                addColumn("doubleValue").
                addColumn("booleanValue").
                addColumn("listValues").
                build();

        ObjectWriter writer = mapper.writerFor(SampleReportConf.class).with(schema);

        writer.writeValues(os).write(sampleReportConf);
    }

    @Override
    public void setConf(final ReportConf conf) {
        if (conf instanceof final SampleReportConf reportConf) {
            sampleReportConf = reportConf;
        } else {
            throw new IllegalArgumentException("Expected " + SampleReportConf.class.getName() + ", got " + conf);
        }
    }

    @Override
    protected String doExecute(
            final boolean dryRun,
            final OutputStream os,
            final String executor,
            final JobExecutionContext context) throws JobExecutionException {

        if (!dryRun) {
            try {
                switch (report.getMimeType()) {
                    case MediaType.APPLICATION_PDF_VALUE:
                        generateSamplePdfContent(os);
                        break;

                    case "text/csv":
                        generateSampleCsvContent(os);
                        break;

                    default:
                }
            } catch (IOException e) {
                throw new JobExecutionException(e);
            }
        }

        return (dryRun
                ? "DRY "
                : "") + "RUNNING";
    }
}
