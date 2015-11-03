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

import java.util.Arrays;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaDelete extends AbstractSchemaCommand {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaDelete.class);

    private static final String DELETE_HELP_MESSAGE = "schema --delete {SCHEMA-TYPE} {SCHEMA-KEY}\n"
            + "   Schema type: PLAIN / DERIVED / VIRTUAL";

    private final Input input;

    public SchemaDelete(final Input input) {
        this.input = input;
    }

    public void delete() {
        if (input.parameterNumber() >= 2) {
            final String[] parameters = Arrays.copyOfRange(input.getParameters(), 1, input.parameterNumber());
            try {
                for (final String parameter : parameters) {
                    schemaSyncopeOperations.delete(input.firstParameter(), parameter);
                    schemaResultManager.deletedMessage("Schema", parameter);
                }
            } catch (final SyncopeClientException | WebServiceException ex) {
                LOG.error("Error deleting schema", ex);
                if (ex.getMessage().startsWith("NotFound")) {
                    schemaResultManager.notFoundError("Schema", parameters[0]);
                } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                    schemaResultManager.genericError("You cannot delete schema " + parameters[0]);
                } else {
                    schemaResultManager.genericError(ex.getMessage());
                }
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error deleting schema", ex);
                schemaResultManager.typeNotValidError(
                        "schema", input.firstParameter(), CommandUtils.fromEnumToArray(SchemaType.class));
            }
        } else {
            schemaResultManager.commandOptionError(DELETE_HELP_MESSAGE);
        }
    }
}
