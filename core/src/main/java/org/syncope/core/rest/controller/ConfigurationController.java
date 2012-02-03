/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.validation.attrvalue.AbstractValidator;
import org.syncope.core.persistence.validation.attrvalue.Validator;
import org.syncope.core.rest.data.ConfigurationDataBinder;
import org.syncope.core.util.ImportExport;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractController {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConfigurationDataBinder configurationDataBinder;

    @Autowired
    private ImportExport importExport;

    @Autowired
    private ResourcePatternResolver resResolver;

    @PreAuthorize("hasRole('CONFIGURATION_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConfigurationTO create(final HttpServletResponse response,
            @RequestBody final ConfigurationTO configurationTO) {

        LOG.debug("Configuration create called with parameters {}",
                configurationTO);

        SyncopeConf conf = configurationDataBinder.createSyncopeConfiguration(
                configurationTO);
        conf = confDAO.save(conf);

        response.setStatus(HttpServletResponse.SC_CREATED);

        return configurationDataBinder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{key}")
    public void delete(@PathVariable("key") final String key)
            throws MissingConfKeyException {

        confDAO.find(key);
        confDAO.delete(key);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<ConfigurationTO> list(HttpServletRequest request) {
        List<SyncopeConf> configurations = confDAO.findAll();
        List<ConfigurationTO> configurationTOs =
                new ArrayList<ConfigurationTO>(configurations.size());

        for (SyncopeConf configuration : configurations) {
            configurationTOs.add(
                    configurationDataBinder.getConfigurationTO(configuration));
        }

        return configurationTOs;
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{key}")
    public ConfigurationTO read(HttpServletResponse response,
            @PathVariable("key") String key)
            throws MissingConfKeyException {

        ConfigurationTO result;
        try {
            SyncopeConf conf = confDAO.find(key);
            result = configurationDataBinder.getConfigurationTO(conf);
        } catch (MissingConfKeyException e) {
            LOG.error("Could not find configuration key '" + key
                    + "', returning null");

            result = new ConfigurationTO();
            result.setKey(key);
        }

        return result;
    }

    @PreAuthorize("hasRole('CONFIGURATION_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConfigurationTO update(final HttpServletResponse response,
            @RequestBody final ConfigurationTO configurationTO)
            throws MissingConfKeyException {

        SyncopeConf syncopeConfiguration =
                confDAO.find(configurationTO.getKey());

        syncopeConfiguration.setValue(configurationTO.getValue());

        return configurationDataBinder.getConfigurationTO(syncopeConfiguration);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/validators")
    public ModelAndView getValidators() {
        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        Set<String> validators = new HashSet<String>();
        try {
            for (Resource resource : resResolver.getResources(
                    "classpath:org/syncope/core/persistence/validation/"
                    + "attrvalue/*.class")) {

                ClassMetadata metadata =
                        cachingMetadataReaderFactory.getMetadataReader(
                        resource).getClassMetadata();
                if (ArrayUtils.contains(metadata.getInterfaceNames(),
                        Validator.class.getName())
                        || AbstractValidator.class.getName().equals(
                        metadata.getSuperClassName())) {

                    try {
                        Class jobClass = Class.forName(metadata.getClassName());
                        if (!Modifier.isAbstract(jobClass.getModifiers())) {
                            validators.add(jobClass.getName());
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error("Could not load class {}",
                                metadata.getClassName(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}",
                    Validator.class.getName(), e);
        }

        return new ModelAndView().addObject(validators);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/mailTemplates")
    public ModelAndView getMailTemplates() {
        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        Set<String> htmlTemplates = new HashSet<String>();
        Set<String> textTemplates = new HashSet<String>();

        try {
            for (Resource resource : resResolver.getResources(
                    "classpath:/mailTemplates/*.vm")) {

                String template = resource.getURL().toExternalForm();
                if (template.endsWith(".html.vm")) {
                    htmlTemplates.add(template.substring(
                            template.indexOf("mailTemplates/") + 14,
                            template.indexOf(".html.vm")));
                } else if (template.endsWith(".txt.vm")) {
                    textTemplates.add(template.substring(
                            template.indexOf("mailTemplates/") + 14,
                            template.indexOf(".txt.vm")));
                } else {
                    LOG.warn("Unexpected template found: {}, ignoring...",
                            template);
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}",
                    Validator.class.getName(), e);
        }

        // Only templates available both as HTML and TEXT are considered
        htmlTemplates.retainAll(textTemplates);

        return new ModelAndView().addObject(htmlTemplates);
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/dbexport")
    @Transactional(readOnly = true)
    public void dbExport(final HttpServletResponse response) {
        try {
            importExport.export(response.getOutputStream());
            response.flushBuffer();

            LOG.debug("Default content successfully exported");
        } catch (Throwable t) {
            LOG.error("While exporting content", t);
        }

        response.setContentType("application/xml;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=content.xml");
    }
}
