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
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.init.WorkflowAdapterLoader;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.conf.CAttr;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.validation.attrvalue.Validator;
import org.apache.syncope.core.rest.data.ConfigurationDataBinder;
import org.apache.syncope.core.util.ContentExporter;
import org.apache.syncope.core.util.ResourceWithFallbackLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConfigurationController extends AbstractTransactionalController<ConfTO> {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConfigurationDataBinder binder;

    @Autowired
    private ContentExporter exporter;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @javax.annotation.Resource(name = "velocityResourceLoader")
    private ResourceWithFallbackLoader resourceLoader;

    @Autowired
    private WorkflowAdapterLoader wfAdapterLoader;

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    public void delete(final String key) {
        confDAO.delete(key);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    public ConfTO list() {
        return binder.getConfTO(confDAO.get());
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    public AttributeTO read(final String key) {
        CAttr conf = confDAO.find(key);
        if (conf == null) {
            throw new NotFoundException("Configuration key " + key);
        }

        return binder.getAttributeTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_SET')")
    public void set(final AttributeTO value) {
        confDAO.save(binder.getAttribute(value));
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
            for (Resource resource : resourceLoader.getResources(NotificationManager.MAIL_TEMPLATES + "*.vm")) {
                String template = resource.getURL().toExternalForm();
                if (template.endsWith(NotificationManager.MAIL_TEMPLATE_HTML_SUFFIX)) {
                    htmlTemplates.add(
                            template.substring(template.indexOf(NotificationManager.MAIL_TEMPLATES) + 14,
                                    template.indexOf(NotificationManager.MAIL_TEMPLATE_HTML_SUFFIX)));
                } else if (template.endsWith(NotificationManager.MAIL_TEMPLATE_TEXT_SUFFIX)) {
                    textTemplates.add(
                            template.substring(template.indexOf(NotificationManager.MAIL_TEMPLATES) + 14,
                                    template.indexOf(NotificationManager.MAIL_TEMPLATE_TEXT_SUFFIX)));
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

    @PreAuthorize("hasRole('CONFIGURATION_EXPORT')")
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
    protected ConfTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
