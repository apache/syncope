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
package org.apache.syncope.common.lib.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.junit.jupiter.api.Test;

class AMPolicyConfsTest {

    @Test
    void defaultAccessPolicyConf() {
        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setOrder(1);
        conf.setEnabled(false);
        conf.setSsoEnabled(false);
        conf.setRequireAllAttributes(false);
        conf.setCaseInsensitive(true);
        conf.setUnauthorizedRedirectUrl(URI.create("https://syncope.apache.org"));
        conf.getRequiredAttrs().put("cn", "admin");
        conf.getRejectedAttrs().put("uid", "plain");

        assertEquals(1, conf.getOrder());
        assertFalse(conf.isEnabled());
        assertFalse(conf.isSsoEnabled());
        assertFalse(conf.isRequireAllAttributes());
        assertTrue(conf.isCaseInsensitive());
        assertEquals(URI.create("https://syncope.apache.org"), conf.getUnauthorizedRedirectUrl());
        assertEquals("admin", conf.getRequiredAttrs().get("cn"));
        assertEquals("plain", conf.getRejectedAttrs().get("uid"));
    }

    @Test
    void defaultAttrReleasePolicyConf() {
        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.setPrincipalIdAttr("principalIdAttr");
        conf.getReleaseAttrs().put("cn", "value");
        conf.getAllowedAttrs().add("allowed");
        conf.getExcludedAttrs().add("excluded");
        conf.getIncludeOnlyAttrs().add("includeOnly");

        DefaultAttrReleasePolicyConf.PrincipalAttrRepoConf principalAttrRepoConf =
                new DefaultAttrReleasePolicyConf.PrincipalAttrRepoConf();
        principalAttrRepoConf.setMergingStrategy(
                DefaultAttrReleasePolicyConf.PrincipalAttrRepoMergingStrategy.ADD);
        principalAttrRepoConf.setIgnoreResolvedAttributes(true);
        principalAttrRepoConf.setExpiration(1L);
        principalAttrRepoConf.setTimeUnit(TimeUnit.MINUTES);
        principalAttrRepoConf.getAttrRepos().add("attrRepo");
        conf.setPrincipalAttrRepoConf(principalAttrRepoConf);

        assertEquals("principalIdAttr", conf.getPrincipalIdAttr());
        assertEquals("value", conf.getReleaseAttrs().get("cn"));
        assertEquals(1, conf.getAllowedAttrs().size());
        assertEquals(1, conf.getExcludedAttrs().size());
        assertEquals(1, conf.getIncludeOnlyAttrs().size());
        assertSame(principalAttrRepoConf, conf.getPrincipalAttrRepoConf());

        assertEquals(DefaultAttrReleasePolicyConf.PrincipalAttrRepoMergingStrategy.ADD,
                principalAttrRepoConf.getMergingStrategy());
        assertTrue(principalAttrRepoConf.isIgnoreResolvedAttributes());
        assertEquals(1L, principalAttrRepoConf.getExpiration());
        assertEquals(TimeUnit.MINUTES, principalAttrRepoConf.getTimeUnit());
        assertEquals(1, principalAttrRepoConf.getAttrRepos().size());
    }

    @Test
    void defaultAuthPolicyConf() {
        DefaultAuthPolicyConf conf = new DefaultAuthPolicyConf();
        conf.setTryAll(true);
        conf.setBypassEnabled(true);
        conf.setForceMfaExecution(false);
        conf.setBypassPrincipalAttributeName("name");
        conf.setBypassPrincipalAttributeValue("value");
        conf.setBypassTrustedDeviceEnabled(false);
        conf.getAuthModules().add("module");

        assertTrue(conf.isTryAll());
        assertTrue(conf.isBypassEnabled());
        assertFalse(conf.isForceMfaExecution());
        assertEquals("name", conf.getBypassPrincipalAttributeName());
        assertEquals("value", conf.getBypassPrincipalAttributeValue());
        assertFalse(conf.isBypassTrustedDeviceEnabled());
        assertEquals(1, conf.getAuthModules().size());
    }

    @Test
    void defaultTicketExpirationPolicyConf() {
        DefaultTicketExpirationPolicyConf conf = new DefaultTicketExpirationPolicyConf();

        DefaultTicketExpirationPolicyConf.STConf stConf = new DefaultTicketExpirationPolicyConf.STConf();
        stConf.setMaxTimeToLiveInSeconds(10L);
        stConf.setNumberOfUses(3L);
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(20L);
        DefaultTicketExpirationPolicyConf.STConf proxyStConf = new DefaultTicketExpirationPolicyConf.STConf();
        proxyStConf.setMaxTimeToLiveInSeconds(30L);
        proxyStConf.setNumberOfUses(4L);
        DefaultTicketExpirationPolicyConf.TGTConf proxyTgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        proxyTgtConf.setMaxTimeToLiveInSeconds(40L);

        conf.setStConf(stConf);
        conf.setTgtConf(tgtConf);
        conf.setProxyStConf(proxyStConf);
        conf.setProxyTgtConf(proxyTgtConf);

        assertSame(stConf, conf.getStConf());
        assertSame(tgtConf, conf.getTgtConf());
        assertSame(proxyStConf, conf.getProxyStConf());
        assertSame(proxyTgtConf, conf.getProxyTgtConf());
        assertEquals(10L, stConf.getMaxTimeToLiveInSeconds());
        assertEquals(3L, stConf.getNumberOfUses());
        assertEquals(20L, tgtConf.getMaxTimeToLiveInSeconds());
        assertEquals(4L, proxyStConf.getNumberOfUses());
    }

    @Test
    void otherAccessPolicyConfs() {
        HttpRequestAccessPolicyConf httpConf = new HttpRequestAccessPolicyConf();
        httpConf.setIpAddress("127.0.0.1");
        httpConf.setUserAgent("agent");
        assertEquals("127.0.0.1", httpConf.getIpAddress());
        assertEquals("agent", httpConf.getUserAgent());

        OpenFGAAccessPolicyConf openFGAConf = new OpenFGAAccessPolicyConf();
        openFGAConf.setApiUrl("https://openfga");
        openFGAConf.setToken("token");
        openFGAConf.setStoreId("store");
        openFGAConf.setUserType(AnyTypeKind.GROUP.name());
        openFGAConf.setRelation("relation");
        openFGAConf.setObject("object");
        assertEquals("https://openfga", openFGAConf.getApiUrl());
        assertEquals("token", openFGAConf.getToken());
        assertEquals("store", openFGAConf.getStoreId());
        assertEquals(AnyTypeKind.GROUP.name(), openFGAConf.getUserType());
        assertEquals("relation", openFGAConf.getRelation());
        assertEquals("object", openFGAConf.getObject());

        RemoteEndpointAccessPolicyConf remoteConf = new RemoteEndpointAccessPolicyConf();
        remoteConf.setEndpointUrl("https://endpoint");
        remoteConf.getAcceptableResponseCodes().add("200");
        assertEquals("https://endpoint", remoteConf.getEndpointUrl());
        assertEquals(1, remoteConf.getAcceptableResponseCodes().size());

        TimeBasedAccessPolicyConf timeConf = new TimeBasedAccessPolicyConf();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        timeConf.setStart(now);
        timeConf.setEnd(now.plusHours(1));
        timeConf.setZoneId("UTC");
        assertEquals(now, timeConf.getStart());
        assertEquals(now.plusHours(1), timeConf.getEnd());
        assertEquals("UTC", timeConf.getZoneId());
    }

    @Test
    void policyTOs() {
        DefaultAccessPolicyConf accessConf = new DefaultAccessPolicyConf();
        AccessPolicyTO accessTO = new AccessPolicyTO();
        accessTO.setName("access");
        accessTO.setConf(accessConf);
        assertEquals("access", accessTO.getName());
        assertEquals(AccessPolicyTO.class.getName(), accessTO.getDiscriminator());
        assertSame(accessConf, accessTO.getConf());

        DefaultAttrReleasePolicyConf attrReleaseConf = new DefaultAttrReleasePolicyConf();
        AttrReleasePolicyTO attrReleaseTO = new AttrReleasePolicyTO();
        attrReleaseTO.setName("attrRelease");
        attrReleaseTO.setOrder(2);
        attrReleaseTO.setStatus(false);
        attrReleaseTO.setConf(attrReleaseConf);
        assertEquals("attrRelease", attrReleaseTO.getName());
        assertEquals(AttrReleasePolicyTO.class.getName(), attrReleaseTO.getDiscriminator());
        assertEquals(2, attrReleaseTO.getOrder());
        assertFalse(attrReleaseTO.getStatus());
        assertSame(attrReleaseConf, attrReleaseTO.getConf());

        DefaultAuthPolicyConf authConf = new DefaultAuthPolicyConf();
        AuthPolicyTO authTO = new AuthPolicyTO();
        authTO.setName("auth");
        authTO.setConf(authConf);
        assertEquals("auth", authTO.getName());
        assertEquals(AuthPolicyTO.class.getName(), authTO.getDiscriminator());
        assertSame(authConf, authTO.getConf());

        DefaultTicketExpirationPolicyConf ticketConf = new DefaultTicketExpirationPolicyConf();
        TicketExpirationPolicyTO ticketTO = new TicketExpirationPolicyTO();
        ticketTO.setName("ticket");
        ticketTO.setConf(ticketConf);
        assertEquals("ticket", ticketTO.getName());
        assertEquals(TicketExpirationPolicyTO.class.getName(), ticketTO.getDiscriminator());
        assertSame(ticketConf, ticketTO.getConf());
    }
}
