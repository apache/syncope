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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
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
import org.syncope.client.to.ConnBundleTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.data.ConnInstanceDataBinder;
import org.syncope.core.util.ConnBundleManager;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/connector")
public class ConnInstanceController extends AbstractController {

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnBundleManager bundleManager;

    @Autowired
    private ConnInstanceLoader connLoader;

    @PreAuthorize("hasRole('CONNECTOR_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConnInstanceTO create(
            final HttpServletResponse response,
            @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("ConnInstance create called with configuration {}",
                connectorTO);

        ConnInstance connInstance = binder.getConnInstance(connectorTO);

        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (Throwable t) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidConnInstance);
            invalidConnInstance.addElement(t.getMessage());

            scce.addException(invalidConnInstance);
            throw scce;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConnInstanceTO update(
            @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("Connector update called with configuration {}", connectorTO);

        ConnInstance connInstance =
                binder.updateConnInstance(connectorTO.getId(), connectorTO);

        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (RuntimeException e) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidConnInstance);
            invalidConnInstance.addElement(e.getMessage());

            scce.addException(invalidConnInstance);
            throw scce;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{connectorId}")
    public void delete(@PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnInstance connInstance = connInstanceDAO.find(connectorId);
        if (connInstance == null) {
            LOG.error("Could not find connector '" + connectorId + "'");

            throw new NotFoundException(String.valueOf(connectorId));
        }

        if (!connInstance.getResources().isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.ResourceExist);
            for (ExternalResource resource : connInstance.getResources()) {
                invalidConnInstance.addElement(resource.getName());
            }

            scce.addException(invalidConnInstance);
            throw scce;
        }

        connInstanceDAO.delete(connectorId);
    }

    @PreAuthorize("hasRole('CONNECTOR_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    @Transactional(readOnly = true)
    public List<ConnInstanceTO> list(
            @RequestParam(value = "lang", required = false) final String lang) {

        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnInstance> connInstances = connInstanceDAO.findAll();

        final List<ConnInstanceTO> connInstanceTOs =
                new ArrayList<ConnInstanceTO>();

        for (ConnInstance connector : connInstances) {
            try {
                connInstanceTOs.add(binder.getConnInstanceTO(connector));
            } catch (NotFoundException e) {
                LOG.error("Connector '{}#{}' not found",
                        connector.getBundleName(),
                        connector.getVersion());
            }
        }

        return connInstanceTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{connectorId}")
    @Transactional(readOnly = true)
    public ConnInstanceTO read(
            @PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnInstance connInstance = connInstanceDAO.find(connectorId);

        if (connInstance == null) {
            LOG.error("Could not find connector '" + connectorId + "'");

            throw new NotFoundException(String.valueOf(connectorId));
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/bundle/list")
    @Transactional(readOnly = true)
    public List<ConnBundleTO> getBundles(
            @RequestParam(value = "lang", required = false) final String lang)
            throws NotFoundException, MissingConfKeyException {

        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        ConnectorInfoManager manager =
                bundleManager.getConnectorManager();

        List<ConnectorInfo> bundles = manager.getConnectorInfos();

        if (LOG.isDebugEnabled() && bundles != null) {
            LOG.debug("#Bundles: {}", bundles.size());

            for (ConnectorInfo bundle : bundles) {
                LOG.debug("Bundle: {}", bundle.getConnectorDisplayName());
            }
        }

        ConnBundleTO connectorBundleTO;
        ConnectorKey key;
        ConfigurationProperties properties;

        List<ConnBundleTO> connectorBundleTOs = new ArrayList<ConnBundleTO>();
        if (bundles != null) {
            for (ConnectorInfo bundle : bundles) {
                connectorBundleTO = new ConnBundleTO();
                connectorBundleTO.setDisplayName(
                        bundle.getConnectorDisplayName());

                key = bundle.getConnectorKey();

                LOG.debug("\nBundle name: {}"
                        + "\nBundle version: {}"
                        + "\nBundle class: {}",
                        new Object[]{
                            key.getBundleName(),
                            key.getBundleVersion(),
                            key.getConnectorName()});

                connectorBundleTO.setBundleName(key.getBundleName());
                connectorBundleTO.setConnectorName(key.getConnectorName());
                connectorBundleTO.setVersion(key.getBundleVersion());

                properties = bundleManager.getConfigurationProperties(bundle);

                ConnConfPropSchema connConfPropSchema;
                ConfigurationProperty configurationProperty;

                for (String propName : properties.getPropertyNames()) {
                    connConfPropSchema = new ConnConfPropSchema();

                    configurationProperty = properties.getProperty(propName);

                    // set name
                    connConfPropSchema.setName(
                            configurationProperty.getName());

                    // set display name
                    connConfPropSchema.setDisplayName(
                            configurationProperty.getDisplayName(propName));

                    // set help message
                    connConfPropSchema.setHelpMessage(
                            configurationProperty.getHelpMessage(propName));

                    // set if mandatory
                    connConfPropSchema.setRequired(
                            configurationProperty.isRequired());

                    // set type
                    connConfPropSchema.setType(
                            configurationProperty.getType().getName());

                    connectorBundleTO.addProperty(connConfPropSchema);
                }

                LOG.debug("Bundle properties: {}",
                        connectorBundleTO.getProperties());

                connectorBundleTOs.add(connectorBundleTO);
            }
        }

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/schema/list")
    @Transactional(readOnly = true)
    public List<String> getSchemaNames(
            final HttpServletResponse response,
            @RequestBody final ConnInstanceTO connectorTO,
            @RequestParam(required = false,
            value = "showall", defaultValue = "false") final boolean showall)
            throws NotFoundException {

        final ConnInstance connInstance =
                connInstanceDAO.find(connectorTO.getId());

        if (connInstance == null) {
            LOG.error("Could not find connector '" + connInstance + "'");
            throw new NotFoundException("Connector '" + connInstance + "'");
        }

        // consider the possibility to receive overridden properties only
        final Set<ConnConfProperty> conf = mergeConnConfProperties(
                connectorTO.getConfiguration(),
                connInstance.getConfiguration());

        // We cannot use Spring bean because this method could be used during
        // resource definition or modification: bean couldn't exist or bean
        // couldn't be updated.
        // This is the reason why we should take a "not mature" connector
        // facade proxy to ask for schema names.

        final List<String> result = new ArrayList<String>(
                connLoader.createConnectorBean(
                connInstance, conf).getSchema(showall));

        Collections.sort(result);

        return result;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{connectorId}/configurationProperty/list")
    @Transactional(readOnly = true)
    public List<ConnConfProperty> getConfigurationProperties(
            @PathVariable("connectorId") final Long connectorId)
            throws NotFoundException {

        final ConnInstance connector = connInstanceDAO.find(connectorId);
        if (connector == null) {
            throw new NotFoundException(String.format(
                    "Connector instance with id %d not found", connectorId));
        }
        return new ArrayList<ConnConfProperty>(connector.getConfiguration());
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/check")
    @Transactional(readOnly = true)
    public ModelAndView check(final HttpServletResponse response,
            @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        final ConnectorFacadeProxy connector =
                new ConnectorFacadeProxy(
                binder.getConnInstance(connectorTO), bundleManager);

        try {
            connector.test();
            return new ModelAndView().addObject(true);
        } catch (Exception ex) {
            LOG.error("Test connection failure {}", ex);
            return new ModelAndView().addObject(false);
        }
    }

    /**
     * Merge connector configuration properties avoiding repetition but giving
     * priority to primary set.
     *
     * @param primary primary set.
     * @param secondary secondary set.
     * @return merged set.
     */
    private Set<ConnConfProperty> mergeConnConfProperties(
            final Set<ConnConfProperty> primary,
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
}
