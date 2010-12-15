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
package org.syncope.core.persistence.dao;

import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.PropertyTO;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.identityconnectors.bundles.staticwebservice.WebServiceConnector;

@Transactional
public class ConnectorInstanceTest extends AbstractTest {

    @Autowired
    private ConnectorInstanceDAO connectorInstanceDAO;

    @Test
    public final void findById() {
        ConnectorInstance connectorInstance = connectorInstanceDAO.find(100L);

        assertNotNull("findById did not work", connectorInstance);

        assertEquals("invalid connector name",
                WebServiceConnector.class.getName(),
                connectorInstance.getConnectorName());

        assertEquals("invalid bundle name",
                "org.syncope.identityconnectors.bundles.staticws",
                connectorInstance.getBundleName());

        assertEquals("invalid bundle version",
                bundlesVersion, connectorInstance.getVersion());
    }

    @Test
    public final void save()
            throws ClassNotFoundException {
        ConnectorInstance connectorInstance = new ConnectorInstance();

        // set connector version
        connectorInstance.setVersion("1.0");

        // set connector name
        connectorInstance.setConnectorName("WebService");

        // set bundle name
        connectorInstance.setBundleName(
                "org.syncope.core.persistence.test.util");

        connectorInstance.setDisplayName("New");

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

        // set connector configuration
        connectorInstance.setConfiguration(conf);
        assertFalse(connectorInstance.getConfiguration().isEmpty());

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

        assertEquals("New", actual.getDisplayName());

        conf = connectorInstance.getConfiguration();
        assertFalse(conf.isEmpty());

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
