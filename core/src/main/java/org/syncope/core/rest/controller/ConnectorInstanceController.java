
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConnectorBundleTO;
import org.syncope.client.to.ConnectorBundleTOs;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ConnectorInstanceTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.ConnectorInstanceLoader;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.data.ConnectorInstanceDataBinder;

@Controller
@RequestMapping("/connector")
public class ConnectorInstanceController extends AbstractController {

    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;
    @Autowired
    private ConnectorInstanceDataBinder binder;

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConnectorInstanceTO create(HttpServletResponse response,
            @RequestBody ConnectorInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException,
            MissingConfKeyException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Create called with configuration " + connectorTO);
        }

        ConnectorInstance connectorInstance = null;
        try {
            connectorInstance = binder.getConnectorInstance(connectorTO);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("Could not create for " + connectorTO, e);

            throw e;
        }

        // Everything went out fine, we can flush to the database
        // and register the new connector instance for later usage
        connectorInstance = connectorInstanceDAO.save(connectorInstance);

        return binder.getConnectorInstanceTO(connectorInstance);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConnectorInstanceTO update(HttpServletResponse response,
            @RequestBody ConnectorInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException,
            MissingConfKeyException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("update called with configuration " + connectorTO);
        }

        ConnectorInstance connectorInstance = null;
        try {
            connectorInstance = binder.updateConnectorInstance(
                    connectorTO.getId(), connectorTO);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("Could not create for " + connectorTO, e);

            throw e;
        }

        // Everything went out fine, we can flush to the database
        // and register the new connector instance for later usage
        connectorInstance = connectorInstanceDAO.save(connectorInstance);

        return binder.getConnectorInstanceTO(connectorInstance);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{connectorId}")
    public void delete(HttpServletResponse response,
            @PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnectorInstance connectorInstance =
                connectorInstanceDAO.find(connectorId);

        if (connectorInstance == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not find connector '" + connectorId + "'");
            }

            throw new NotFoundException(String.valueOf(connectorId));
        }

        connectorInstanceDAO.delete(connectorId);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public ConnectorInstanceTOs list() {

        List<ConnectorInstance> connectorInstances =
                connectorInstanceDAO.findAll();

        ConnectorInstanceTOs connectorInstanceTOs = new ConnectorInstanceTOs();

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
            LOG.error("Could not find connector '" + connectorId + "'");

            throw new NotFoundException(String.valueOf(connectorId));
        }

        return binder.getConnectorInstanceTO(connectorInstance);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/check/{connectorId}")
    public ModelAndView check(HttpServletResponse response,
            @PathVariable("connectorId") String connectorId) {

        ConnectorFacadeProxy connector =
                ConnectorInstanceLoader.getConnector(connectorId);

        ModelAndView mav = new ModelAndView();

        Boolean verify = Boolean.FALSE;
        try {
            if (connector != null) {
                connector.validate();
                verify = Boolean.TRUE;
            }
        } catch (RuntimeException ignore) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Connector validation failed", ignore);
            }
        }

        mav.addObject(verify);

        return mav;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/getBundles")
    public ConnectorBundleTOs getBundles()
            throws NotFoundException, MissingConfKeyException {

        ConnectorInfoManager manager =
                ConnectorInstanceLoader.getConnectorManager();

        List<ConnectorInfo> bundles = manager.getConnectorInfos();

        if (LOG.isDebugEnabled() && bundles != null) {
            LOG.debug("#Bundles: " + bundles.size());

            for (ConnectorInfo bundle : bundles) {
                LOG.debug("Bundle: " + bundle.getConnectorDisplayName());
            }
        }

        ConnectorBundleTO connectorBundleTO = null;
        ConnectorKey key = null;
        ConfigurationProperties properties = null;

        ConnectorBundleTOs connectorBundleTOs = new ConnectorBundleTOs();
        for (ConnectorInfo bundle : bundles) {
            connectorBundleTO = new ConnectorBundleTO();
            connectorBundleTO.setDisplayName(bundle.getConnectorDisplayName());

            key = bundle.getConnectorKey();

            if (LOG.isDebugEnabled()) {
                LOG.debug("\nBundle name: " + key.getBundleName()
                        + "\nBundle version: " + key.getBundleVersion()
                        + "\nBundle class: " + key.getConnectorName());
            }

            connectorBundleTO.setBundleName(key.getBundleName());
            connectorBundleTO.setConnectorName(key.getConnectorName());
            connectorBundleTO.setVersion(key.getBundleVersion());

            properties = bundle.createDefaultAPIConfiguration().
                    getConfigurationProperties();

            connectorBundleTO.setProperties(properties.getPropertyNames());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Bundle properties: "
                        + connectorBundleTO.getProperties());
            }

            connectorBundleTOs.addBundle(connectorBundleTO);
        }

        return connectorBundleTOs;
    }
}
