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
package org.syncope.core.test.persistence;

import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.api.ConnectorFacade;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.PropertyTO;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.util.ApplicationContextManager;
import org.syncope.core.rest.data.ConnectorInstanceDataBinder;

@Transactional
public class ConnectorInstanceDAOTest extends AbstractTest {

    @Autowired
    ConnectorInstanceDAO connectorInstanceDAO;

    @Test
    public final void findById() {
        ConnectorInstance connectorInstance = connectorInstanceDAO.find(100L);

        assertNotNull("findById did not work", connectorInstance);

        assertEquals(
                "invalid connector name",
                "org.syncope.identityconnectors.bundles.staticwebservice.WebServiceConnector",
                connectorInstance.getConnectorName());

        assertEquals("invalid bundle name",
                "org.syncope.identityconnectors.bundles.staticws",
                connectorInstance.getBundleName());

        assertEquals("invalid bundle version",
                "0.1-SNAPSHOT", connectorInstance.getVersion());
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

        // set the connector configuration using PropertyTO
        Set<PropertyTO> conf = new HashSet<PropertyTO>();

        PropertyTO endpoint = new PropertyTO();
        endpoint.setKey("endpoint");
        endpoint.setValue("http://host.domain");

        PropertyTO servicename = new PropertyTO();
        servicename.setKey("servicename");
        servicename.setValue("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // serialize configuration
        String xmlconf = ConnectorInstanceDataBinder.serializeToXML(conf);

        assertNotNull("xml configuration string is null", xmlconf);

        // set connector configuration
        connectorInstance.setXmlConfiguration(xmlconf);

        // perform save operation
        ConnectorInstance actual =
                connectorInstanceDAO.save(connectorInstance);

        assertNotNull("save did not work", actual.getId());

        assertTrue("save did not work", actual.getId() > 100L);

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

        conf = (Set<PropertyTO>) ConnectorInstanceDataBinder.buildFromXML(
                xmlConfiguration);

        assertNotNull("configuration retrieving failed", conf);
        assertTrue(conf.size() == 2);
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
