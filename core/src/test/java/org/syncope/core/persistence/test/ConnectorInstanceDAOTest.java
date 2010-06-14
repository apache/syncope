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
package org.syncope.core.persistence.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.test.util.WebServiceConfiguration;

@Transactional
public class ConnectorInstanceDAOTest extends AbstractDAOTest {

    @Autowired
    ConnectorInstanceDAO connectorInstanceDAO;

    @Test
    public final void findById() {
        ConnectorInstance connectorInstance = connectorInstanceDAO.find(100L);

        assertNotNull("findById did not work", connectorInstance);

        assertEquals("invalid name", "OpenAM",
                connectorInstance.getConnectorName());

        assertEquals("invalid name", "org.syncope.core.persistence.test.util.openam",
                connectorInstance.getBundleName());

        assertEquals("invalid version",
                "1.0", connectorInstance.getVersion());
    }

    @Test
    public final void save() throws ClassNotFoundException {
        ConnectorInstance connectorInstance = new ConnectorInstance();

        // set connector version
        connectorInstance.setVersion("1.0");

        // set connector name
        connectorInstance.setConnectorName("WebService");

        // set bundle name
        connectorInstance.setBundleName(
                "org.syncope.core.persistence.test.util");

        WebServiceConfiguration conf = new WebServiceConfiguration();
        conf.setEndpoint("http://host.domain");
        conf.setService("/provisioning");
        conf.setContext("/service");

        String xmlconf = conf.serializeToXML();

        assertNotNull("xml configuration string is null", xmlconf);

        // set connector configuration
        connectorInstance.setXmlConfiguration(xmlconf);

        // perform save operation
        ConnectorInstance actual =
                connectorInstanceDAO.save(connectorInstance);

        assertNotNull("save did not work", actual.getId());

        assertEquals("save did not work",
                Long.valueOf(101L), actual.getId());

        assertEquals("save did not work for \"name\" attribute",
                "WebService",
                actual.getConnectorName());

        assertEquals("save did not work for \"bundle name\" attribute",
                "org.syncope.core.persistence.test.util",
                actual.getBundleName());

        assertEquals("save did not work for \"majorVersion\" attribute",
                "1.0", connectorInstance.getVersion());

        String xmlConfiguration = connectorInstance.getXmlConfiguration();

        assertNotNull("configuration not found", xmlConfiguration);

        conf = (WebServiceConfiguration) WebServiceConfiguration.buildFromXML(
                xmlConfiguration);

        assertNotNull("configuration retrieving failed", conf);

        Throwable t = null;

        try {
            conf.validate();
        } catch (IllegalArgumentException e) {
            t = e;
        }

        assertNull("configuration validation failed", t);
    }

    @Test
    public final void delete() {
        ConnectorInstance connectorInstance = connectorInstanceDAO.find(100L);

        assertNotNull("find to delete did not work", connectorInstance);

        connectorInstanceDAO.delete(connectorInstance.getId());

        ConnectorInstance actual = connectorInstanceDAO.find(100L);
        assertNull("delete did not work", actual);
    }
}
