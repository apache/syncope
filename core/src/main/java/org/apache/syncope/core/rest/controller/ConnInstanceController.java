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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.init.ConnInstanceLoader;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.propagation.ConnectorFacadeProxy;
import org.apache.syncope.core.rest.data.ConnInstanceDataBinder;
import org.apache.syncope.core.util.ConnBundleManager;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.to.ConnBundleTO;
import org.apache.syncope.to.ConnInstanceTO;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.ConnectorSubCategory;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.ConnConfPropSchema;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/connector")
public class ConnInstanceController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnBundleManager bundleManager;

    @Autowired
    private ConnInstanceLoader connLoader;

    @PreAuthorize("hasRole('CONNECTOR_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public ConnInstanceTO create(final HttpServletResponse response, @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("ConnInstance create called with configuration {}", connectorTO);

        ConnInstance connInstance = binder.getConnInstance(connectorTO);

        try {
            connInstance = connInstanceDAO.save(connInstance);
            auditManager.audit(Category.connector, ConnectorSubCategory.create, Result.success,
                    "Successfully created connector instance: " + connInstance.getDisplayName());
        } catch (Exception e) {
            auditManager.audit(Category.connector, ConnectorSubCategory.create, Result.failure,
                    "Could not create connector instance: " + connectorTO.getDisplayName(), e);

            SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidConnInstance);
            invalidConnInstance.addElement(e.getMessage());

            scce.addException(invalidConnInstance);
            throw scce;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public ConnInstanceTO update(@RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("Connector update called with configuration {}", connectorTO);

        ConnInstance connInstance = binder.updateConnInstance(connectorTO.getId(), connectorTO);

        try {
            connInstance = connInstanceDAO.save(connInstance);
            auditManager.audit(Category.connector, ConnectorSubCategory.update, Result.success,
                    "Successfully update connector instance: " + connInstance.getDisplayName());
        } catch (Exception e) {
            auditManager.audit(Category.connector, ConnectorSubCategory.create, Result.failure,
                    "Could not update connector instance: " + connectorTO.getDisplayName(), e);

            SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidConnInstance);
            invalidConnInstance.addElement(e.getMessage());

            scce.addException(invalidConnInstance);
            throw scce;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{connectorId}")
    public ConnInstanceTO delete(@PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnInstance connInstance = connInstanceDAO.find(connectorId);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connectorId + "'");
        }

        if (!connInstance.getResources().isEmpty()) {
            SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance = new SyncopeClientException(
                    SyncopeClientExceptionType.ExistingResource);
            for (ExternalResource resource : connInstance.getResources()) {
                invalidConnInstance.addElement(resource.getName());
            }

            scce.addException(invalidConnInstance);
            throw scce;
        }

        ConnInstanceTO connToDelete = binder.getConnInstanceTO(connInstance);

        connInstanceDAO.delete(connectorId);
        auditManager.audit(Category.connector, ConnectorSubCategory.delete, Result.success,
                "Successfully deleted connector instance: " + connectorId);

        return connToDelete;
    }

    @PreAuthorize("hasRole('CONNECTOR_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true)
    public List<ConnInstanceTO> list(@RequestParam(value = "lang", required = false) final String lang) {
        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnInstance> connInstances = connInstanceDAO.findAll();

        final List<ConnInstanceTO> connInstanceTOs = new ArrayList<ConnInstanceTO>();

        for (ConnInstance connector : connInstances) {
            try {
                connInstanceTOs.add(binder.getConnInstanceTO(connector));
            } catch (NotFoundException e) {
                LOG.error("Connector '{}#{}' not found", connector.getBundleName(), connector.getVersion());
            }
        }

        auditManager.audit(Category.connector, ConnectorSubCategory.list, Result.success,
                "Successfully listed all connectors: " + connInstanceTOs.size());

        return connInstanceTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{connectorId}")
    @Transactional(readOnly = true)
    public ConnInstanceTO read(@PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnInstance connInstance = connInstanceDAO.find(connectorId);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connectorId + "'");
        }

        auditManager.audit(Category.connector, ConnectorSubCategory.read, Result.success,
                "Successfully read connector: " + connInstance.getDisplayName());

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/bundle/list")
    @Transactional(readOnly = true)
    public List<ConnBundleTO> getBundles(@RequestParam(value = "lang", required = false) final String lang)
            throws NotFoundException, MissingConfKeyException {

        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        ConnectorInfoManager manager = bundleManager.getConnectorManager();

        List<ConnectorInfo> bundles = manager.getConnectorInfos();

        if (LOG.isDebugEnabled() && bundles != null) {
            LOG.debug("#Bundles: {}", bundles.size());

            for (ConnectorInfo bundle : bundles) {
                LOG.debug("Bundle: {}", bundle.getConnectorDisplayName());
            }
        }

        List<ConnBundleTO> connectorBundleTOs = new ArrayList<ConnBundleTO>();
        if (bundles != null) {
            for (ConnectorInfo bundle : bundles) {
                ConnBundleTO connectorBundleTO = new ConnBundleTO();
                connectorBundleTO.setDisplayName(bundle.getConnectorDisplayName());

                ConnectorKey key = bundle.getConnectorKey();

                LOG.debug("\nBundle name: {}" + "\nBundle version: {}" + "\nBundle class: {}", new Object[]{
                            key.getBundleName(), key.getBundleVersion(), key.getConnectorName()});

                connectorBundleTO.setBundleName(key.getBundleName());
                connectorBundleTO.setConnectorName(key.getConnectorName());
                connectorBundleTO.setVersion(key.getBundleVersion());

                ConfigurationProperties properties = bundleManager.getConfigurationProperties(bundle);

                for (String propName : properties.getPropertyNames()) {
                    ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();

                    ConfigurationPropertyImpl configurationProperty =
                            (ConfigurationPropertyImpl) properties.getProperty(propName);

                    // set name
                    connConfPropSchema.setName(configurationProperty.getName());

                    // set display name
                    connConfPropSchema.setDisplayName(configurationProperty.getDisplayName(propName));

                    // set help message
                    connConfPropSchema.setHelpMessage(configurationProperty.getHelpMessage(propName));

                    // set if mandatory
                    connConfPropSchema.setRequired(configurationProperty.isRequired());

                    // set type
                    connConfPropSchema.setType(configurationProperty.getType().getName());

                    // set order
                    connConfPropSchema.setOrder(configurationProperty.getOrder());

                    // set confidential
                    connConfPropSchema.setConfidential(configurationProperty.isConfidential());

                    connectorBundleTO.addProperty(connConfPropSchema);
                }

                LOG.debug("Bundle properties: {}", connectorBundleTO.getProperties());

                connectorBundleTOs.add(connectorBundleTO);
            }
        }

        auditManager.audit(Category.connector, ConnectorSubCategory.getBundles, Result.success,
                "Successfully listed all bundles: " + connectorBundleTOs.size());

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/schema/list")
    @Transactional(readOnly = true)
    public List<String> getSchemaNames(final HttpServletResponse response,
            @RequestBody final ConnInstanceTO connectorTO,
            @RequestParam(required = false, value = "showall", defaultValue = "false") final boolean showall)
            throws NotFoundException {

        final ConnInstance connInstance = connInstanceDAO.find(connectorTO.getId());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connectorTO.getId() + "'");
        }

        // consider the possibility to receive overridden properties only
        final Set<ConnConfProperty> conf = mergeConnConfProperties(connectorTO.getConfiguration(), connInstance.
                getConfiguration());

        // We cannot use Spring bean because this method could be used during
        // resource definition or modification: bean couldn't exist or bean
        // couldn't be updated.
        // This is the reason why we should take a "not mature" connector
        // facade proxy to ask for schema names.

        final List<String> result = new ArrayList<String>(connLoader.createConnectorBean(connInstance, conf).getSchema(
                showall));

        Collections.sort(result);

        auditManager.audit(Category.connector, ConnectorSubCategory.getSchemaNames, Result.success,
                "Successfully listed all schema names (" + result.size() + ") for connector "
                + connInstance.getDisplayName());

        return result;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/{connectorId}/configurationProperty/list")
    @Transactional(readOnly = true)
    public List<ConnConfProperty> getConfigurationProperties(@PathVariable("connectorId") final Long connectorId)
            throws NotFoundException {

        final ConnInstance connInstance = connInstanceDAO.find(connectorId);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connectorId + "'");
        }

        List<ConnConfProperty> result = new ArrayList<ConnConfProperty>(connInstance.getConfiguration());

        auditManager.audit(Category.connector, ConnectorSubCategory.getConfigurationProperties, Result.success,
                "Successfully listed all conf properties (" + result.size() + ") for connector "
                + connInstance.getDisplayName());

        return result;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/check")
    @Transactional(readOnly = true)
    public ModelAndView check(final HttpServletResponse response, @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        final ConnectorFacadeProxy connector = new ConnectorFacadeProxy(binder.getConnInstance(connectorTO),
                bundleManager);

        boolean result;
        try {
            connector.test();
            result = true;

            auditManager.audit(Category.connector, ConnectorSubCategory.check, Result.success,
                    "Successfully checked connector: " + connectorTO);
        } catch (Exception ex) {
            auditManager.audit(Category.connector, ConnectorSubCategory.check, Result.failure,
                    "Unsuccessful check for connector: " + connectorTO, ex);

            LOG.error("Test connection failure {}", ex);
            result = false;
        }

        return new ModelAndView().addObject(result);
    }

    /**
     * Merge connector configuration properties avoiding repetition but giving priority to primary set.
     *
     * @param primary primary set.
     * @param secondary secondary set.
     * @return merged set.
     */
    private Set<ConnConfProperty> mergeConnConfProperties(final Set<ConnConfProperty> primary,
            final Set<ConnConfProperty> secondary) {

        final Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        // to be used to control managed prop (needed by overridden mechanism)
        final Set<String> propertyNames = new HashSet<String>();

        // get overridden connector configuration properties
        for (ConnConfProperty prop : primary) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                conf.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        // get connector configuration properties
        for (ConnConfProperty prop : secondary) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                conf.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        return conf;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/{resourceName}/connectorBean")
    @Transactional(readOnly = true)
    public ConnInstanceTO readConnectorBean(@PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        final ConnectorFacadeProxy connector = connLoader.getConnector(resource);

        auditManager.audit(Category.connector, ConnectorSubCategory.readConnectorBean, Result.success,
                "Successfully read connector for resource: " + resourceName);

        return binder.getConnInstanceTO(connector.getActiveConnInstance());
    }
}
