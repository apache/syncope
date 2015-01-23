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
package org.apache.syncope.cli.commands;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.File;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.cli.SyncopeServices;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.wrap.MailTemplate;
import org.apache.syncope.common.wrap.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Parameters(
        commandNames = "config",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope configuration service")
public class ConfigurationCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationCommand.class);

    private static final Class SYNCOPE_CONFIGURATION_CLASS = ConfigurationService.class;

    private static final String EXPORT_FILE_NAME = "/content.xml";

    private final String helpMessage = "Usage: config [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -r, --read \n"
            + "       Syntax: -r={CONF-NAME} \n"
            + "    -u, --update \n"
            + "       Syntax: {CONF-NAME}={CONF-VALUE} \n"
            + "    -c, --create \n"
            + "       Syntax: {CONF-NAME}={CONF-VALUE} \n"
            + "    -d, --delete \n"
            + "       Syntax: -d={CONF-NAME}"
            + "    -v, --validators \n"
            + "    -mt, --mail-templates \n"
            + "    -e, --export \n"
            + "       Syntax: -e={WHERE-DIR} \n";

    @Parameter(names = {"-l", "--list"})
    public boolean list = false;

    @Parameter(names = {"-r", "--read"})
    public String confNameToRead;

    @DynamicParameter(names = {"-u", "--update"})
    private final Map<String, String> updateConf = new HashMap<String, String>();

    @DynamicParameter(names = {"-c", "--create"})
    private final Map<String, String> createConf = new HashMap<String, String>();

    @Parameter(names = {"-d", "--delete"})
    public String confNameToDelete;

    @Parameter(names = {"-v", "--validators"})
    public boolean validators = false;

    @Parameter(names = {"-mt", "--mail-templates"})
    public boolean mailTemplates = false;

    @Parameter(names = {"-e", "--export"})
    public String export;

    @Override
    public void execute() {
        final ConfigurationService configurationService = ((ConfigurationService) SyncopeServices.
                get(SYNCOPE_CONFIGURATION_CLASS));

        LOG.debug("Logger service successfully created");

        if (help) {
            LOG.debug("- configuration help command");
            System.out.println(helpMessage);
        } else if (list) {
            LOG.debug("- configuration list command");
            try {
                final ConfTO confTO = configurationService.list();
                for (final AttributeTO attrTO : confTO.getAttrMap().values()) {
                    System.out.println(" - Conf " + attrTO.getSchema() + " has value(s) " + attrTO.getValues()
                            + " - readonly: " + attrTO.isReadonly());
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (StringUtils.isNotBlank(confNameToRead)) {
            LOG.debug("- configuration read {} command", confNameToRead);
            try {
                final AttributeTO attrTO = configurationService.read(confNameToRead);
                System.out.println(" - Conf " + attrTO.getSchema() + " has value(s) " + attrTO.getValues()
                        + " - readonly: " + attrTO.isReadonly());
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (!updateConf.isEmpty()) {
            LOG.debug("- configuration update command with params {}", updateConf);
            try {
                for (final Map.Entry<String, String> entrySet : updateConf.entrySet()) {
                    final AttributeTO attrTO = configurationService.read(entrySet.getKey());
                    attrTO.getValues().clear();
                    attrTO.getValues().add(entrySet.getValue());
                    configurationService.set(entrySet.getKey(), attrTO);
                    System.out.println(" - Conf " + attrTO.getSchema() + " has value(s) " + attrTO.getValues()
                            + " - readonly: " + attrTO.isReadonly());
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (!createConf.isEmpty()) {
            LOG.debug("- configuration create command with params {}", createConf);
            try {
                for (final Map.Entry<String, String> entrySet : createConf.entrySet()) {
                    final AttributeTO attrTO = new AttributeTO();
                    attrTO.setSchema(entrySet.getKey());
                    attrTO.getValues().add(entrySet.getValue());
                    configurationService.set(entrySet.getKey(), attrTO);
                    System.out.println(" - Conf " + attrTO.getSchema() + " created with value(s) " + attrTO.getValues()
                            + " - readonly: " + attrTO.isReadonly());
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (StringUtils.isNotBlank(confNameToDelete)) {
            try {
                LOG.debug("- configuration delete {} command", confNameToDelete);
                configurationService.delete(confNameToDelete);
                System.out.println(" - Conf " + confNameToDelete + " deleted!");
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (validators) {
            LOG.debug("- configuration validators command");
            try {
                for (final Validator validator : configurationService.getValidators()) {
                    System.out.println(" - Conf validator class: " + validator.getElement());
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (mailTemplates) {
            LOG.debug("- configuration mailTemplates command");
            try {
                System.out.println(" - Conf mail template for:");
                for (final MailTemplate mailTemplate : configurationService.getMailTemplates()) {
                    System.out.println("    *** " + mailTemplate.getElement());
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (StringUtils.isNotBlank(export)) {
            LOG.debug("- configuration export command, directory where xml will be export: {}", export);

            try {
                TransformerFactory.newInstance().newTransformer()
                        .transform(new DOMSource(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                                                new InputSource(new StringReader(IOUtils.toString(
                                                                        (SequenceInputStream) configurationService.
                                                                        export().getEntity()))))),
                                new StreamResult(new File(export + EXPORT_FILE_NAME)));
                System.out.println(" - " + export + EXPORT_FILE_NAME + " successfully created");
            } catch (final IOException ex) {
                LOG.error("Error creating content.xml file in {} directory", export, ex);
                System.out.println(" - Error creating " + export + EXPORT_FILE_NAME + " " + ex.getMessage());
            } catch (final ParserConfigurationException ex) {
                LOG.error("Error creating content.xml file in {} directory", export, ex);
                System.out.println(" - Error creating " + export + EXPORT_FILE_NAME + " " + ex.getMessage());
            } catch (final SAXException ex) {
                LOG.error("Error creating content.xml file in {} directory", export, ex);
                System.out.println(" - Error creating " + export + EXPORT_FILE_NAME + " " + ex.getMessage());
            } catch (final TransformerConfigurationException ex) {
                LOG.error("Error creating content.xml file in {} directory", export, ex);
                System.out.println(" - Error creating " + export + EXPORT_FILE_NAME + " " + ex.getMessage());
            } catch (final TransformerException ex) {
                LOG.error("Error creating content.xml file in {} directory", export, ex);
                System.out.println(" - Error creating " + export + EXPORT_FILE_NAME + " " + ex.getMessage());
            } catch (final SyncopeClientException ex) {
                LOG.error("Error calling configuration service", ex);
                System.out.println(" - Error calling configuration service " + ex.getMessage());
            }
        } else {
            System.out.println(helpMessage);
        }
    }

}
