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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ConnInstanceTest extends AbstractTest {

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public void findAll() {
        List<GrantedAuthority> authorities = StandardEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails("Master"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            List<ConnInstance> connectors = connInstanceDAO.findAll();
            assertNotNull(connectors);
            assertFalse(connectors.isEmpty());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void findById() {
        ConnInstance connInstance = connInstanceDAO.find("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNotNull(connInstance);
        assertEquals("invalid connector name",
                "net.tirasa.connid.bundles.soap.WebServiceConnector", connInstance.getConnectorName());
        assertEquals("invalid bundle name", "net.tirasa.connid.bundles.soap", connInstance.getBundleName());

        try {
            connInstanceDAO.authFind("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
            fail();
        } catch (DelegatedAdministrationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void save() throws ClassNotFoundException {
        ConnInstance connInstance = entityFactory.newEntity(ConnInstance.class);

        connInstance.setLocation(new File(System.getProperty("java.io.tmpdir")).toURI().toString());

        // set connector version
        connInstance.setVersion("1.0");

        // set connector name
        connInstance.setConnectorName("WebService");

        // set bundle name
        connInstance.setBundleName("org.apache.syncope.core.persistence.test.util");

        connInstance.setDisplayName("New");

        connInstance.setConnRequestTimeout(60);

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.getValues().add("http://host.domain");

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        endpoint.getValues().add("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connInstance.setConf(conf);
        assertFalse(connInstance.getConf().isEmpty());

        // perform save operation
        ConnInstance actual = connInstanceDAO.save(connInstance);

        assertNotNull("save did not work", actual.getKey());

        assertEquals("save did not work for \"name\" attribute", "WebService", actual.getConnectorName());

        assertEquals("save did not work for \"bundle name\" attribute", "org.apache.syncope.core.persistence.test.util",
                actual.getBundleName());

        assertEquals("save did not work for \"majorVersion\" attribute", "1.0", connInstance.getVersion());

        assertEquals("New", actual.getDisplayName());

        assertEquals(60, actual.getConnRequestTimeout(), 0);

        conf = connInstance.getConf();
        assertFalse(conf.isEmpty());

        assertNotNull("configuration retrieving failed", conf);
        assertTrue(conf.size() == 2);
    }

    @Test
    public void delete() {
        ConnInstance connectorInstance = connInstanceDAO.find("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNotNull("find to delete did not work", connectorInstance);

        connInstanceDAO.delete(connectorInstance.getKey());

        ConnInstance actual = connInstanceDAO.find("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNull("delete did not work", actual);
    }
}
