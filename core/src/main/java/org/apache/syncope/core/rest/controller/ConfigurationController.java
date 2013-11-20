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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.init.WorkflowAdapterLoader;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.persistence.validation.attrvalue.Validator;
import org.apache.syncope.core.rest.data.ConfigurationDataBinder;
import org.apache.syncope.core.util.ContentExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConfigurationController extends AbstractTransactionalController<ConfigurationTO> {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConfigurationDataBinder binder;

    @Autowired
    private ContentExporter exporter;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private ResourcePatternResolver resResolver;

    @Autowired
    private WorkflowAdapterLoader wfAdapterLoader;

    @PreAuthorize("hasRole('CONFIGURATION_CREATE')")
    public ConfigurationTO create(final ConfigurationTO configurationTO) {
        LOG.debug("Configuration create called with parameters {}", configurationTO);

        SyncopeConf conf = binder.create(configurationTO);
        conf = confDAO.save(conf);

        return binder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    public ConfigurationTO delete(final String key) {
        SyncopeConf conf = confDAO.find(key);
        ConfigurationTO confToDelete = binder.getConfigurationTO(conf);
        confDAO.delete(key);
        return confToDelete;
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    public List<ConfigurationTO> list() {
        List<SyncopeConf> configurations = confDAO.findAll();
        List<ConfigurationTO> configurationTOs = new ArrayList<ConfigurationTO>(configurations.size());

        for (SyncopeConf configuration : configurations) {
            configurationTOs.add(binder.getConfigurationTO(configuration));
        }

        return configurationTOs;
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    public ConfigurationTO read(final String key) {
        ConfigurationTO result;
        try {
            SyncopeConf conf = confDAO.find(key);
            result = binder.getConfigurationTO(conf);
        } catch (MissingConfKeyException e) {
            LOG.error("Could not find configuration key '" + key + "', returning null");

            result = new ConfigurationTO();
            result.setKey(key);
        }

        return result;
    }

    @PreAuthorize("hasRole('CONFIGURATION_UPDATE')")
    public ConfigurationTO update(final ConfigurationTO configurationTO) {
        SyncopeConf conf = confDAO.find(configurationTO.getKey());
        conf.setValue(configurationTO.getValue());
        return binder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    public Set<String> getValidators() {
        return classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.VALIDATOR);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    public Set<String> getMailTemplates() {
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

        return htmlTemplates;
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @Transactional(readOnly = true)
    public void export(final OutputStream os) {
        try {
            exporter.export(os, wfAdapterLoader.getTablePrefix());
            LOG.debug("Database content successfully exported");
        } catch (Exception e) {
            LOG.error("While exporting database content", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigurationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ConfigurationTO) {
                    key = ((ConfigurationTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getConfigurationTO(confDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}