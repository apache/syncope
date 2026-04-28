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
package org.apache.syncope.core.logic;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.util.Utils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.syncope.common.lib.types.MfaCheck;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.security.access.prepost.PreAuthorize;

public class MfaLogic extends AbstractLogic<EntityTO> {

    protected final UserDataBinder userDataBinder;

    protected final UserDAO userDAO;

    protected final EncryptorManager encryptorManager;

    protected final DomainOps domainOps;

    protected final SecretGenerator totpSecretGenerator;

    protected final QrGenerator totpQrGenerator;

    protected final HashingAlgorithm totpHashingAlgorithm;

    protected final RecoveryCodeGenerator totpRecoveryCodeGenerator;

    protected final CodeVerifier totpCodeVerifier;

    protected final SecurityProperties securityProperties;

    public MfaLogic(
            final UserDataBinder userDataBinder,
            final UserDAO userDAO,
            final EncryptorManager encryptorManager,
            final DomainOps domainOps,
            final SecretGenerator totpSecretGenerator,
            final QrGenerator totpQrGenerator,
            final HashingAlgorithm totpHashingAlgorithm,
            final RecoveryCodeGenerator totpRecoveryCodeGenerator,
            final CodeVerifier totpCodeVerifier,
            final SecurityProperties securityProperties) {

        this.userDataBinder = userDataBinder;
        this.userDAO = userDAO;
        this.encryptorManager = encryptorManager;
        this.domainOps = domainOps;
        this.totpSecretGenerator = totpSecretGenerator;
        this.totpQrGenerator = totpQrGenerator;
        this.totpHashingAlgorithm = totpHashingAlgorithm;
        this.totpRecoveryCodeGenerator = totpRecoveryCodeGenerator;
        this.totpCodeVerifier = totpCodeVerifier;
        this.securityProperties = securityProperties;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public Mfa generate(final String username) {
        Mfa generated;
        try {
            String secret = totpSecretGenerator.generate();

            QrData qr = new QrData.Builder().
                    secret(secret).
                    issuer("Apache Syncope").
                    label("Apache Syncope " + AuthContextUtils.getDomain() + ":" + username).
                    algorithm(totpHashingAlgorithm).
                    build();
            String dataUri = Utils.getDataUriForImage(totpQrGenerator.generate(qr), totpQrGenerator.getImageMimeType());

            List<String> recoveryCodes = securityProperties.getAdminUser().equals(username)
                    ? List.of()
                    : Stream.of(totpRecoveryCodeGenerator.generateCodes(10)).collect(Collectors.toList());

            generated = new Mfa(secret, dataUri, recoveryCodes);
        } catch (Exception e) {
            LOG.error("While generating MFA secret for {}", username, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.ExecutionError);
            sce.getElements().add(
                    "While generating MFA secret for " + username + ": " + e.getMessage());
            throw sce;
        }

        return generated;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MFA_ENROLL + "')"
            + "or @environment.getProperty('security.adminUser') == authentication.name")
    public void enroll(final Mfa mfa) {
        String encrypted;
        try {
            encrypted = encryptorManager.getInstance().encode(mfa.secret(), CipherAlgorithm.AES);
        } catch (Exception e) {
            LOG.error("While enrolling MFA secret for {}", AuthContextUtils.getUsername(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.ExecutionError);
            sce.getElements().add(
                    "While enrolling MFA secret for " + AuthContextUtils.getUsername() + ": " + e.getMessage());
            throw sce;
        }

        if (securityProperties.getAdminUser().equals(AuthContextUtils.getUsername())) {
            if (SyncopeConstants.MASTER_DOMAIN.equals(AuthContextUtils.getDomain())) {
                if (StringUtils.isNotEmpty(securityProperties.getAdminMfaSecret())) {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.EntityExists);
                    sce.getElements().add("MFA secret for " + AuthContextUtils.getUsername() + " already set");
                    throw sce;
                } else {
                    securityProperties.setAdminMfaSecret(encrypted);
                    LOG.warn(
                            """
                             Generated MFA secret for {}. The property MUST be added to Core settings:
                             
                             \tsecurity.adminMfaSecret={}
                             
                             """,
                            securityProperties.getAdminUser(), encrypted);
                }
            } else {
                domainOps.setAdminMfaSecret(AuthContextUtils.getDomain(), encrypted);
            }
        } else {
            String username = AuthContextUtils.getUsername();
            AuthContextUtils.runAsAdmin(AuthContextUtils.getDomain(), () -> userDataBinder.setMfa(username, mfa));
        }
    }

    protected void doDismiss(final String username) {
        if (securityProperties.getAdminUser().equals(username)) {
            if (SyncopeConstants.MASTER_DOMAIN.equals(AuthContextUtils.getDomain())) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidUser);
                sce.getElements().add("Cannot dismiss " + username + " MFA");
                throw sce;
            }

            domainOps.setAdminMfaSecret(AuthContextUtils.getDomain(), null);
        } else {
            AuthContextUtils.runAsAdmin(AuthContextUtils.getDomain(),
                    () -> userDataBinder.setMfa(username, null));
        }
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MFA_ENROLL + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public void dismiss() {
        doDismiss(AuthContextUtils.getUsername());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    public void dismiss(final String username) {
        String realm = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                () -> userDAO.findByUsername(username).
                        orElseThrow(() -> new NotFoundException("User " + username)).
                        getRealm().getFullPath());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE), realm);

        AuthContextUtils.callAs(
                AuthContextUtils.getDomain(),
                AuthContextUtils.getUsername(),
                authRealms,
                () -> userDAO.findKey(username)).orElseThrow(() -> new NotFoundException("User " + username));

        doDismiss(username);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean enrolled(final String username) {
        if (securityProperties.getAdminUser().equals(username)) {
            if (SyncopeConstants.MASTER_DOMAIN.equals(AuthContextUtils.getDomain())) {
                return StringUtils.isNotEmpty(securityProperties.getAdminMfaSecret());
            }

            try {
                Domain domainObj = domainOps.read(AuthContextUtils.getDomain());
                return Optional.of(domainObj).map(d -> d.getAdminMfaSecret() != null).orElse(false);
            } catch (KeymasterException e) {
                throw new NotFoundException("Domain " + AuthContextUtils.getDomain(), e);
            }
        }

        return AuthContextUtils.callAsAdmin(
                AuthContextUtils.getDomain(),
                () -> userDAO.findByUsername(username).orElseThrow(() -> new NotFoundException("User " + username)).
                        getMfa() != null);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean check(final MfaCheck mfaCheck) {
        if (mfaCheck.secret() == null || mfaCheck.otp() == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidValues);
            sce.getElements().add("No secret and / or OTP provided");
            throw sce;
        }

        return totpCodeVerifier.isValidCode(mfaCheck.secret(), mfaCheck.otp());
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        throw new UnsupportedOperationException();
    }
}
