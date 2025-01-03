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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ConnInstanceTest extends AbstractTest {

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public void findAll() {
        List<GrantedAuthority> authorities = IdMEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            List<? extends ConnInstance> connectors = connInstanceDAO.findAll();
            assertNotNull(connectors);
            assertFalse(connectors.isEmpty());
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test
    public void findById() {
        ConnInstance connInstance = connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").orElseThrow();
        assertNotNull(connInstance);
        assertEquals("net.tirasa.connid.bundles.soap.WebServiceConnector", connInstance.getConnectorName());
        assertEquals("net.tirasa.connid.bundles.soap", connInstance.getBundleName());

        try {
            connInstanceDAO.authFind("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
            fail("This should not happen");
        } catch (DelegatedAdministrationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void save() {
        ConnInstance connInstance = entityFactory.newEntity(ConnInstance.class);
        connInstance.setLocation("file:" + System.getProperty("java.io.tmpdir"));
        connInstance.setVersion("1.0");
        connInstance.setConnectorName("WebService");
        connInstance.setBundleName("org.apache.syncope.core.persistence.test.util");
        connInstance.setDisplayName("New");
        connInstance.setConnRequestTimeout(60);

        // set the connector configuration
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

        List<ConnConfProperty> conf = new ArrayList<>();
        conf.add(endpoint);
        conf.add(servicename);
        connInstance.setConf(conf);
        assertFalse(connInstance.getConf().isEmpty());

        // perform save operation
        ConnInstance actual = connInstanceDAO.save(connInstance);

        assertNotNull("save did not work", actual::getKey);

        assertEquals("WebService", actual.getConnectorName());

        assertEquals("org.apache.syncope.core.persistence.test.util", actual.getBundleName());

        assertEquals("1.0", connInstance.getVersion());

        assertEquals("New", actual.getDisplayName());

        assertEquals(60, actual.getConnRequestTimeout().intValue());

        assertEquals(2, connInstance.getConf().size());
    }

    @Test
    public void delete() {
        ConnInstance connInstance = connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").orElseThrow();

        connInstanceDAO.deleteById(connInstance.getKey());

        assertTrue(connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").isEmpty());
    }
}
