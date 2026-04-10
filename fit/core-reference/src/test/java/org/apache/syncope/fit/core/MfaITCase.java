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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.time.Instant;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.lib.BasicAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.syncope.common.lib.types.MfaCheck;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.MfaService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MfaITCase extends AbstractITCase {

    private static final CodeGenerator TOTP_CODE_GENERATOR = new DefaultCodeGenerator(HashingAlgorithm.SHA512);

    private static String generateOtp(final String secret) throws CodeGenerationException {
        return TOTP_CODE_GENERATOR.generate(secret, Math.floorDiv(Instant.now().getEpochSecond(), 30));
    }

    private static void enrollDismiss(
            final SyncopeClientFactoryBean clientFactory,
            final String username,
            final String password) throws CodeGenerationException {

        // cannot obtain JWT client instance without MFA
        ForbiddenException fe = assertThrows(
                ForbiddenException.class, () -> clientFactory.create(username, password).self());
        assertEquals(IdRepoEntitlement.MFA_ENROLL, fe.getResponse().getHeaderString(RESTHeaders.OWNED_ENTITLEMENTS));
        assertEquals("Please enroll your MFA first", fe.getMessage());

        // cannot access REST services without MFA
        SyncopeClient nonMfaClient = clientFactory.create(new BasicAuthenticationHandler(username, password));

        fe = assertThrows(ForbiddenException.class, () -> nonMfaClient.self());
        assertEquals(IdRepoEntitlement.MFA_ENROLL, fe.getResponse().getHeaderString(RESTHeaders.OWNED_ENTITLEMENTS));
        assertEquals("Please enroll your MFA first", fe.getMessage());

        MfaService mfaService = clientFactory.createAnonymous(ANONYMOUS_UNAME, ANONYMOUS_KEY).
                getService(MfaService.class);

        // MFA is not enrolled
        assertFalse(BooleanUtils.toBoolean(mfaService.enrolled(username).getHeaderString(RESTHeaders.VERIFIED)));

        // generate MFA
        Mfa mfa = mfaService.generate(username);

        // check OTP
        assertTrue(BooleanUtils.toBoolean(mfaService.check(
                new MfaCheck(mfa.secret(), generateOtp(mfa.secret()))).getHeaderString(RESTHeaders.VERIFIED)));

        // enroll MFA
        nonMfaClient.getService(MfaService.class).enroll(mfa);

        // MFA was enrolled
        assertTrue(BooleanUtils.toBoolean(mfaService.enrolled(username).getHeaderString(RESTHeaders.VERIFIED)));

        // cannot obtain JWT client instance without OTP
        assertThrows(NotAuthorizedException.class, () -> clientFactory.create(username, password));

        // now JWT client instance is obtained
        SyncopeClient mfaClient = clientFactory.create(username, password, generateOtp(mfa.secret()));

        // now REST services can be accessed
        assertDoesNotThrow(() -> mfaClient.self());

        // cannot dismiss MFA with non-MFA client
        assertThrows(NotAuthorizedException.class, () -> nonMfaClient.getService(MfaService.class).dismiss());

        // MFA dismiss requires MFA client
        assertDoesNotThrow(() -> mfaClient.getService(MfaService.class).dismiss());

        // cannot access REST services without MFA
        fe = assertThrows(ForbiddenException.class, () -> nonMfaClient.self());
        assertEquals(IdRepoEntitlement.MFA_ENROLL, fe.getResponse().getHeaderString(RESTHeaders.OWNED_ENTITLEMENTS));
        assertEquals("Please enroll your MFA first", fe.getMessage());

        // new MFA can be enrolled
        Mfa newmfa = mfaService.generate(username);
        assertNotEquals(mfa.secret(), newmfa.secret());
        nonMfaClient.getService(MfaService.class).enroll(mfa);
    }

    @BeforeEach
    public void enableMfa() {
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, StandardConfParams.MFA_ENABLED, true);
        confParamOps.set("Two", StandardConfParams.MFA_ENABLED, true);
    }

    @AfterEach
    public void disableMfa() {
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, StandardConfParams.MFA_ENABLED, false);
        confParamOps.set("Two", StandardConfParams.MFA_ENABLED, false);
    }

    @Test
    public void enrollDismissAsMasterAdmin() throws CodeGenerationException {
        // cannot obtain JWT client instance without MFA
        ForbiddenException fe = assertThrows(
                ForbiddenException.class, () -> CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD).self());
        assertEquals(IdRepoEntitlement.MFA_ENROLL, fe.getResponse().getHeaderString(RESTHeaders.OWNED_ENTITLEMENTS));
        assertEquals("Please enroll your MFA first", fe.getMessage());

        MfaService mfaService = ANONYMOUS_CLIENT.getService(MfaService.class);

        // MFA is not enrolled
        assertFalse(BooleanUtils.toBoolean(mfaService.enrolled(ADMIN_UNAME).getHeaderString(RESTHeaders.VERIFIED)));

        // generate MFA
        Mfa mfa = mfaService.generate(ADMIN_UNAME);

        // check OTP
        assertTrue(BooleanUtils.toBoolean(mfaService.check(
                new MfaCheck(mfa.secret(), generateOtp(mfa.secret()))).getHeaderString(RESTHeaders.VERIFIED)));

        // enroll MFA
        MFA_SERVICE.enroll(mfa);

        // MFA was enrolled
        assertTrue(BooleanUtils.toBoolean(mfaService.enrolled(ADMIN_UNAME).getHeaderString(RESTHeaders.VERIFIED)));

        // now JWT client instance is obtained
        SyncopeClient mfaClient = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD, generateOtp(mfa.secret()));

        // Master admin cannot dismiss MFA
        SyncopeClientException sce = assertThrows(
                SyncopeClientException.class, () -> mfaClient.getService(MfaService.class).dismiss());
        assertEquals(ClientExceptionType.InvalidUser, sce.getType());
        assertEquals("InvalidUser [Cannot dismiss admin MFA]", sce.getMessage());
    }

    @Test
    public void enrollDismissAsMultitenancyAdmin() throws CodeGenerationException {
        assumeTrue(domainOps.list().stream().anyMatch(d -> "Two".equals(d.getKey())));

        SyncopeClientFactoryBean twoClientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("Two");

        enrollDismiss(twoClientFactory, ADMIN_UNAME, "password2");
    }

    @Test
    public void enrollDismissAsUser() throws CodeGenerationException {
        enrollDismiss(CLIENT_FACTORY, "bellini", "password");
    }
}
