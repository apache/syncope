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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConnInstanceDataBinderTest extends AbstractDataBinderTest {

    private static final String CONNECTOR_KEY = "88a7a819-dab5-46b4-9b90-0b9769eabdb8";

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    protected ConnConfProperty confProperty(
            final String name, final Object value, final boolean confidential) {

        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setName(name);
        schema.setDisplayName(name);
        schema.setHelpMessage(name + " help");
        schema.setType(value.getClass().getName());
        schema.setRequired(true);
        schema.setOrder(1);
        schema.setConfidential(confidential);

        ConnConfProperty property = new ConnConfProperty();
        property.setSchema(schema);
        property.setOverridable(true);
        property.getValues().add(value);
        return property;
    }

    @Test
    void working() {
        ConnInstanceTO connInstance = binder.getConnInstanceTO(
                connInstanceDAO.findById(CONNECTOR_KEY).orElseThrow());
        assertNotNull(connInstance);
        assertFalse(connInstance.isErrored());
        assertNotNull(connInstance.getLocation());
        assertFalse(connInstance.getConf().isEmpty());
        assertEquals("ConnInstance100", connInstance.getDisplayName());
        assertEquals(SyncopeConstants.ROOT_REALM, connInstance.getAdminRealm());
    }

    @Test
    void errored() {
        ConnInstanceTO connInstance = binder.getConnInstanceTO(
                connInstanceDAO.findById("413bf072-678a-41d3-9d20-8c453b3a39d1").orElseThrow());
        assertNotNull(connInstance);
        assertTrue(connInstance.isErrored());
        assertNotNull(connInstance.getLocation());
        assertTrue(connInstance.getConf().isEmpty());
    }

    @Test
    void create() {
        ConnPoolConf poolConf = new ConnPoolConf();
        poolConf.setMaxObjects(10);
        poolConf.setMaxIdle(5);
        poolConf.setMinIdle(2);
        poolConf.setMaxWait(1000L);
        poolConf.setMinEvictableIdleTimeMillis(2000L);

        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setLocation("connid://localhost");
        connInstanceTO.setBundleName("net.tirasa.connid.bundles.soap");
        connInstanceTO.setConnectorName("net.tirasa.connid.bundles.soap.WebServiceConnector");
        connInstanceTO.setVersion("1.5.0");
        connInstanceTO.setDisplayName("Test connector");
        connInstanceTO.setAdminRealm("/even");
        connInstanceTO.setConnRequestTimeout(30);
        connInstanceTO.setPoolConf(poolConf);
        connInstanceTO.getCapabilities().addAll(
                Set.of(ConnectorCapability.CREATE, ConnectorCapability.UPDATE, ConnectorCapability.SEARCH));
        connInstanceTO.getConf().add(confProperty("endpoint", "http://localhost:8080/soap", false));
        connInstanceTO.getConf().add(confProperty("servicename", "Provisioning", false));

        ConnInstance connInstance = binder.create(connInstanceTO);
        connInstance = connInstanceDAO.save(connInstance);
        entityManager.flush();

        ConnInstanceTO actual = binder.getConnInstanceTO(
                connInstanceDAO.findById(connInstance.getKey()).orElseThrow());
        assertEquals("connid://localhost", actual.getLocation());
        assertEquals("net.tirasa.connid.bundles.soap", actual.getBundleName());
        assertEquals("net.tirasa.connid.bundles.soap.WebServiceConnector", actual.getConnectorName());
        assertEquals("1.5.0", actual.getVersion());
        assertEquals("Test connector", actual.getDisplayName());
        assertEquals("/even", actual.getAdminRealm());
        assertEquals(30, actual.getConnRequestTimeout());
        assertEquals(Set.of(ConnectorCapability.CREATE, ConnectorCapability.UPDATE,
                ConnectorCapability.SEARCH), actual.getCapabilities());
        assertEquals(2, actual.getConf().size());
        assertEquals("http://localhost:8080/soap",
                actual.getConf().getFirst().getValues().getFirst());
        assertNotNull(actual.getPoolConf());
        assertEquals(10, actual.getPoolConf().getMaxObjects());
        assertEquals(5, actual.getPoolConf().getMaxIdle());
        assertEquals(2, actual.getPoolConf().getMinIdle());
        assertEquals(1000L, actual.getPoolConf().getMaxWait());
        assertEquals(2000L, actual.getPoolConf().getMinEvictableIdleTimeMillis());
    }

    @Test
    void update() {
        ConnInstance variant = connInstanceDAO.findById(CONNECTOR_KEY).orElseThrow();
        ConnInstanceTO connInstanceTO = binder.getConnInstanceTO(variant);

        ConnPoolConf poolConf = new ConnPoolConf();
        poolConf.setMaxObjects(20);
        connInstanceTO.setPoolConf(poolConf);

        connInstanceTO.setLocation("connid://updated");
        connInstanceTO.setBundleName("net.tirasa.connid.bundles.db");
        connInstanceTO.setConnectorName("net.tirasa.connid.bundles.db.table.DatabaseTableConnector");
        connInstanceTO.setVersion("2.5.0");
        connInstanceTO.setDisplayName("Updated connector");
        connInstanceTO.setAdminRealm("/even/two");
        connInstanceTO.setConnRequestTimeout(60);
        connInstanceTO.getCapabilities().clear();
        connInstanceTO.getCapabilities().addAll(Set.of(ConnectorCapability.AUTHENTICATE));
        connInstanceTO.getConf().clear();
        connInstanceTO.getConf().add(confProperty(AnyTypeKind.USER.name(), "sa", false));
        connInstanceTO.getConf().add(confProperty("keyColumn", "id", false));

        binder.update(connInstanceTO);
        entityManager.flush();

        ConnInstanceTO actual = binder.getConnInstanceTO(
                connInstanceDAO.findById(CONNECTOR_KEY).orElseThrow());
        assertEquals("connid://updated", actual.getLocation());
        assertEquals("net.tirasa.connid.bundles.db", actual.getBundleName());
        assertEquals("net.tirasa.connid.bundles.db.table.DatabaseTableConnector", actual.getConnectorName());
        assertEquals("2.5.0", actual.getVersion());
        assertEquals("Updated connector", actual.getDisplayName());
        assertEquals("/even/two", actual.getAdminRealm());
        assertEquals(60, actual.getConnRequestTimeout());
        assertEquals(Set.of(ConnectorCapability.AUTHENTICATE), actual.getCapabilities());
        assertEquals(2, actual.getConf().size());
        assertEquals("sa", actual.getConf().getFirst().getValues().getFirst());
        assertNotNull(actual.getPoolConf());
        assertEquals(20, actual.getPoolConf().getMaxObjects());

        // setting no pool conf removes it
        actual.setPoolConf(null);
        binder.update(actual);
        entityManager.flush();

        ConnInstanceTO withoutPool = binder.getConnInstanceTO(
                connInstanceDAO.findById(CONNECTOR_KEY).orElseThrow());
        assertNull(withoutPool.getPoolConf());
    }

    @Test
    void invalid() {
        ConnInstanceTO missing = new ConnInstanceTO();
        assertThrows(SyncopeClientException.class, () -> binder.create(missing));

        ConnInstanceTO invalidRealm = new ConnInstanceTO();
        invalidRealm.setLocation("connid://localhost");
        invalidRealm.setBundleName("net.tirasa.connid.bundles.soap");
        invalidRealm.setConnectorName("net.tirasa.connid.bundles.soap.WebServiceConnector");
        invalidRealm.setVersion("1.5.0");
        invalidRealm.setAdminRealm("/even/notexisting");
        invalidRealm.getConf().add(confProperty("endpoint", "http://localhost:8080/soap", false));
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidRealm));

        ConnInstanceTO valid = binder.getConnInstanceTO(
                connInstanceDAO.findById(CONNECTOR_KEY).orElseThrow());
        valid.setAdminRealm("/even/notexisting");
        assertThrows(SyncopeClientException.class, () -> binder.update(valid));
    }
}
