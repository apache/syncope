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
package org.apache.syncope.client.cli.commands.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaRead extends AbstractSchemaCommand {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaRead.class);

    private static final String READ_HELP_MESSAGE = "schema --read {SCHEMA-TYPE} {SCHEMA-KEY}\n"
            + "   Schema type: PLAIN / DERIVED / VIRTUAL";

    private final Input input;

    public SchemaRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 2) {
            final String[] parameters = Arrays.copyOfRange(input.getParameters(), 1, input.parameterNumber());
            try {
                final List<AbstractSchemaTO> schemaTOs = new ArrayList<>();
                for (final String parameter : parameters) {
                    schemaTOs.add(schemaSyncopeOperations.read(input.firstParameter(), parameter));
                }
                schemaResultManager.toView(input.firstParameter(), schemaTOs);
            } catch (final SyncopeClientException | WebServiceException ex) {
                LOG.error("Error reading schema", ex);
                if (ex.getMessage().startsWith("NotFound")) {
                    schemaResultManager.notFoundError("Schema", parameters[0]);
                } else {
                    schemaResultManager.genericError(ex.getMessage());
                }
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error reading schema", ex);
                schemaResultManager.typeNotValidError(
                        "schema", input.firstParameter(), CommandUtils.fromEnumToArray(SchemaType.class));
            }
        } else {
            schemaResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
