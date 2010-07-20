
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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConnectorBundleTO;
import org.syncope.client.to.ConnectorBundleTOs;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ConnectorInstanceTOs;
import org.syncope.client.to.PropertyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
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

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConnectorInstanceTO create(HttpServletResponse response,
            @RequestBody ConnectorInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException,
            MissingConfKeyException {

        if (log.isDebugEnabled()) {
            log.debug("Create called with configuration " + connectorTO);
        }

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        ConnectorInstance actual = null;

        try {

            ConnectorInstance connectorInstance =
                    binder.getConnectorInstance(connectorTO);

            // Everything went out fine, we can flush to the database
            actual = connectorInstanceDAO.save(connectorInstance);

        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + connectorTO, e);

            throw e;
        }

        SyncopeConfiguration syncopeConfiguration =
                syncopeConfigurationDAO.find(
                "identityconnectors.bundle.directory");

        ConnectorInfoManager manager =
                getConnectorManager(syncopeConfiguration.getConfValue());

        ConnectorFacade connector = getConnectorFacade(
                manager,
                connectorTO.getBundleName(),
                connectorTO.getVersion(),
                connectorTO.getConnectorName(),
                connectorTO.getConfiguration());

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        try {

            beanFactory.destroySingleton(actual.getId().toString());

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

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConnectorInstanceTO update(HttpServletResponse response,
            @RequestBody ConnectorInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException,
            MissingConfKeyException {

        if (log.isDebugEnabled()) {
            log.debug("update called with configuration " + connectorTO);
        }

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        ConnectorInstance actual = null;

        try {
            ConnectorInstance connectorInstance =
                    binder.updateConnectorInstance(
                    connectorTO.getId(), connectorTO);

            // Everything went out fine, we can flush to the database
            actual = connectorInstanceDAO.save(
                    connectorInstance);

        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + connectorTO, e);

            throw e;
        }

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        ConnectorFacade connector = (ConnectorFacade) beanFactory.getBean(
                actual.getId().toString());

        if (connector == null) {

            SyncopeConfiguration syncopeConfiguration =
                    syncopeConfigurationDAO.find(
                    "identityconnectors.bundle.directory");

            ConnectorInfoManager manager =
                    getConnectorManager(syncopeConfiguration.getConfValue());

            connector = getConnectorFacade(
                    manager,
                    connectorTO.getBundleName(),
                    connectorTO.getVersion(),
                    connectorTO.getConnectorName(),
                    connectorTO.getConfiguration());
        }

        try {

            beanFactory.destroySingleton(actual.getId().toString());

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

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{connectorId}")
    public void delete(HttpServletResponse response,
            @PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnectorInstance connectorInstance =
                connectorInstanceDAO.find(connectorId);

        if (connectorInstance == null) {

            if (log.isErrorEnabled()) {
                log.error("Could not find connector '" + connectorId + "'");
            }

            throw new NotFoundException(String.valueOf(connectorId));

        } else {
            connectorInstanceDAO.delete(connectorId);

            ConfigurableApplicationContext context =
                    ApplicationContextManager.getApplicationContext();

            DefaultListableBeanFactory beanFactory =
                    (DefaultListableBeanFactory) context.getBeanFactory();

            try {

                beanFactory.destroySingleton(connectorId.toString());

            } catch (NoSuchBeanDefinitionException ignore) {
                // ignore exception
                if (log.isInfoEnabled()) {
                    log.info("No bean named '" + connectorId + "' is defined");
                }
            }
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public ConnectorInstanceTOs list() {

        List<ConnectorInstance> connectorInstances =
                connectorInstanceDAO.findAll();

        ConnectorInstanceTOs connectorInstanceTOs = new ConnectorInstanceTOs();

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        for (ConnectorInstance connector : connectorInstances) {
            connectorInstanceTOs.addInstance(
                    binder.getConnectorInstanceTO(connector));
        }

        return connectorInstanceTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{connectorId}")
    public ConnectorInstanceTO read(HttpServletResponse response,
            @PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnectorInstance connectorInstance =
                connectorInstanceDAO.find(connectorId);

        if (connectorInstance == null) {
            log.error("Could not find connector '" + connectorId + "'");

            throw new NotFoundException(String.valueOf(connectorId));
        }

        ConnectorInstanceDataBinder binder =
                new ConnectorInstanceDataBinder(connectorInstanceDAO);

        return binder.getConnectorInstanceTO(connectorInstance);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/check/{connectorId}")
    public ModelAndView check(HttpServletResponse response,
            @PathVariable("connectorId") Long connectorId) {

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        if (log.isDebugEnabled()) {
            log.debug("Singleton in bean factory: "
                    + beanFactory.getSingletonNames());
        }

        ConnectorFacade connector = (ConnectorFacade) beanFactory.getBean(
                connectorId.toString());

        ModelAndView mav = new ModelAndView();

        Boolean verify = Boolean.FALSE;

        try {

            if (connector != null) {
                connector.validate();
                verify = Boolean.TRUE;
            }

        } catch (RuntimeException ignore) {
            if (log.isInfoEnabled()) {
                log.info("Connector validation failed", ignore);
            }
        }

        mav.addObject(verify);

        return mav;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/getBundles")
    public ConnectorBundleTOs getBundles()
            throws NotFoundException, MissingConfKeyException {

        ConnectorBundleTOs connectorBundleTOs = new ConnectorBundleTOs();

        SyncopeConfiguration syncopeConfiguration =
                syncopeConfigurationDAO.find(
                "identityconnectors.bundle.directory");

        ConnectorInfoManager manager =
                getConnectorManager(syncopeConfiguration.getConfValue());

        List<ConnectorInfo> bundles = getBundles(manager);

        ConnectorBundleTO connectorBundleTO = null;
        ConnectorKey key = null;
        ConfigurationProperties properties = null;

        for (ConnectorInfo bundle : bundles) {

            connectorBundleTO = new ConnectorBundleTO();

            connectorBundleTO.setDisplayName(bundle.getConnectorDisplayName());

            key = bundle.getConnectorKey();

            if (log.isDebugEnabled()) {
                log.debug(
                        "\nBundle name: " + key.getBundleName()
                        + "\nBundle version: " + key.getBundleVersion()
                        + "\nBundle class: " + key.getConnectorName());
            }

            connectorBundleTO.setBundleName(key.getBundleName());
            connectorBundleTO.setConnectorName(key.getConnectorName());
            connectorBundleTO.setVersion(key.getBundleVersion());

            properties = bundle.createDefaultAPIConfiguration().
                    getConfigurationProperties();

            connectorBundleTO.setProperties(properties.getPropertyNames());

            if (log.isDebugEnabled()) {
                log.debug("Bundle properties: "
                        + connectorBundleTO.getProperties());
            }

            connectorBundleTOs.addBundle(connectorBundleTO);
        }

        return connectorBundleTOs;
    }

    public static List<ConnectorInfo> getBundles(
            ConnectorInfoManager manager) {

        List<ConnectorInfo> bundles = manager.getConnectorInfos();

        if (log.isDebugEnabled() && bundles != null) {
            log.debug("#Bundles: " + bundles.size());

            for (ConnectorInfo bundle : bundles) {
                log.debug("Bundle: " + bundle.getConnectorDisplayName());
            }
        }

        return bundles;
    }

    public static ConnectorFacade getConnectorFacade(
            ConnectorInfoManager manager,
            String bundlename,
            String bundleversion,
            String connectorname,
            Set<PropertyTO> configuration) throws NotFoundException {

        // specify a connector.
        ConnectorKey key = new ConnectorKey(
                bundlename,
                bundleversion,
                connectorname);

        if (key == null) {
            throw new NotFoundException("Connector Key");
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector.
        ConnectorInfo info = manager.findConnectorInfo(key);

        if (info == null) {
            throw new NotFoundException("Connector Info");
        }

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        if (apiConfig == null) {
            throw new NotFoundException("Default API configuration");
        }

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                apiConfig.getConfigurationProperties();

        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        // Print out what the properties are (not necessary)
        List<String> propertyNames = properties.getPropertyNames();

        for (String propName : propertyNames) {
            ConfigurationProperty prop = properties.getProperty(propName);

            if (log.isDebugEnabled()) {
                log.debug(
                        "\nProperty Name: " + prop.getName()
                        + "\nProperty Type: " + prop.getType());
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
            throw new NotFoundException("Connector");
        }

        // Make sure we have set up the Configuration properly
        connector.validate();
        //connector.test(); //needs a target resource deployed

        return connector;
    }

    public static ConnectorInfoManager getConnectorManager(
            String bundledirectory) throws NotFoundException {

        ConnectorInfoManagerFactory connectorInfoManagerFactory =
                ConnectorInfoManagerFactory.getInstance();

        File bundleDirectory = new File(bundledirectory);

        List<URL> urls = new ArrayList<URL>();

        String[] files = bundleDirectory.list();

        if (files == null) {
            throw new NotFoundException("Bundles");
        }

        for (String file : files) {
            try {
                urls.add(IOUtil.makeURL(bundleDirectory, file));
            } catch (Exception ignore) {
                // ignore exception and don't add bundle
                if (log.isDebugEnabled()) {
                    log.debug(
                            "\""
                            + bundleDirectory.toString() + "/" + file
                            + "\""
                            + " is not a valid connector bundle.", ignore);
                }
            }
        }

        if (urls.isEmpty()) {
            throw new NotFoundException("Bundles");
        }

        if (log.isDebugEnabled()) {
            log.debug("URL: " + urls.toString());
        }

        ConnectorInfoManager manager =
                connectorInfoManagerFactory.getLocalManager(
                urls.toArray(new URL[0]));

        if (manager == null) {
            throw new NotFoundException("Connector Info Manager");
        }

        return manager;
    }
}
