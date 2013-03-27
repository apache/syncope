/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ConnInstanceTest extends AbstractDAOTest {

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public void findAll() {
        List<ConnInstance> connectors = connInstanceDAO.findAll();
        assertNotNull(connectors);
        assertFalse(connectors.isEmpty());
    }

    @Test
    public void findById() {
        ConnInstance connectorInstance = connInstanceDAO.find(100L);

        assertNotNull("findById did not work", connectorInstance);

        assertEquals("invalid connector name",
                "org.connid.bundles.soap.WebServiceConnector", connectorInstance.getConnectorName());

        assertEquals("invalid bundle name", "org.connid.bundles.soap", connectorInstance.getBundleName());

        assertEquals("invalid bundle version", connidSoapVersion, connectorInstance.getVersion());
    }

    @Test
    public void save() throws ClassNotFoundException {
        ConnInstance connInstance = new ConnInstance();

        connInstance.setLocation(new File("java.io.tmpdir").toURI().toString());

        // set connector version
        connInstance.setVersion("1.0");

        // set connector name
        connInstance.setConnectorName("WebService");

        // set bundle name
        connInstance.setBundleName("org.apache.syncope.core.persistence.test.util");

        connInstance.setDisplayName("New");

        connInstance.setConnRequestTimeout(60);

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
        connInstance.setConfiguration(conf);
        assertFalse(connInstance.getConfiguration().isEmpty());

        // perform save operation
        ConnInstance actual = connInstanceDAO.save(connInstance);

        assertNotNull("save did not work", actual.getId());

        assertTrue("save did not work", actual.getId() > 100L);

        assertEquals("save did not work for \"name\" attribute", "WebService", actual.getConnectorName());

        assertEquals("save did not work for \"bundle name\" attribute", "org.apache.syncope.core.persistence.test.util",
                actual.getBundleName());

        assertEquals("save did not work for \"majorVersion\" attribute", "1.0", connInstance.getVersion());

        assertEquals("New", actual.getDisplayName());

        assertEquals(new Integer(60), actual.getConnRequestTimeout());

        conf = connInstance.getConfiguration();
        assertFalse(conf.isEmpty());

        assertNotNull("configuration retrieving failed", conf);
        assertTrue(conf.size() == 2);
    }

    @Test
    public void delete() {
        ConnInstance connectorInstance = connInstanceDAO.find(100L);
        assertNotNull("find to delete did not work", connectorInstance);

        connInstanceDAO.delete(connectorInstance.getId());

        ConnInstance actual = connInstanceDAO.find(100L);
        assertNull("delete did not work", actual);
    }
}
