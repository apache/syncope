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

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRead extends AbstractConfigurationCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationRead.class);

    private static final String READ_HELP_MESSAGE = "configuration --read {CONF-NAME} {CONF-NAME} [...]";

    private final Input input;

    public ConfigurationRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            final List<AttrTO> attrList = new ArrayList<>();
            boolean failed = false;
            for (final String parameter : input.getParameters()) {
                try {
                    attrList.add(configurationSyncopeOperations.get(parameter));
                } catch (final SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error reading configuration", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        configurationResultManager.notFoundError("Configuration", parameter);
                    } else {
                        configurationResultManager.genericError(ex.getMessage());
                    }
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                configurationResultManager.fromGet(attrList);
            }
        } else {
            configurationResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
