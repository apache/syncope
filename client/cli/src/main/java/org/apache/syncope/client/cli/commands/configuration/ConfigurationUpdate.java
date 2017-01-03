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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationUpdate extends AbstractConfigurationCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUpdate.class);

    private static final String UPDATE_HELP_MESSAGE =
            "configuration --update {CONF-NAME}={CONF-VALUE} {CONF-NAME}={CONF-VALUE} [...]";

    private final Input input;

    public ConfigurationUpdate(final Input input) {
        this.input = input;
    }

    public void update() {
        if (input.parameterNumber() >= 1) {
            List<AttrTO> attrList = new ArrayList<>();
            boolean failed = false;
            for (String parameter : input.getParameters()) {
                Pair<String, String> pairParameter = Input.toPairParameter(parameter);
                try {
                    AttrTO attrTO = configurationSyncopeOperations.get(pairParameter.getKey());
                    attrTO.getValues().clear();
                    attrTO.getValues().add(pairParameter.getValue());
                    configurationSyncopeOperations.set(attrTO);
                    attrList.add(attrTO);
                } catch (IllegalArgumentException ex) {
                    LOG.error("Error updating configuration", ex);
                    configurationResultManager.genericError(ex.getMessage());
                    configurationResultManager.genericError(UPDATE_HELP_MESSAGE);
                    failed = true;
                    break;
                } catch (SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error updating configuration", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        configurationResultManager.notFoundError("Configuration", pairParameter.getKey());
                    } else if (ex.getMessage().startsWith("InvalidValues")) {
                        configurationResultManager.genericError(
                                pairParameter.getValue() + " is not a valid value for " + pairParameter.getKey());
                    } else {
                        configurationResultManager.genericError(ex.getMessage());
                    }
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                configurationResultManager.fromUpdate(attrList);
            }
        } else {
            configurationResultManager.commandOptionError(UPDATE_HELP_MESSAGE);
        }
    }
}
