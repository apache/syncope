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
package org.apache.syncope.client.cli.commands.self;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public class WorkflowExportDiagram extends AbstractWorkflowCommand {

    private static final String EXPORT_HELP_MESSAGE = "workflow --export-diagram {ANY-TYPE-KIND}\n";

    private final Input input;

    public WorkflowExportDiagram(final Input input) {
        this.input = input;
    }

    public void export() {
        if (input.parameterNumber() == 1) {
            try {
                final Response response = workflowSyncopeOperations.exportDiagram(input.firstParameter());
                final byte[] diagram = IOUtils.readBytesFromStream((InputStream) response.getEntity());
                try (final FileOutputStream fos = new FileOutputStream("/tmp/diagram.png")) {
                    fos.write(diagram);
                    fos.close();
                }
                workflowResultManager.genericMessage("Diagram created: /tmp/diagram.png");
            } catch (final SyncopeClientException | WebServiceException ex) {
                if (ex.getMessage().startsWith("NotFound")) {
                    workflowResultManager.notFoundError("Workflow", input.firstParameter());
                } else {
                    workflowResultManager.genericError(ex.getMessage());
                }
            } catch (final IllegalArgumentException ex) {
                workflowResultManager.typeNotValidError(
                        "workflow", input.firstParameter(), CommandUtils.fromEnumToArray(AnyTypeKind.class));
            } catch (final IOException ex) {
                workflowResultManager.genericError(ex.getMessage());
            }
        } else {
            workflowResultManager.commandOptionError(EXPORT_HELP_MESSAGE);
        }
    }
}
