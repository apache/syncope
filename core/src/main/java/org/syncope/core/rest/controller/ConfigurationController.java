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
package org.syncope.core.rest.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.core.audit.AuditManager;
import org.syncope.core.init.ImplementationClassNamesLoader;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.validation.attrvalue.Validator;
import org.syncope.core.rest.data.ConfigurationDataBinder;
import org.syncope.core.util.ImportExport;
import org.syncope.types.AuditElements.Category;
import org.syncope.types.AuditElements.ConfigurationSubCategory;
import org.syncope.types.AuditElements.Result;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConfigurationDataBinder configurationDataBinder;

    @Autowired
    private ImportExport importExport;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private ResourcePatternResolver resResolver;

    @PreAuthorize("hasRole('CONFIGURATION_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public ConfigurationTO create(final HttpServletResponse response,
            @RequestBody final ConfigurationTO configurationTO) {
        LOG.debug("Configuration create called with parameters {}", configurationTO);

        SyncopeConf conf = configurationDataBinder.createSyncopeConfiguration(configurationTO);
        conf = confDAO.save(conf);

        auditManager.audit(Category.configuration, ConfigurationSubCategory.create, Result.success,
                "Successfully created conf: " + conf.getKey());

        response.setStatus(HttpServletResponse.SC_CREATED);

        return configurationDataBinder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{key}")
    public ConfigurationTO delete(@PathVariable("key") final String key) throws MissingConfKeyException {

        SyncopeConf conf = confDAO.find(key);
        ConfigurationTO confToDelete = configurationDataBinder.getConfigurationTO(conf);
        confDAO.delete(key);

        auditManager.audit(Category.configuration, ConfigurationSubCategory.delete, Result.success,
                "Successfully deleted conf: " + key);
        return confToDelete;
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public List<ConfigurationTO> list(final HttpServletRequest request) {
        List<SyncopeConf> configurations = confDAO.findAll();
        List<ConfigurationTO> configurationTOs = new ArrayList<ConfigurationTO>(configurations.size());

        for (SyncopeConf configuration : configurations) {
            configurationTOs.add(configurationDataBinder.getConfigurationTO(configuration));
        }

        auditManager.audit(Category.configuration, ConfigurationSubCategory.list, Result.success,
                "Successfully listed all confs: " + configurationTOs.size());

        return configurationTOs;
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{key}")
    public ConfigurationTO read(final HttpServletResponse response, @PathVariable("key") final String key)
            throws MissingConfKeyException {

        ConfigurationTO result;
        try {
            SyncopeConf conf = confDAO.find(key);
            result = configurationDataBinder.getConfigurationTO(conf);

            auditManager.audit(Category.configuration, ConfigurationSubCategory.read, Result.success,
                    "Successfully read conf: " + key);
        } catch (MissingConfKeyException e) {
            LOG.error("Could not find configuration key '" + key + "', returning null");

            result = new ConfigurationTO();
            result.setKey(key);

            auditManager.audit(Category.configuration, ConfigurationSubCategory.read, Result.failure,
                    "Could not find conf: " + key);
        }

        return result;
    }

    @PreAuthorize("hasRole('CONFIGURATION_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public ConfigurationTO update(final HttpServletResponse response, @RequestBody final ConfigurationTO configurationTO)
            throws MissingConfKeyException {

        SyncopeConf conf = confDAO.find(configurationTO.getKey());
        conf.setValue(configurationTO.getValue());

        auditManager.audit(Category.configuration, ConfigurationSubCategory.update, Result.success,
                "Successfully updated conf: " + conf.getKey());

        return configurationDataBinder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/validators")
    public ModelAndView getValidators() {
        Set<String> validators = classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.VALIDATOR);

        auditManager.audit(Category.configuration, ConfigurationSubCategory.getValidators, Result.success,
                "Successfully listed all validators: " + validators.size());

        return new ModelAndView().addObject(validators);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/mailTemplates")
    public ModelAndView getMailTemplates() {
        Set<String> htmlTemplates = new HashSet<String>();
        Set<String> textTemplates = new HashSet<String>();

        try {
            for (Resource resource : resResolver.getResources("classpath:/mailTemplates/*.vm")) {

                String template = resource.getURL().toExternalForm();
                if (template.endsWith(".html.vm")) {
                    htmlTemplates.add(
                            template.substring(template.indexOf("mailTemplates/") + 14, template.indexOf(".html.vm")));
                } else if (template.endsWith(".txt.vm")) {
                    textTemplates.add(
                            template.substring(template.indexOf("mailTemplates/") + 14, template.indexOf(".txt.vm")));
                } else {
                    LOG.warn("Unexpected template found: {}, ignoring...", template);
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}", Validator.class.getName(), e);
        }

        // Only templates available both as HTML and TEXT are considered
        htmlTemplates.retainAll(textTemplates);

        auditManager.audit(Category.configuration, ConfigurationSubCategory.getMailTemplates, Result.success,
                "Successfully listed all mail templates: " + htmlTemplates.size());

        return new ModelAndView().addObject(htmlTemplates);
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/dbexport")
    @Transactional(readOnly = true)
    public void dbExport(final HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_XML_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=content.xml");

        try {
            importExport.export(response.getOutputStream());

            auditManager.audit(Category.configuration, ConfigurationSubCategory.dbExport, Result.success,
                    "Successfully exported database content");
            LOG.debug("Database content successfully exported");
        } catch (Throwable t) {
            auditManager.audit(Category.configuration, ConfigurationSubCategory.dbExport, Result.failure,
                    "Could not export database content", t);
            LOG.error("While exporting database content", t);
        }
    }
}
