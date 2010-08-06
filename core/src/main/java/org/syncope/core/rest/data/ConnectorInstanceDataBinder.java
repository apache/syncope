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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.PropertyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ConnectorInstanceDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            ConnectorInstanceDataBinder.class);
    private static final String[] ignoreProperties = {
        "id", "resources", "xmlConfiguration", "configuration"};
    private ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    public ConnectorInstanceDataBinder(
            ConnectorInstanceDAO connectorInstanceDAO) {
        this.connectorInstanceDAO = connectorInstanceDAO;
    }

    public ConnectorInstance getConnectorInstance(
            ConnectorInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorTO.getBundleName() == null) {
            requiredValuesMissing.addElement("bundlename");
        }

        if (connectorTO.getVersion() == null) {
            requiredValuesMissing.addElement("bundleversion");
        }

        if (connectorTO.getConnectorName() == null) {
            requiredValuesMissing.addElement("connectorname");
        }

        if (connectorTO.getConfiguration() == null
                || connectorTO.getConfiguration().isEmpty()) {
            requiredValuesMissing.addElement("configuration");
        }

        ConnectorInstance connectorInstance = new ConnectorInstance();

        BeanUtils.copyProperties(
                connectorTO, connectorInstance, ignoreProperties);

        connectorInstance.setXmlConfiguration(
                serializeToXML(
                connectorTO.getConfiguration()));

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
            ConnectorInstanceTO connectorTO)
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


        if (connectorTO.getBundleName() != null) {
            connectorInstance.setBundleName(connectorTO.getBundleName());
        }

        if (connectorTO.getVersion() != null) {
            connectorInstance.setVersion(connectorTO.getVersion());
        }

        if (connectorTO.getConnectorName() != null) {
            connectorInstance.setConnectorName(connectorTO.getConnectorName());
        }

        if (connectorTO.getConfiguration() != null
                || connectorTO.getConfiguration().isEmpty()) {

            connectorInstance.setXmlConfiguration(
                    serializeToXML(
                    connectorTO.getConfiguration()));
        }

        try {
            log.error(URLEncoder.encode(
                    serializeToXML(connectorTO.getConfiguration()), "UTF-8"));
            // Throw composite exception if there is at least one element set
            // in the composing exceptions
        } catch (UnsupportedEncodingException ex) {
            log.error("Unexpected exception", ex);
        }

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

    public ConnectorInstanceTO getConnectorInstanceTO(
            ConnectorInstance connectorInstance) {
        
        ConnectorInstanceTO connectorInstanceTO =
                new ConnectorInstanceTO();

        BeanUtils.copyProperties(
                connectorInstance, connectorInstanceTO, ignoreProperties);

        connectorInstanceTO.setConfiguration(
                (Set<PropertyTO>) buildFromXML(
                connectorInstance.getXmlConfiguration()));

        connectorInstanceTO.setId(connectorInstance.getId());

        return connectorInstanceTO;
    }

    public static String serializeToXML(Object obj) {
        try {

            ByteArrayOutputStream tokenContentOS = new ByteArrayOutputStream();
            XMLEncoder encoder = new XMLEncoder(tokenContentOS);
            encoder.writeObject(obj);
            encoder.flush();
            encoder.close();

            String res = tokenContentOS.toString();

            return URLEncoder.encode(res, "UTF-8");
        } catch (Throwable t) {
            if (log.isInfoEnabled()) {
                log.info("Exception during connector serialization", t);
            }
            return null;
        }
    }

    public static Object buildFromXML(String xml) {
        try {

            ByteArrayInputStream tokenContentIS = new ByteArrayInputStream(
                    URLDecoder.decode(xml, "UTF-8").getBytes());

            XMLDecoder decoder = new XMLDecoder(tokenContentIS);
            Object object = decoder.readObject();
            decoder.close();

            return object;

        } catch (Throwable t) {
            if (log.isInfoEnabled()) {
                log.info("Exception during connector serialization", t);
            }
            return null;
        }
    }
}
