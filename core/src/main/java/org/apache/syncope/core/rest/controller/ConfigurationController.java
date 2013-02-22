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
package org.apache.syncope.core.rest.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.ConfigurationSubCategory;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.init.WorkflowAdapterLoader;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.persistence.validation.attrvalue.Validator;
import org.apache.syncope.core.rest.data.ConfigurationDataBinder;
import org.apache.syncope.core.util.ImportExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConfigurationDataBinder binder;

    @Autowired
    private ImportExport importExport;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private ResourcePatternResolver resResolver;

    @Autowired
    private WorkflowAdapterLoader wfAdapterLoader;

    @PreAuthorize("hasRole('CONFIGURATION_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public ConfigurationTO create(final HttpServletResponse response,
            @RequestBody final ConfigurationTO configurationTO) {
        LOG.debug("Configuration create called with parameters {}", configurationTO);

        SyncopeConf conf = binder.create(configurationTO);
        conf = confDAO.save(conf);

        auditManager.audit(Category.configuration, ConfigurationSubCategory.create, Result.success,
                "Successfully created conf: " + conf.getKey());

        response.setStatus(HttpServletResponse.SC_CREATED);

        return binder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{key}")
    public ConfigurationTO delete(@PathVariable("key") final String key) {
        SyncopeConf conf = confDAO.find(key);
        ConfigurationTO confToDelete = binder.getConfigurationTO(conf);
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
            configurationTOs.add(binder.getConfigurationTO(configuration));
        }

        auditManager.audit(Category.configuration, ConfigurationSubCategory.list, Result.success,
                "Successfully listed all confs: " + configurationTOs.size());

        return configurationTOs;
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{key}")
    public ConfigurationTO read(final HttpServletResponse response, @PathVariable("key") final String key) {

        ConfigurationTO result;
        try {
            SyncopeConf conf = confDAO.find(key);
            result = binder.getConfigurationTO(conf);

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
    public ConfigurationTO update(@RequestBody final ConfigurationTO configurationTO) {
        SyncopeConf conf = confDAO.find(configurationTO.getKey());
        conf.setValue(configurationTO.getValue());

        auditManager.audit(Category.configuration, ConfigurationSubCategory.update, Result.success,
                "Successfully updated conf: " + conf.getKey());

        return binder.getConfigurationTO(conf);
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
        response.setContentType(MediaType.TEXT_XML);
        response.setHeader(SyncopeConstants.CONTENT_DISPOSITION_HEADER,
                "attachment; filename=" + ImportExport.CONTENT_FILE);
        try {
            dbExportInternal(response.getOutputStream());
        } catch (IOException e) {
            LOG.error("Getting servlet output stream", e);
        }
    }

    @Transactional(readOnly = true)
    public void dbExportInternal(final OutputStream os) {
        try {
            importExport.export(os, wfAdapterLoader.getTablePrefix());

            auditManager.audit(Category.configuration, ConfigurationSubCategory.dbExport, Result.success,
                    "Successfully exported database content");
            LOG.debug("Database content successfully exported");
        } catch (Exception e) {
            auditManager.audit(Category.configuration, ConfigurationSubCategory.dbExport, Result.failure,
                    "Could not export database content", e);
            LOG.error("While exporting database content", e);
        }
    }
}
