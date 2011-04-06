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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ConnInstanceDataBinder {

    private static final String[] ignoreProperties = {
        "id", "resources"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

    public ConnInstance getConnInstance(
            final ConnInstanceTO connectorInstanceTO)
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

        ConnInstance connectorInstance = new ConnInstance();

        BeanUtils.copyProperties(
                connectorInstanceTO, connectorInstance, ignoreProperties);

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

    public ConnInstance updateConnInstance(
            Long connectorInstanceId,
            ConnInstanceTO connectorInstanceTO)
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

        ConnInstance connectorInstance =
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

            connectorInstance.setConfiguration(
                    connectorInstanceTO.getConfiguration());
        }

        if (connectorInstanceTO.getDisplayName() != null) {
            connectorInstance.setDisplayName(
                    connectorInstanceTO.getDisplayName());
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

    public ConnInstanceTO getConnInstanceTO(
            ConnInstance connectorInstance) {

        ConnInstanceTO connectorInstanceTO =
                new ConnInstanceTO();

        BeanUtils.copyProperties(
                connectorInstance, connectorInstanceTO, ignoreProperties);

        connectorInstanceTO.setId(connectorInstance.getId());

        return connectorInstanceTO;
    }
}
