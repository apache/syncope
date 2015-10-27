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
import java.io.SequenceInputStream;
import java.util.Arrays;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.client.cli.util.XMLUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.xml.sax.SAXException;

public class ReportExportExecution extends AbstractReportCommand {

    private static final String EXPORT_EXECUTION_HELP_MESSAGE
            = "report --export-execution-result {EXECUTION-ID} {EXECUTION-ID} [...] {FORMAT}\n"
            + "          Format: CSV / HTML / PDF / XML / RTF";

    private final Input input;

    public ReportExportExecution(final Input input) {
        this.input = input;
    }

    public void export() {
        if (input.parameterNumber() >= 2) {
            final String[] parameters = Arrays.copyOf(input.getParameters(), input.parameterNumber() - 1);
            for (final String parameter : parameters) {
                try {
                    final ReportExecExportFormat format = ReportExecExportFormat.valueOf(input.lastParameter());
                    final SequenceInputStream report = (SequenceInputStream) reportSyncopeOperations.
                            exportExecutionResult(parameter, format).getEntity();
                    switch (format) {
                        case XML:
                            final String xmlFinalName = "export_" + parameter + ".xml";
                            XMLUtils.createXMLFile(report, xmlFinalName);
                            reportResultManager.generic(xmlFinalName + " successfully created");
                            break;
                        case CSV:
                            reportResultManager.generic(format + " doesn't supported");
                            break;
                        case PDF:
                            reportResultManager.generic(format + " doesn't supported");
                            break;
                        case HTML:
                            reportResultManager.generic(format + " doesn't supported");
                            break;
                        case RTF:
                            reportResultManager.generic(format + " doesn't supported");
                            break;
                        default:
                            reportResultManager.generic(format + " doesn't supported");
                            break;
                    }
                } catch (final WebServiceException | SyncopeClientException ex) {
                    if (ex.getMessage().startsWith("NotFound")) {
                        reportResultManager.notFoundError("Report", parameter);
                    } else {
                        reportResultManager.generic(ex.getMessage());
                    }
                } catch (final NumberFormatException ex) {
                    reportResultManager.numberFormatException("report", parameter);
                } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
                    reportResultManager.generic(" - Error creating " + "export_" + parameter + " " + e.getMessage());
                } catch (final IllegalArgumentException ex) {
                    reportResultManager.typeNotValidError(
                            "format", input.firstParameter(),
                            CommandUtils.fromEnumToArray(ReportExecExportFormat.class));
                }
                break;
            }
        } else {
            reportResultManager.commandOptionError(EXPORT_EXECUTION_HELP_MESSAGE);
        }
    }

}
