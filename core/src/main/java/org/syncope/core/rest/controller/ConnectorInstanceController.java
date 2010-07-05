
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

import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ConnectorInstanceTOs;
import org.syncope.client.to.PropertyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;
import org.syncope.core.persistence.util.ApplicationContextManager;
import org.syncope.core.rest.data.ConnectorInstanceDataBinder;

@Controller
@RequestMapping("/connector")
public class ConnectorInstanceController extends AbstractController {

    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    SyncopeConfigurationDAO syncopeConfigurationDAO;

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConnectorInstanceTO create(HttpServletResponse response,
            @RequestBody ConnectorInstanceTO connectorTO) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("create called with configuration " + connectorTO);
        }

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        ConnectorInstance actual = null;

        try {

            actual = binder.createConnectorInstance(connectorTO);

        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + connectorTO, e);
            return throwCompositeException(e, response);
        }

        if (actual == null)
            throw new IOException("Connector bind failed");

        ConnectorFacade connector = getConnectorFacade(
                connectorTO.getBundleName(),
                connectorTO.getVersion(),
                connectorTO.getConnectorName(),
                connectorTO.getConfiguration());

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        try {

            beanFactory.destroyBean(
                    actual.getId().toString(),
                    beanFactory.getBean(actual.getId().toString()));

        } catch (NoSuchBeanDefinitionException ignore) {
            // ignore exception
            if (log.isInfoEnabled()) {
                log.info("No bean named '" + actual.getId() + "' is defined");
            }
        }

        beanFactory.registerSingleton(actual.getId().toString(), connector);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getConnectorInstanceTO(actual);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConnectorInstanceTO update(HttpServletResponse response,
            @RequestBody ConnectorInstanceTO connectorTO) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("update called with configuration " + connectorTO);
        }

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        ConnectorInstance actual = null;

        try {

            actual = binder.updateConnectorInstance(
                    connectorTO.getId(), connectorTO);

        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + connectorTO, e);
            return throwCompositeException(e, response);
        }

        if (actual == null)
            throw new IOException("Connector bind failed");

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        ConnectorFacade connector = (ConnectorFacade) beanFactory.getSingleton(
                actual.getId().toString());

        if (connector == null) {
            connector = getConnectorFacade(
                    connectorTO.getBundleName(),
                    connectorTO.getVersion(),
                    connectorTO.getConnectorName(),
                    connectorTO.getConfiguration());
        }

        try {

            beanFactory.destroyBean(
                    actual.getId().toString(),
                    beanFactory.getBean(actual.getId().toString()));

        } catch (NoSuchBeanDefinitionException ignore) {
            // ignore exception
            if (log.isInfoEnabled()) {
                log.info("No bean named '" + actual.getId() + "' is defined");
            }
        }

        beanFactory.registerSingleton(actual.getId().toString(), connector);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getConnectorInstanceTO(actual);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{connectorId}")
    public void delete(HttpServletResponse response,
            @PathVariable("connectorId") Long connectorId)
            throws IOException {

        ConnectorInstance connectorInstance =
                connectorInstanceDAO.find(connectorId);

        if (connectorInstance == null) {
            log.error("Could not find connector '" + connectorId + "'");
            throwNotFoundException(String.valueOf(connectorId), response);
        } else {
            connectorInstanceDAO.delete(connectorId);

            ConfigurableApplicationContext context =
                    ApplicationContextManager.getApplicationContext();

            DefaultListableBeanFactory beanFactory =
                    (DefaultListableBeanFactory) context.getBeanFactory();

            try {

                beanFactory.destroyBean(
                        connectorId.toString(),
                        beanFactory.getBean(connectorId.toString()));

            } catch (NoSuchBeanDefinitionException ignore) {
                // ignore exception
                if (log.isInfoEnabled()) {
                    log.info("No bean named '" + connectorId + "' is defined");
                }
            }
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public ConnectorInstanceTOs list() {

        List<ConnectorInstance> connectorInstances =
                connectorInstanceDAO.findAll();

        List<ConnectorInstanceTO> instances =
                new ArrayList<ConnectorInstanceTO>();

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        for (ConnectorInstance connector : connectorInstances) {
            instances.add(binder.getConnectorInstanceTO(connector));
        }

        ConnectorInstanceTOs connectorInstanceTOs =
                new ConnectorInstanceTOs();

        connectorInstanceTOs.setInstances(instances);

        return connectorInstanceTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{connectorId}")
    public ConnectorInstanceTO read(HttpServletResponse response,
            @PathVariable("connectorId") Long connectorId) throws IOException {

        ConnectorInstance connectorInstance =
                connectorInstanceDAO.find(connectorId);

        if (connectorInstance == null) {
            log.error("Could not find connector '" + connectorId + "'");
            return throwNotFoundException("Connector not found", response);
        }

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        return binder.getConnectorInstanceTO(connectorInstance);
    }

    private ConnectorFacade getConnectorFacade(
            String bundlename,
            String bundleversion,
            String connectorname,
            Set<PropertyTO> configuration) throws IOException {

        ConnectorInfoManagerFactory connectorInfoManagerFactory =
                ConnectorInfoManagerFactory.getInstance();

        SyncopeConfiguration syncopeConfiguration =
                syncopeConfigurationDAO.find(
                "identityconnectors.bundle.directory");

        if (syncopeConfiguration == null) {
            throw new IOException("Syncope configuration not found");
        }

        File bundleDirectory = new File(syncopeConfiguration.getConfValue());

        List<URL> urls = new ArrayList<URL>();

        String[] files = bundleDirectory.list();

        if(files == null){
            throw new IOException("No bundles found");
        }

        for (String file : files) {
            try {
                urls.add(IOUtil.makeURL(bundleDirectory, file));
            } catch (Exception ignore) {
                // ignore exception and don't add bundle
                if (log.isDebugEnabled()) {
                    log.debug(
                            "\"" +
                            bundleDirectory.toString() + "/" + file +
                            "\"" +
                            " is not a valid connector bundle.", ignore);
                }
            }
        }

        if (urls.isEmpty()) {
            throw new IOException("No bundles found");
        }

        if (log.isDebugEnabled()) {
            log.debug("URL: " + urls.toString());
        }

        ConnectorInfoManager manager =
                connectorInfoManagerFactory.getLocalManager(
                urls.toArray(new URL[0]));


        if (manager == null) {
            throw new IOException("Connector Info Manager not found");
        }

        // list connectors info
        List<ConnectorInfo> infos = manager.getConnectorInfos();
        if (infos != null) {
            log.debug("infos size: " + infos.size());
            for (ConnectorInfo i : infos) {
                log.debug("Name: " + i.getConnectorDisplayName());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "\nBundle name: " + bundlename +
                    "\nBundle version: " + bundleversion +
                    "\nBundle class: " + connectorname);
        }

        // specify a connector.
        ConnectorKey key = new ConnectorKey(
                bundlename,
                bundleversion,
                connectorname);

        if (key == null) {
            throw new IOException("Connector Key not found");
        }


        // get the specified connector.
        ConnectorInfo info = manager.findConnectorInfo(key);

        if (info == null) {
            throw new IOException("Connector Info not found");
        }


        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        if (apiConfig == null) {
            throw new IOException("Default API configuration not found");
        }

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                apiConfig.getConfigurationProperties();

        if (properties == null) {
            throw new IOException("Configuration properties not found");
        }

        // Print out what the properties are (not necessary)
        List<String> propertyNames = properties.getPropertyNames();

        for (String propName : propertyNames) {
            ConfigurationProperty prop = properties.getProperty(propName);

            if (log.isDebugEnabled()) {
                log.debug(
                        "\nProperty Name: " + prop.getName() +
                        "\nProperty Type: " + prop.getType());
            }
        }

        // Set all of the ConfigurationProperties needed by the connector.
        for (PropertyTO property : configuration) {
            properties.setPropertyValue(
                    property.getKey(), property.getValue());
        }

        // Use the ConnectorFacadeFactory's newInstance() method to get
        // a new connector.
        ConnectorFacade connector =
                ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        if (connector == null) {
            throw new IOException("Connector not found");
        }

        // Make sure we have set up the Configuration properly
        connector.validate();
        //connector.test(); //needs a target resource deployed

        return connector;
    }
}
