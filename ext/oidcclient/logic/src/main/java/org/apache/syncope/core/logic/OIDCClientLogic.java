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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectProvider;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.AbstractUserInfo;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rs.security.oidc.rp.IdTokenReader;
import org.apache.cxf.rs.security.oidc.rp.UserInfoClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.OIDCLoginRequestTO;
import org.apache.syncope.common.lib.to.OIDCLoginResponseTO;
import org.apache.syncope.common.lib.to.OIDCLogoutRequestTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.model.TokenEndpointResponse;
import org.apache.syncope.core.logic.oidc.OIDCUserManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCProviderItem;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class OIDCClientLogic extends AbstractTransactionalLogic<EntityTO> {

    private static final String JWT_CLAIM_OP_ENTITYID = "OP_ENTITYID";

    private static final String JWT_CLAIM_USERID = "USERID";

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Autowired
    private AuthDataAccessor authDataAccessor;

    @Autowired
    private AccessTokenDataBinder accessTokenDataBinder;

    @Autowired
    private OIDCProviderDAO opDAO;

    @Autowired
    private OIDCUserManager userManager;

    private OIDCProvider getOIDCProvider(final String opName) {
        OIDCProvider op = null;
        if (StringUtils.isBlank(opName)) {
            List<OIDCProvider> ops = opDAO.findAll();
            if (!ops.isEmpty()) {
                op = ops.get(0);
            }
        } else {
            op = opDAO.findByName(opName);
        }
        if (op == null) {
            throw new NotFoundException(StringUtils.isBlank(opName)
                    ? "Any OIDC Provider"
                    : "OIDC Provider '" + opName + '\'');
        }
        return op;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCLoginRequestTO createLoginRequest(final String redirectURI, final String opName) {
        // 1. look for Provider
        OIDCProvider op = getOIDCProvider(opName);

        // 2. create AuthnRequest
        OIDCLoginRequestTO requestTO = new OIDCLoginRequestTO();
        requestTO.setProviderAddress(op.getAuthorizationEndpoint());
        requestTO.setClientId(op.getClientID());
        requestTO.setScope("openid email profile");
        requestTO.setResponseType(OAuthConstants.CODE_RESPONSE_TYPE);
        requestTO.setRedirectURI(redirectURI);
        requestTO.setState(SecureRandomUtils.generateRandomUUID().toString());
        return requestTO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCLoginResponseTO login(final String redirectURI, final String authorizationCode, final String opName) {
        OIDCProvider op = getOIDCProvider(opName);

        // 1. get OpenID Connect tokens
        String body = OAuthConstants.AUTHORIZATION_CODE_VALUE + '=' + authorizationCode
                + '&' + OAuthConstants.CLIENT_ID + '=' + op.getClientID()
                + '&' + OAuthConstants.CLIENT_SECRET + '=' + op.getClientSecret()
                + '&' + OAuthConstants.REDIRECT_URI + '=' + redirectURI
                + '&' + OAuthConstants.GRANT_TYPE + '=' + OAuthConstants.AUTHORIZATION_CODE_GRANT;
        TokenEndpointResponse tokenEndpointResponse;
        try {
            tokenEndpointResponse = getOIDCTokens(op.getTokenEndpoint(), body);
        } catch (IOException e) {
            LOG.error("Unexpected response for OIDC Tokens", e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add("Unexpected response for OIDC Tokens: " + e.getMessage());
            throw sce;
        }

        Consumer consumer = new Consumer(op.getClientID(), op.getClientSecret());

        // 2. validate token
        LOG.debug("Id Token to be validated: {}", tokenEndpointResponse.getIdToken());
        IdToken idToken = getValidatedIdToken(op, consumer, tokenEndpointResponse.getIdToken());

        // 3. prepare the result:
        final OIDCLoginResponseTO responseTO = new OIDCLoginResponseTO();
        responseTO.setLogoutSupported(StringUtils.isNotBlank(op.getEndSessionEndpoint()));

        // 3a. extract user info from userInfoEndpoint if exists otherwise from idToken
        AbstractUserInfo userInfo = StringUtils.isBlank(op.getUserinfoEndpoint())
                ? idToken
                : getUserInfo(op.getUserinfoEndpoint(), tokenEndpointResponse.getAccessToken(), idToken, consumer);

        // 3b. find matching user (if any) and return the received attributes
        String keyValue = userInfo.getEmail();
        for (OIDCProviderItem item : op.getItems()) {
            Attr attrTO = new Attr();
            attrTO.setSchema(item.getExtAttrName());
            switch (item.getExtAttrName()) {
                case UserInfo.PREFERRED_USERNAME_CLAIM:
                    attrTO.getValues().add(userInfo.getPreferredUserName());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getPreferredUserName();
                    }
                    break;

                case UserInfo.PROFILE_CLAIM:
                    attrTO.getValues().add(userInfo.getProfile());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getProfile();
                    }
                    break;

                case UserInfo.EMAIL_CLAIM:
                    attrTO.getValues().add(userInfo.getEmail());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getEmail();
                    }
                    break;

                case UserInfo.NAME_CLAIM:
                    attrTO.getValues().add(userInfo.getName());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getName();
                    }
                    break;

                case UserInfo.FAMILY_NAME_CLAIM:
                    attrTO.getValues().add(userInfo.getFamilyName());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getFamilyName();
                    }
                    break;

                case UserInfo.MIDDLE_NAME_CLAIM:
                    attrTO.getValues().add(userInfo.getMiddleName());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getMiddleName();
                    }
                    break;

                case UserInfo.GIVEN_NAME_CLAIM:
                    attrTO.getValues().add(userInfo.getGivenName());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getGivenName();
                    }
                    break;

                case UserInfo.NICKNAME_CLAIM:
                    attrTO.getValues().add(userInfo.getNickName());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getNickName();
                    }
                    break;

                case UserInfo.GENDER_CLAIM:
                    attrTO.getValues().add(userInfo.getGender());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getGender();
                    }
                    break;

                case UserInfo.LOCALE_CLAIM:
                    attrTO.getValues().add(userInfo.getLocale());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getLocale();
                    }
                    break;

                case UserInfo.ZONEINFO_CLAIM:
                    attrTO.getValues().add(userInfo.getZoneInfo());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getZoneInfo();
                    }
                    break;

                case UserInfo.BIRTHDATE_CLAIM:
                    attrTO.getValues().add(userInfo.getBirthDate());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getBirthDate();
                    }
                    break;

                case UserInfo.PHONE_CLAIM:
                    attrTO.getValues().add(userInfo.getPhoneNumber());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getPhoneNumber();
                    }
                    break;

                case UserInfo.ADDRESS_CLAIM:
                    attrTO.getValues().add(userInfo.getUserAddress().getFormatted());
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = userInfo.getUserAddress().getFormatted();
                    }
                    break;

                case UserInfo.UPDATED_AT_CLAIM:
                    attrTO.getValues().add(Long.toString(userInfo.getUpdatedAt()));
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = Long.toString(userInfo.getUpdatedAt());
                    }
                    break;

                default:
                    String value = userInfo.getClaim(item.getExtAttrName()) == null
                            ? null
                            : userInfo.getClaim(item.getExtAttrName()).toString();
                    attrTO.getValues().add(value);
                    responseTO.getAttrs().add(attrTO);
                    if (item.isConnObjectKey()) {
                        keyValue = value;
                    }
            }
        }

        final List<String> matchingUsers = keyValue == null
                ? List.of()
                : userManager.findMatchingUser(keyValue, op.getConnObjectKeyItem().get());
        LOG.debug("Found {} matching users for {}", matchingUsers.size(), keyValue);

        String username;
        if (matchingUsers.isEmpty()) {
            if (op.isCreateUnmatching()) {
                LOG.debug("No user matching {}, about to create", keyValue);

                final String emailValue = userInfo.getEmail();
                username = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                        () -> userManager.create(op, responseTO, emailValue));
            } else if (op.isSelfRegUnmatching()) {
                UserTO userTO = new UserTO();

                userManager.fill(op, responseTO, userTO);

                responseTO.getAttrs().clear();
                responseTO.getAttrs().addAll(userTO.getPlainAttrs());
                responseTO.getAttrs().addAll(userTO.getVirAttrs());
                if (StringUtils.isNotBlank(userTO.getUsername())) {
                    responseTO.setUsername(userTO.getUsername());
                }

                responseTO.setSelfReg(true);

                return responseTO;
            } else {
                throw new NotFoundException(Optional.ofNullable(keyValue)
                    .map(value -> "User matching the provided value " + value)
                    .orElse("User marching the provided claims"));
            }
        } else if (matchingUsers.size() > 1) {
            throw new IllegalArgumentException("Several users match the provided value " + keyValue);
        } else {
            if (op.isUpdateMatching()) {
                LOG.debug("About to update {} for {}", matchingUsers.get(0), keyValue);

                username = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                        () -> userManager.update(matchingUsers.get(0), op, responseTO));
            } else {
                username = matchingUsers.get(0);
            }

        }

        responseTO.setUsername(username);

        // 4. generate JWT for further access
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_OP_ENTITYID, idToken.getIssuer());
        claims.put(JWT_CLAIM_USERID, idToken.getSubject());

        byte[] authorities = null;
        try {
            authorities = ENCRYPTOR.encode(POJOHelper.serialize(
                    authDataAccessor.getAuthorities(responseTO.getUsername())), CipherAlgorithm.AES).
                    getBytes();
        } catch (Exception e) {
            LOG.error("Could not fetch authorities", e);
        }

        Pair<String, Date> accessTokenInfo =
                accessTokenDataBinder.create(responseTO.getUsername(), claims, authorities, true);
        responseTO.setAccessToken(accessTokenInfo.getLeft());
        responseTO.setAccessTokenExpiryTime(accessTokenInfo.getRight());

        return responseTO;
    }

    private static TokenEndpointResponse getOIDCTokens(final String url, final String body) throws IOException {
        Response response = WebClient.create(url, List.of(new JacksonJsonProvider())).
                type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON).
                post(body);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            LOG.error("Unexpected response from OIDC Provider: {}\n{}\n{}",
                    response.getStatus(), response.getHeaders(),
                    IOUtils.toString((InputStream) response.getEntity()));

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add("Unexpected response from OIDC Provider");
            throw sce;
        }

        return response.readEntity(TokenEndpointResponse.class);
    }

    private static IdToken getValidatedIdToken(final OIDCProvider op, final Consumer consumer,
                                               final String jwtIdToken) {
        IdTokenReader idTokenReader = new IdTokenReader();
        idTokenReader.setClockOffset(10);
        idTokenReader.setIssuerId(op.getIssuer());
        idTokenReader.setJwkSetClient(WebClient.create(op.getJwksUri(), List.of(new JsonWebKeysProvider())).
                accept(MediaType.APPLICATION_JSON));
        IdToken idToken;
        try {
            idToken = idTokenReader.getIdToken(jwtIdToken, consumer);
        } catch (Exception e) {
            LOG.error("While validating the id_token", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        return idToken;
    }

    private static UserInfo getUserInfo(
        final String endpoint,
        final String accessToken,
        final IdToken idToken,
        final Consumer consumer) {

        WebClient userInfoServiceClient = WebClient.create(endpoint, List.of(new JsonMapObjectProvider())).
                accept(MediaType.APPLICATION_JSON);
        ClientAccessToken clientAccessToken =
                new ClientAccessToken(OAuthConstants.BEARER_AUTHORIZATION_SCHEME, accessToken);
        UserInfoClient userInfoClient = new UserInfoClient();
        userInfoClient.setUserInfoServiceClient(userInfoServiceClient);
        UserInfo userInfo = null;
        try {
            userInfo = userInfoClient.getUserInfo(clientAccessToken, idToken, consumer);
        } catch (Exception e) {
            LOG.error("While getting the userInfo", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
        return userInfo;
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "'))")
    public OIDCLogoutRequestTO createLogoutRequest(final String op) {
        OIDCLogoutRequestTO logoutRequest = new OIDCLogoutRequestTO();
        logoutRequest.setEndSessionEndpoint(getOIDCProvider(op).getEndSessionEndpoint());
        return logoutRequest;
    }

    @Override
    protected EntityTO resolveReference(
            final Method method, final Object... args) throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
