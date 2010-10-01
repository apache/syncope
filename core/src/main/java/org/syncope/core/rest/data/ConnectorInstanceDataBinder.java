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
package org.syncope.core.rest.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.PropertyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.ConnectorInstanceLoader;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
@Transactional(rollbackFor = {Throwable.class})
public class ConnectorInstanceDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ConnectorInstanceDataBinder.class);
    private static final String[] ignoreProperties = {
        "id", "resources", "displayName"};
    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;

    public ConnectorInstance getConnectorInstance(
            ConnectorInstanceTO connectorInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorInstanceTO.getBundleName() == null) {
            requiredValuesMissing.addElement("bundlename");
        }

        if (connectorInstanceTO.getVersion() == null) {
            requiredValuesMissing.addElement("bundleversion");
        }

        if (connectorInstanceTO.getConnectorName() == null) {
            requiredValuesMissing.addElement("connectorname");
        }

        if (connectorInstanceTO.getConfiguration() == null
                || connectorInstanceTO.getConfiguration().isEmpty()) {
            requiredValuesMissing.addElement("configuration");
        }

        ConnectorInstance connectorInstance = new ConnectorInstance();

        BeanUtils.copyProperties(
                connectorInstanceTO, connectorInstance, ignoreProperties);
        connectorInstance.getCapabilities().addAll(
                connectorInstanceTO.getCapabilities());

        connectorInstance.setXmlConfiguration(
                ConnectorInstanceLoader.serializeToXML(
                connectorInstanceTO.getConfiguration()));

        // Throw composite exception if there is at least one element set
        // in the composing exceptions

        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connectorInstance;
    }

    public ConnectorInstance updateConnectorInstance(
            Long connectorInstanceId,
            ConnectorInstanceTO connectorInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorInstanceId == null) {
            requiredValuesMissing.addElement("connector id");
        }

        ConnectorInstance connectorInstance =
                connectorInstanceDAO.find(connectorInstanceId);

        if (connectorInstanceTO.getBundleName() != null) {
            connectorInstance.setBundleName(
                    connectorInstanceTO.getBundleName());
        }

        if (connectorInstanceTO.getVersion() != null) {
            connectorInstance.setVersion(connectorInstanceTO.getVersion());
        }

        if (connectorInstanceTO.getConnectorName() != null) {
            connectorInstance.setConnectorName(
                    connectorInstanceTO.getConnectorName());
        }

        if (connectorInstanceTO.getConfiguration() != null
                || connectorInstanceTO.getConfiguration().isEmpty()) {

            connectorInstance.setXmlConfiguration(
                    ConnectorInstanceLoader.serializeToXML(
                    connectorInstanceTO.getConfiguration()));
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(URLEncoder.encode(
                        ConnectorInstanceLoader.serializeToXML(
                        connectorInstanceTO.getConfiguration()),
                        "UTF-8"));
            }
        } catch (UnsupportedEncodingException ex) {
            LOG.error("Unexpected exception", ex);
        }

        connectorInstance.setCapabilities(
                connectorInstanceTO.getCapabilities());

        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connectorInstance;
    }

    public ConnectorInstanceTO getConnectorInstanceTO(
            ConnectorInstance connectorInstance) {

        ConnectorInstanceTO connectorInstanceTO =
                new ConnectorInstanceTO();

        BeanUtils.copyProperties(
                connectorInstance, connectorInstanceTO, ignoreProperties);
        connectorInstanceTO.getCapabilities().addAll(
                connectorInstance.getCapabilities());

        connectorInstanceTO.setConfiguration(
                (Set<PropertyTO>) ConnectorInstanceLoader.buildFromXML(
                connectorInstance.getXmlConfiguration()));

        connectorInstanceTO.setId(connectorInstance.getId());

        return connectorInstanceTO;
    }
}
