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
package org.apache.syncope.client.cli.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.client.cli.messages.TwoColumnTable;
import org.apache.syncope.client.cli.util.XMLUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.common.rest.api.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Command(name = "configuration")
public class ConfigurationCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationCommand.class);

    private static final String EXPORT_FILE_NAME = "/content.xml";

    private static final String HELP_MESSAGE = "Usage: configuration [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --get \n"
            + "    --read \n"
            + "       Syntax: --read {CONF-NAME} {CONF-NAME} [...] \n"
            + "    --update \n"
            + "       Syntax: --update {CONF-NAME}={CONF-VALUE} {CONF-NAME}={CONF-VALUE} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {CONF-NAME} {CONF-NAME} [...]\n"
            + "    --export \n"
            + "       Syntax: --export {WHERE-DIR}";

    @Override
    public void execute(final Input input) {
        LOG.debug("Logger service successfully created");
        LOG.debug("Option: {}", input.getOption());
        LOG.debug("Parameters:");
        for (final String parameter : input.getParameters()) {
            LOG.debug("   > " + parameter);
        }

        final String[] parameters = input.getParameters();

        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        final ConfigurationService configurationService = SyncopeServices.get(ConfigurationService.class);
        switch (Options.fromName(input.getOption())) {
            case GET:
                try {
                    final ConfTO confTO = configurationService.list();
                    toTable("Syncope configuration", "attribute", "value", confTO.getPlainAttrs());
                } catch (final Exception ex) {
                    Messages.printMessage(ex.getMessage());
                    break;
                }
                break;
            case READ:
                final String readErrorMessage = "configuration --read {CONF-NAME} {CONF-NAME} [...]";
                if (parameters.length >= 1) {
                    final Set<AttrTO> attrList = new HashSet<>();
                    boolean failed = false;
                    for (final String parameter : parameters) {
                        try {
                            attrList.add(configurationService.get(parameter));
                        } catch (final SyncopeClientException | WebServiceException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Logger", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                            failed = true;
                            break;
                        }
                    }
                    if (!failed) {
                        toTable("Read result", "attribute", "value", attrList);
                    }
                } else {
                    Messages.printCommandOptionMessage(readErrorMessage);
                }
                break;
            case UPDATE:
                final String updateErrorMessage
                        = "configuration --update {CONF-NAME}={CONF-VALUE} {CONF-NAME}={CONF-VALUE} [...]";
                if (parameters.length >= 1) {
                    Input.PairParameter pairParameter = null;
                    AttrTO attrTO;
                    final Set<AttrTO> attrList = new HashSet<>();
                    boolean failed = false;
                    for (final String parameter : parameters) {
                        try {
                            pairParameter = input.toPairParameter(parameter);
                            attrTO = configurationService.get(pairParameter.getKey());
                            attrTO.getValues().clear();
                            attrTO.getValues().add(pairParameter.getValue());
                            configurationService.set(attrTO);
                            attrList.add(attrTO);
                        } catch (final IllegalArgumentException ex) {
                            Messages.printMessage(ex.getMessage(), updateErrorMessage);
                            failed = true;
                            break;
                        } catch (final SyncopeClientException | WebServiceException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Configuration", pairParameter.getKey());
                            } else if (ex.getMessage().startsWith("InvalidValues")) {
                                Messages.printMessage(
                                        pairParameter.getValue() + " is not a valid value for "
                                        + pairParameter.getKey());
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                            failed = true;
                            break;
                        }
                    }
                    if (!failed) {
                        toTable("updated attribute", "attribute", "new value", attrList);
                    }
                } else {
                    Messages.printCommandOptionMessage(updateErrorMessage);
                }
                break;
            case DELETE:
                final String deleteErrorMessage = "configuration --delete {CONF-NAME} {CONF-NAME} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            configurationService.delete(parameter);
                            Messages.printDeletedMessage("Configuration", parameter);
                        } catch (final SyncopeClientException | WebServiceException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Configuration", parameter);
                            } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                                Messages.printMessage("You cannot delete configuration", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                            break;
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(deleteErrorMessage);
                }
                break;
            case EXPORT:
                final String exportErrorMessage = "configuration --export {WHERE-DIR}";
                if (parameters.length == 1) {
                    try {
                        XMLUtils.createXMLFile((SequenceInputStream) configurationService.export().getEntity(),
                                parameters[0] + EXPORT_FILE_NAME);
                        System.out.println(" - " + parameters[0] + EXPORT_FILE_NAME + " successfully created");
                    } catch (final IOException ex) {
                        Messages.printMessage(ex.getMessage());
                    } catch (ParserConfigurationException | SAXException | TransformerConfigurationException ex) {
                        LOG.error("Error creating content.xml file in {} directory", parameters[0], ex);
                        Messages.printMessage(
                                "Error creating " + parameters[0] + EXPORT_FILE_NAME + " " + ex.getMessage());
                        break;
                    } catch (final TransformerException ex) {
                        LOG.error("Error creating content.xml file in {} directory", parameters[0], ex);
                        if (ex.getCause() instanceof FileNotFoundException) {
                            Messages.printMessage("Permission denied on " + parameters[0]);
                        } else {
                            Messages.printMessage(
                                    "Error creating " + parameters[0] + EXPORT_FILE_NAME + " " + ex.getMessage());
                        }
                        break;
                    } catch (final SyncopeClientException ex) {
                        LOG.error("Error calling configuration service", ex);
                        Messages.printMessage("Error calling configuration service " + ex.getMessage());
                        break;
                    }
                } else {
                    Messages.printCommandOptionMessage(exportErrorMessage);
                }
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                Messages.printDefaultMessage(input.getOption(), HELP_MESSAGE);
                break;
        }
    }

    private void toTable(final String tableTitle,
            final String firstHeader,
            final String seconHeader,
            final Set<AttrTO> attrList) {
        int maxFirstColumnLenght = 0;
        int maxSecondColumnLenght = 0;
        final Map<String, String> attributes = new HashMap<>();
        for (final AttrTO attrTO : attrList) {
            String value = attrTO.getValues().toString();
            value = value.substring(0, value.length() - 1);
            value = value.substring(1, value.length());
            attributes.put(attrTO.getSchema(), value);
            if (attrTO.getSchema().length() > maxFirstColumnLenght) {
                maxFirstColumnLenght = attrTO.getSchema().length();
            }

            if (value.length() > maxSecondColumnLenght) {
                maxSecondColumnLenght = attrTO.getSchema().length();
            }
        }
        final TwoColumnTable loggerTableResult = new TwoColumnTable(
                tableTitle,
                firstHeader, maxFirstColumnLenght,
                seconHeader, maxSecondColumnLenght);
        loggerTableResult.printTable(attributes);
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum Options {

        HELP("--help"),
        GET("--get"),
        READ("--read"),
        UPDATE("--update"),
        DELETE("--delete"),
        EXPORT("--export");

        private final String optionName;

        private Options(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static Options fromName(final String name) {
            Options optionToReturn = HELP;
            for (final Options option : Options.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final Options value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
