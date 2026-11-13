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
package org.apache.syncope.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.auth.AzureActiveDirectoryAuthModuleConf;
import org.apache.syncope.common.lib.auth.JDBCAuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.junit.jupiter.api.Test;

class AbstractConfTest {

    @Test
    void ldap() {
        LDAPAuthModuleConf conf = new LDAPAuthModuleConf();

        conf.setSearchFilter("cn={user}");
        conf.setPageSize(100);
        conf.setSubtreeSearch(false);
        conf.setLdapType(AbstractLDAPConf.LdapType.AD);
        conf.setLdapUrl("ldap://localhost:389");
        conf.setBindDn("cn=admin");
        conf.setBindCredential("secret");
        conf.setBaseDn("dc=example,dc=com");
        conf.setDisablePooling(true);
        conf.setMinPoolSize(1);
        conf.setMaxPoolSize(2);
        conf.setPoolPassivator(AbstractLDAPConf.LdapConnectionPoolPassivator.NONE);
        conf.setHostnameVerifier(AbstractLDAPConf.LdapHostnameVerifier.ANY);
        conf.setTrustManager(AbstractLDAPConf.LdapTrustManager.ANY);
        conf.setValidateOnCheckout(false);
        conf.setValidatePeriodically(false);
        conf.setValidateTimeout(Duration.ofSeconds(1));
        conf.setValidatePeriod(Duration.ofMinutes(1));
        conf.setFailFast(false);
        conf.setIdleTime(Duration.ofMinutes(2));
        conf.setPrunePeriod(Duration.ofMinutes(3));
        conf.setBlockWaitTime(Duration.ofSeconds(2));
        conf.setConnectionStrategy(AbstractLDAPConf.LdapConnectionStrategy.ROUND_ROBIN);
        conf.setUseStartTls(true);
        conf.setConnectTimeout(Duration.ofSeconds(3));
        conf.setResponseTimeout(Duration.ofSeconds(4));
        conf.setAllowMultipleDns(true);
        conf.setAllowMultipleEntries(true);
        conf.setFollowReferrals(false);
        conf.setBinaryAttributes(List.of("binary"));

        assertEquals("cn={user}", conf.getSearchFilter());
        assertEquals(100, conf.getPageSize());
        assertFalse(conf.isSubtreeSearch());
        assertEquals(AbstractLDAPConf.LdapType.AD, conf.getLdapType());
        assertEquals("ldap://localhost:389", conf.getLdapUrl());
        assertEquals("cn=admin", conf.getBindDn());
        assertEquals("secret", conf.getBindCredential());
        assertEquals("dc=example,dc=com", conf.getBaseDn());
        assertTrue(conf.isDisablePooling());
        assertEquals(1, conf.getMinPoolSize());
        assertEquals(2, conf.getMaxPoolSize());
        assertEquals(AbstractLDAPConf.LdapConnectionPoolPassivator.NONE, conf.getPoolPassivator());
        assertEquals(AbstractLDAPConf.LdapHostnameVerifier.ANY, conf.getHostnameVerifier());
        assertEquals(AbstractLDAPConf.LdapTrustManager.ANY, conf.getTrustManager());
        assertFalse(conf.isValidateOnCheckout());
        assertFalse(conf.isValidatePeriodically());
        assertEquals(Duration.ofSeconds(1), conf.getValidateTimeout());
        assertEquals(Duration.ofMinutes(1), conf.getValidatePeriod());
        assertFalse(conf.isFailFast());
        assertEquals(Duration.ofMinutes(2), conf.getIdleTime());
        assertEquals(Duration.ofMinutes(3), conf.getPrunePeriod());
        assertEquals(Duration.ofSeconds(2), conf.getBlockWaitTime());
        assertEquals(AbstractLDAPConf.LdapConnectionStrategy.ROUND_ROBIN, conf.getConnectionStrategy());
        assertTrue(conf.isUseStartTls());
        assertEquals(Duration.ofSeconds(3), conf.getConnectTimeout());
        assertEquals(Duration.ofSeconds(4), conf.getResponseTimeout());
        assertTrue(conf.isAllowMultipleDns());
        assertTrue(conf.isAllowMultipleEntries());
        assertFalse(conf.isFollowReferrals());
        assertEquals(List.of("binary"), conf.getBinaryAttributes());
    }

    @Test
    void jdbc() {
        JDBCAuthModuleConf conf = new JDBCAuthModuleConf();

        conf.setSql("SELECT 1");
        conf.setDialect("dialect");
        conf.setDriverClass("driver");
        conf.setUrl("url");
        conf.setUser("user");
        conf.setPassword("password");
        conf.setDefaultCatalog("catalog");
        conf.setDefaultSchema("schema");
        conf.setHealthQuery("health");
        conf.setIdleTimeout(Duration.ofSeconds(1));
        conf.setDataSourceName("dataSource");
        conf.setMinPoolSize(1);
        conf.setMaxPoolSize(2);
        conf.setMaxPoolWait(Duration.ofSeconds(2));
        conf.setPoolSuspension(true);
        conf.setPoolTimeoutMillis(3L);
        conf.setPoolLeakThreshold(Duration.ofSeconds(3));

        assertEquals("SELECT 1", conf.getSql());
        assertEquals("dialect", conf.getDialect());
        assertEquals("driver", conf.getDriverClass());
        assertEquals("url", conf.getUrl());
        assertEquals("user", conf.getUser());
        assertEquals("password", conf.getPassword());
        assertEquals("catalog", conf.getDefaultCatalog());
        assertEquals("schema", conf.getDefaultSchema());
        assertEquals("health", conf.getHealthQuery());
        assertEquals(Duration.ofSeconds(1), conf.getIdleTimeout());
        assertEquals("dataSource", conf.getDataSourceName());
        assertEquals(1, conf.getMinPoolSize());
        assertEquals(2, conf.getMaxPoolSize());
        assertEquals(Duration.ofSeconds(2), conf.getMaxPoolWait());
        assertTrue(conf.isPoolSuspension());
        assertEquals(3L, conf.getPoolTimeoutMillis());
        assertEquals(Duration.ofSeconds(3), conf.getPoolLeakThreshold());
    }

    @Test
    void azureActiveDirectory() {
        AzureActiveDirectoryAuthModuleConf conf = new AzureActiveDirectoryAuthModuleConf();

        conf.setClientId("clientId");
        conf.setClientSecret("clientSecret");
        conf.setLoginUrl("loginUrl");
        conf.setResource("resource");
        conf.setTenant("tenant");
        conf.setScope("scope");

        assertEquals("clientId", conf.getClientId());
        assertEquals("clientSecret", conf.getClientSecret());
        assertEquals("loginUrl", conf.getLoginUrl());
        assertEquals("resource", conf.getResource());
        assertEquals("tenant", conf.getTenant());
        assertEquals("scope", conf.getScope());
    }

    @Test
    void oauth20AndOidc() {
        OIDCAuthModuleConf conf = new OIDCAuthModuleConf();

        conf.setId("clientId");
        conf.setClientSecret("clientSecret");
        conf.setEnabled(false);
        conf.setCustomParams(Map.of("key", "value"));
        conf.setTokenUrl("tokenUrl");
        conf.setResponseType("responseType");
        conf.setScope("scope");
        conf.setUserIdAttribute("userIdAttribute");
        conf.setDiscoveryUri("discoveryUri");
        conf.setUseNonce(false);
        conf.setPreferredJwsAlgorithm("algorithm");
        conf.setMaxClockSkew("skew");
        conf.setResponseMode("responseMode");
        conf.setExpireSessionWithToken(true);
        conf.setTokenExpirationAdvance("advance");

        assertEquals("clientId", conf.getClientId());
        assertEquals("clientSecret", conf.getClientSecret());
        assertFalse(conf.isEnabled());
        assertEquals("value", conf.getCustomParams().get("key"));
        assertEquals("tokenUrl", conf.getTokenUrl());
        assertEquals("responseType", conf.getResponseType());
        assertEquals("scope", conf.getScope());
        assertEquals("userIdAttribute", conf.getUserIdAttribute());
        assertEquals("discoveryUri", conf.getDiscoveryUri());
        assertFalse(conf.isUseNonce());
        assertEquals("algorithm", conf.getPreferredJwsAlgorithm());
        assertEquals("skew", conf.getMaxClockSkew());
        assertEquals("responseMode", conf.getResponseMode());
        assertTrue(conf.isExpireSessionWithToken());
        assertEquals("advance", conf.getTokenExpirationAdvance());
    }
}
