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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.oidc.OIDCRequest;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.oidc.NoOpSessionStore;
import org.apache.syncope.core.logic.oidc.OIDCC4UIContext;
import org.apache.syncope.core.logic.oidc.OIDCClientCache;
import org.apache.syncope.core.logic.oidc.OIDCUserManager;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.springframework.security.access.prepost.PreAuthorize;

public class OIDCC4UILogic extends AbstractTransactionalLogic<EntityTO> {

    protected static final String JWT_CLAIM_OP_NAME = "OP_NAME";

    protected static final String JWT_CLAIM_ID_TOKEN = "ID_TOKEN";

    protected final OIDCClientCache oidcClientCacheLogin;

    protected final OIDCClientCache oidcClientCacheLogout;

    protected final AuthDataAccessor authDataAccessor;

    protected final AccessTokenDataBinder accessTokenDataBinder;

    protected final OIDCC4UIProviderDAO opDAO;

    protected final OIDCUserManager userManager;

    protected final EncryptorManager encryptorManager;

    public OIDCC4UILogic(
            final OIDCClientCache oidcClientCacheLogin,
            final OIDCClientCache oidcClientCacheLogout,
            final AuthDataAccessor authDataAccessor,
            final AccessTokenDataBinder accessTokenDataBinder,
            final OIDCC4UIProviderDAO opDAO,
            final OIDCUserManager userManager,
            final EncryptorManager encryptorManager) {

        this.oidcClientCacheLogin = oidcClientCacheLogin;
        this.oidcClientCacheLogout = oidcClientCacheLogout;
        this.authDataAccessor = authDataAccessor;
        this.accessTokenDataBinder = accessTokenDataBinder;
        this.opDAO = opDAO;
        this.userManager = userManager;
        this.encryptorManager = encryptorManager;
    }

    protected OidcClient getOidcClient(
            final OIDCClientCache oidcClientCache,
            final OIDCC4UIProvider op,
            final String callbackUrl) {

        return oidcClientCache.get(op.getName()).orElseGet(() -> oidcClientCache.add(op, callbackUrl));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCRequest createLoginRequest(final String redirectURI, final String opName) {
        // 0. look for OP
        OIDCC4UIProvider op = opDAO.findByName(opName).
                orElseThrow(() -> new NotFoundException("OIDC Provider '" + opName + '\''));

        // 1. look for OidcClient
        OidcClient oidcClient = getOidcClient(oidcClientCacheLogin, op, redirectURI);

        // 2. create OIDCRequest
        WithLocationAction action = oidcClient.getRedirectionAction(
                new CallContext(new OIDCC4UIContext(), NoOpSessionStore.INSTANCE)).
                map(WithLocationAction.class::cast).
                orElseThrow(() -> {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                    sce.getElements().add("No RedirectionAction generated for LoginRequest");
                    return sce;
                });

        OIDCRequest loginRequest = new OIDCRequest();
        loginRequest.setLocation(action.getLocation());
        return loginRequest;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCLoginResponse login(final String redirectURI, final String authorizationCode, final String opName) {
        // 0. look for OP
        OIDCC4UIProvider op = opDAO.findByName(opName).
                orElseThrow(() -> new NotFoundException("OIDC Provider '" + opName + '\''));

        // 1. look for configured client
        OidcClient oidcClient = getOidcClient(oidcClientCacheLogin, op, redirectURI);

        // 2. get OpenID Connect tokens
        String idTokenHint;
        JWTClaimsSet idToken;
        try {
            OidcCredentials credentials = new OidcCredentials();
            credentials.setCode(authorizationCode);

            oidcClient.getAuthenticator().validate(
                    new CallContext(new OIDCC4UIContext(), NoOpSessionStore.INSTANCE), credentials);

            idToken = credentials.toIdToken().getJWTClaimsSet();
            idTokenHint = credentials.toIdToken().serialize();
        } catch (Exception e) {
            LOG.error("While validating Token Response", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 3. prepare the result
        OIDCLoginResponse loginResp = new OIDCLoginResponse();
        loginResp.setLogoutSupported(StringUtils.isNotBlank(op.getEndSessionEndpoint()));

        // 3a. find matching user (if any) and return the received attributes
        String keyValue = idToken.getSubject();
        for (Item item : op.getItems()) {
            Attr attrTO = new Attr();
            attrTO.setSchema(item.getExtAttrName());

            String value = Optional.ofNullable(idToken.getClaim(item.getExtAttrName())).
                    map(Object::toString).
                    orElse(null);
            if (value != null) {
                attrTO.getValues().add(value);
                loginResp.getAttrs().add(attrTO);
                if (item.isConnObjectKey()) {
                    keyValue = value;
                }
            }
        }

        List<String> matchingUsers = Optional.ofNullable(keyValue).
                map(k -> userManager.findMatchingUser(k, op.getConnObjectKeyItem().get())).
            orElseGet(List::of);
        LOG.debug("Found {} matching users for {}", matchingUsers.size(), keyValue);

        // 3b. not found: create or selfreg if configured
        String username;
        if (matchingUsers.isEmpty()) {
            if (op.isCreateUnmatching()) {
                LOG.debug("No user matching {}, about to create", keyValue);

                String defaultUsername = keyValue;
                username = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                        () -> userManager.create(op, loginResp, defaultUsername));
            } else if (op.isSelfRegUnmatching()) {
                UserTO userTO = new UserTO();

                userManager.fill(op, loginResp, userTO);

                loginResp.getAttrs().clear();
                loginResp.getAttrs().addAll(userTO.getPlainAttrs());
                if (StringUtils.isNotBlank(userTO.getUsername())) {
                    loginResp.setUsername(userTO.getUsername());
                } else {
                    loginResp.setUsername(keyValue);
                }

                loginResp.setSelfReg(true);

                return loginResp;
            } else {
                throw new NotFoundException(Optional.ofNullable(keyValue).
                        map(value -> "User matching the provided value " + value).
                        orElse("User marching the provided claims"));
            }
        } else if (matchingUsers.size() > 1) {
            throw new IllegalArgumentException("Several users match the provided value " + keyValue);
        } else {
            if (op.isUpdateMatching()) {
                LOG.debug("About to update {} for {}", matchingUsers.getFirst(), keyValue);

                username = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                        () -> userManager.update(matchingUsers.getFirst(), op, loginResp));
            } else {
                username = matchingUsers.getFirst();
            }
        }

        loginResp.setUsername(username);

        // 4. generate JWT for further access
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_OP_NAME, opName);
        claims.put(JWT_CLAIM_ID_TOKEN, idTokenHint);

        byte[] authorities = null;
        try {
            authorities = encryptorManager.getInstance().encode(POJOHelper.serialize(
                    authDataAccessor.getAuthorities(loginResp.getUsername(), null)), CipherAlgorithm.AES).
                    getBytes();
        } catch (Exception e) {
            LOG.error("Could not fetch authorities", e);
        }

        Pair<String, OffsetDateTime> accessTokenInfo =
                accessTokenDataBinder.create(loginResp.getUsername(), claims, authorities, true);
        loginResp.setAccessToken(accessTokenInfo.getLeft());
        loginResp.setAccessTokenExpiryTime(accessTokenInfo.getRight());

        return loginResp;
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "'))")
    public OIDCRequest createLogoutRequest(final String accessToken, final String redirectURI) {
        // 0. fetch the current JWT used for Syncope authentication
        JWTClaimsSet claimsSet;
        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);
            claimsSet = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAccessToken);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 1. look for OidcClient
        OIDCC4UIProvider op = opDAO.findByName((String) claimsSet.getClaim(JWT_CLAIM_OP_NAME)).
                orElseThrow(() -> new NotFoundException(""
                + "OIDC Provider '" + claimsSet.getClaim(JWT_CLAIM_OP_NAME) + '\''));
        OidcClient oidcClient = getOidcClient(oidcClientCacheLogout, op, redirectURI);

        // 2. create OIDCRequest
        OidcProfile profile = new OidcProfile();
        profile.setIdTokenString((String) claimsSet.getClaim(JWT_CLAIM_ID_TOKEN));

        WithLocationAction action = oidcClient.getLogoutAction(
                new CallContext(new OIDCC4UIContext(), NoOpSessionStore.INSTANCE),
                profile,
                redirectURI).
                map(WithLocationAction.class::cast).
                orElseThrow(() -> {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                    sce.getElements().add("No RedirectionAction generated for LogoutRequest");
                    return sce;
                });

        OIDCRequest logoutRequest = new OIDCRequest();
        logoutRequest.setLocation(action.getLocation());
        return logoutRequest;
    }

    @Override
    protected EntityTO resolveReference(
            final Method method, final Object... args) throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
