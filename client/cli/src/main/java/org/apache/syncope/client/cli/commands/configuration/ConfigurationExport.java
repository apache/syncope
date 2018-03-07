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
package org.apache.syncope.client.cli.commands.configuration;

import java.io.InputStream;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.XMLUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationExport extends AbstractConfigurationCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationExport.class);

    private static final String EXPORT_HELP_MESSAGE = "configuration --export {WHERE-DIR}";

    private static final String EXPORT_FILE_NAME = "/content.xml";

    private final Input input;

    public ConfigurationExport(final Input input) {
        this.input = input;
    }

    public void export() {
        if (input.parameterNumber() == 1) {
            try {
                XMLUtils.createXMLFile(
                        (InputStream) configurationSyncopeOperations.export().getEntity(),
                        input.firstParameter() + EXPORT_FILE_NAME);
                configurationResultManager.genericMessage(
                        input.firstParameter() + EXPORT_FILE_NAME + " successfully created");
            } catch (final SyncopeClientException ex) {
                LOG.error("Error exporting configuration", ex);
                configurationResultManager.genericError("Error calling configuration service " + ex.getMessage());
            } catch (final Exception ex) {
                LOG.error("Error exporting configuration", ex);
                configurationResultManager.genericError(ex.getMessage());
            }
        } else {
            configurationResultManager.commandOptionError(EXPORT_HELP_MESSAGE);
        }
    }
}
