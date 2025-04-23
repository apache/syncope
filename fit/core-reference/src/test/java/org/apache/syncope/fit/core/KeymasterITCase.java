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
package org.apache.syncope.fit.core;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class KeymasterITCase extends AbstractITCase {

    @Test
    public void confParamList() {
        Map<String, Object> confParams = confParamOps.list(SyncopeConstants.MASTER_DOMAIN);
        assertNotNull(confParams);
        assertFalse(confParams.isEmpty());
    }

    @Test
    public void confParamGet() {
        String stringValue = confParamOps.get(
                SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", null, String.class);
        assertNotNull(stringValue);
        assertEquals("SHA1", stringValue);

        Long longValue = confParamOps.get(
                SyncopeConstants.MASTER_DOMAIN, "jwt.lifetime.minutes", null, Long.class);
        assertNotNull(longValue);
        assertEquals(120L, longValue.longValue());

        Boolean booleanValue = confParamOps.get(
                SyncopeConstants.MASTER_DOMAIN, "return.password.value", null, Boolean.class);
        assertNotNull(booleanValue);
        assertEquals(false, booleanValue);

        List<String> stringValues =
                List.of(confParamOps.get(
                        SyncopeConstants.MASTER_DOMAIN, "authentication.attributes", null, String[].class));
        assertNotNull(stringValues);
        List<String> actualStringValues = new ArrayList<>();
        actualStringValues.add("username");
        actualStringValues.add("userId");
        assertEquals(actualStringValues, stringValues);
    }

    @Test
    public void confParamSetGetRemove() {
        String key = UUID.randomUUID().toString();

        String stringValue = "stringValue";
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, stringValue);
        String actualStringValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, String.class);
        assertEquals(stringValue, actualStringValue);

        Long longValue = 1L;
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, longValue);
        Long actualLongValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Long.class);
        assertEquals(longValue, actualLongValue);

        Double doubleValue = 2.0;
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, doubleValue);
        Double actualDoubleValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Double.class);
        assertEquals(doubleValue, actualDoubleValue);

        Date dateValue = new Date();
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, dateValue);
        Date actualDateValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Date.class);
        assertEquals(dateValue, actualDateValue);

        Boolean booleanValue = true;
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, booleanValue);
        Boolean actualBooleanValue = confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, Boolean.class);
        assertEquals(booleanValue, actualBooleanValue);

        List<String> stringValues = new ArrayList<>();
        stringValues.add("stringValue1");
        stringValues.add("stringValue2");
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, key, stringValues);
        List<String> actualStringValues =
                List.of(confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, String[].class));
        assertEquals(stringValues, actualStringValues);

        confParamOps.remove(SyncopeConstants.MASTER_DOMAIN, key);
        assertNull(confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, null, String.class));
        assertEquals(
                "defaultValue",
                confParamOps.get(SyncopeConstants.MASTER_DOMAIN, key, "defaultValue", String.class));
    }

    @Test
    public void serviceList() {
        List<NetworkService> services = serviceOps.list(NetworkService.Type.CORE);
        assertFalse(services.isEmpty());
        assertEquals(1, services.size());

        services = serviceOps.list(NetworkService.Type.SRA);
        assertTrue(services.isEmpty());

        services = serviceOps.list(NetworkService.Type.WA);
        assertTrue(services.isEmpty());
    }

    private List<NetworkService> findNetworkServices(
            final NetworkService.Type type,
            final Function<List<NetworkService>, Boolean> check,
            final int maxWaitSeconds) {

        Mutable<List<NetworkService>> holder = new MutableObject<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                holder.setValue(serviceOps.list(type));
                return !check.apply(holder.getValue());
            } catch (Exception e) {
                return false;
            }
        });
        return holder.getValue();
    }

    @Test
    public void serviceRun() {
        List<NetworkService> list = serviceOps.list(NetworkService.Type.SRA);
        assertTrue(list.isEmpty());

        NetworkService sra1 = new NetworkService();
        sra1.setType(NetworkService.Type.SRA);
        sra1.setAddress("http://localhost:9080/syncope-sra");
        serviceOps.register(sra1);

        list = findNetworkServices(NetworkService.Type.SRA, List::isEmpty, 30);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(sra1, list.getFirst());

        assertEquals(sra1, serviceOps.get(NetworkService.Type.SRA));

        NetworkService sra2 = new NetworkService();
        sra2.setType(NetworkService.Type.SRA);
        sra2.setAddress("http://localhost:9080/syncope-sra");
        assertEquals(sra1, sra2);
        serviceOps.register(sra2);

        list = findNetworkServices(NetworkService.Type.SRA, List::isEmpty, 30);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(sra1, list.getFirst());

        assertEquals(sra1, serviceOps.get(NetworkService.Type.SRA));

        serviceOps.unregister(sra1);
        list = findNetworkServices(NetworkService.Type.SRA, l -> !l.isEmpty(), 30);
        assertTrue(list.isEmpty());

        try {
            serviceOps.get(NetworkService.Type.SRA);
            fail();
        } catch (KeymasterException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void domainCRUD() throws Exception {
        List<Domain> initial = domainOps.list();
        assertNotNull(initial);
        assumeTrue(initial.stream().anyMatch(domain -> "Two".equals(domain.getKey())));
        assumeTrue(initial.getFirst() instanceof JPADomain);

        JPADomain two = (JPADomain) initial.getFirst();
        String newdb = "syncopetest";

        // 1. create new domain
        String newDomain = UUID.randomUUID().toString();

        domainOps.create(new JPADomain.Builder(newDomain).
                jdbcDriver(two.getJdbcDriver()).
                jdbcURL(two.getJdbcURL().replace("/syncopetwo", "/" + newdb)).
                dbSchema("SYNCOPETWO".equals(two.getDbSchema()) ? newdb.toUpperCase() : null).
                dbUsername(newdb).
                dbPassword(newdb).
                databasePlatform(two.getDatabasePlatform()).
                orm(two.getOrm()).
                transactionIsolation(two.getTransactionIsolation()).
                adminPassword(encryptorManager.getInstance().encode("password", CipherAlgorithm.BCRYPT)).
                adminCipherAlgorithm(CipherAlgorithm.BCRYPT).
                build());

        JPADomain domain = (JPADomain) domainOps.read(newDomain);
        assertEquals(two.getTransactionIsolation(), domain.getTransactionIsolation());
        assertEquals(CipherAlgorithm.BCRYPT, domain.getAdminCipherAlgorithm());
        assertEquals(10, domain.getPoolMaxActive());
        assertEquals(2, domain.getPoolMinIdle());

        assertEquals(domain, domainOps.read(newDomain));

        // 2. update domain
        domainOps.adjustPoolSize(newDomain, 100, 23);

        domain = (JPADomain) domainOps.read(newDomain);
        assertEquals(100, domain.getPoolMaxActive());
        assertEquals(23, domain.getPoolMinIdle());

        // 3. work with new domain - create user
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).
                until(() -> domainOps.read(newDomain).isDeployed());

        CLIENT_FACTORY = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain(newDomain);
        ADMIN_CLIENT = CLIENT_FACTORY.create(ADMIN_UNAME, "password");

        USER_SERVICE = ADMIN_CLIENT.getService(UserService.class);

        PagedResult<UserTO> users = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).page(1).size(1).build());
        assertNotNull(users);
        assertTrue(users.getResult().isEmpty());
        assertEquals(0, users.getTotalCount());

        Response response = USER_SERVICE.create(
                new UserCR.Builder(SyncopeConstants.ROOT_REALM, "monteverdi").
                        password("password123").
                        plainAttr(attr("email", "monteverdi@syncope.apache.org")).
                        build());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        UserTO user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(user);
        assertEquals("monteverdi", user.getUsername());

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        users = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).page(1).size(1).build());
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());
        assertEquals(1, users.getTotalCount());

        // 4. delete domain
        domainOps.delete(newDomain);

        List<Domain> list = domainOps.list();
        assertEquals(initial, list);
    }

    @Test
    public void domainCreateMaster() {
        assertThrows(
                KeymasterException.class,
                () -> domainOps.create(new JPADomain.Builder(SyncopeConstants.MASTER_DOMAIN).build()));
    }

    @Test
    public void domainCreateDuplicateKey() {
        List<Domain> initial = domainOps.list();
        assertNotNull(initial);
        assumeTrue(initial.stream().anyMatch(domain -> "Two".equals(domain.getKey())));

        if (initial.getFirst() instanceof JPADomain) {
            assertThrows(KeymasterException.class, () -> domainOps.create(new JPADomain.Builder("Two").build()));
        } else if (initial.getFirst() instanceof Neo4jDomain) {
            assertThrows(KeymasterException.class, () -> domainOps.create(new Neo4jDomain.Builder("Two").build()));
        } else {
            throw new IllegalStateException("Unsupported Domain class: " + initial.getFirst().getClass().getName());
        }
    }

    @Test
    public void domainUpdateAdminPassword() throws Exception {
        assumeTrue(domainOps.list().stream().anyMatch(d -> "Two".equals(d.getKey())));

        List<Domain> initial = domainOps.list();
        assertNotNull(initial);
        assumeTrue(initial.stream().anyMatch(domain -> "Two".equals(domain.getKey())));

        Domain two = domainOps.read("Two");
        assertNotNull(two);

        String origPasswowrd = two.getAdminPassword();
        CipherAlgorithm origCipherAlgo = two.getAdminCipherAlgorithm();

        try {
            // 1. change admin pwd for domain Two
            domainOps.changeAdminPassword(two.getKey(),
                    encryptorManager.getInstance().encode("password3", CipherAlgorithm.AES),
                    CipherAlgorithm.AES);

            // 2. attempt to access with old pwd -> fail
            try {
                new SyncopeClientFactoryBean().
                        setAddress(ADDRESS).setDomain(two.getKey()).setContentType(CLIENT_FACTORY.getContentType()).
                        create(ADMIN_UNAME, "password2").self();
            } catch (NotAuthorizedException e) {
                assertNotNull(e);
            }

            // 3. access with new pwd -> succeed
            new SyncopeClientFactoryBean().
                    setAddress(ADDRESS).setDomain(two.getKey()).setContentType(CLIENT_FACTORY.getContentType()).
                    create(ADMIN_UNAME, "password3").self();
        } finally {
            domainOps.changeAdminPassword(two.getKey(), origPasswowrd, origCipherAlgo);
        }
    }
}
