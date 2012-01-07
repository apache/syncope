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

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.connid.bundles.soap.WebServiceConnector;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.types.ConnConfProperty;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.AbstractTest;
import org.syncope.types.ConnConfPropSchema;

@Transactional
public class ConnInstanceTest extends AbstractTest {

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public final void findAll() {
        List<ConnInstance> connectors = connInstanceDAO.findAll();
        assertNotNull(connectors);
        assertFalse(connectors.isEmpty());
    }

    @Test
    public final void findById() {
        ConnInstance connectorInstance = connInstanceDAO.find(100L);

        assertNotNull("findById did not work", connectorInstance);

        assertEquals("invalid connector name",
                WebServiceConnector.class.getName(),
                connectorInstance.getConnectorName());

        assertEquals("invalid bundle name", "org.connid.bundles.soap",
                connectorInstance.getBundleName());

        assertEquals("invalid bundle version",
                connidSoapVersion, connectorInstance.getVersion());
    }

    @Test
    public final void save()
            throws ClassNotFoundException {

        ConnInstance connectorInstance = new ConnInstance();

        // set connector version
        connectorInstance.setVersion("1.0");

        // set connector name
        connectorInstance.setConnectorName("WebService");

        // set bundle name
        connectorInstance.setBundleName(
                "org.syncope.core.persistence.test.util");

        connectorInstance.setDisplayName("New");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.setValues(Collections.singletonList("http://host.domain"));

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        servicename.setValues(Collections.singletonList("Provisioning"));

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorInstance.setConfiguration(conf);
        assertFalse(connectorInstance.getConfiguration().isEmpty());

        // perform save operation
        ConnInstance actual = connInstanceDAO.save(connectorInstance);

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
        ConnInstance connectorInstance = connInstanceDAO.find(100L);
        assertNotNull("find to delete did not work", connectorInstance);

        connInstanceDAO.delete(connectorInstance.getId());

        ConnInstance actual = connInstanceDAO.find(100L);
        assertNull("delete did not work", actual);
    }
}
