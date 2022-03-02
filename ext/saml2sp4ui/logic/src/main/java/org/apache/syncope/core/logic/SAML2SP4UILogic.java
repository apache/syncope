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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.init.SAML2SP4UILoader;
import org.apache.syncope.core.logic.saml2.NoOpSessionStore;
import org.apache.syncope.core.logic.saml2.SAML2ClientCache;
import org.apache.syncope.core.logic.saml2.SAML2SP4UIContext;
import org.apache.syncope.core.logic.saml2.SAML2SP4UIUserManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.RequestedAuthnContextProvider;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusCode;
import org.pac4j.core.context.session.SessionStore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.Encryptor;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.logout.NoLogoutActionBuilder;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.context.SAML2MessageContext;
import org.pac4j.saml.credentials.SAML2Credentials;
import org.pac4j.saml.credentials.authenticator.SAML2Authenticator;
import org.pac4j.saml.metadata.SAML2ServiceProviderMetadataResolver;
import org.pac4j.saml.profile.SAML2Profile;
import org.pac4j.saml.redirect.SAML2RedirectionActionBuilder;
import org.pac4j.saml.sso.impl.SAML2AuthnRequestBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ResourceUtils;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdPItem;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;

public class SAML2SP4UILogic extends AbstractTransactionalLogic<EntityTO> {

    protected static final String JWT_CLAIM_IDP_ENTITYID = "IDP_ENTITYID";

    protected static final String JWT_CLAIM_NAMEID_FORMAT = "NAMEID_FORMAT";

    protected static final String JWT_CLAIM_NAMEID_VALUE = "NAMEID_VALUE";

    protected static final String JWT_CLAIM_SESSIONINDEX = "SESSIONINDEX";

    protected static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    protected final SAML2SP4UILoader loader;

    protected final AccessTokenDataBinder accessTokenDataBinder;

    protected final SAML2ClientCache saml2ClientCache;

    protected final SAML2SP4UIUserManager userManager;

    protected final SAML2SP4UIIdPDAO idpDAO;

    protected final AuthDataAccessor authDataAccessor;

    protected final Map<String, String> metadataCache = new ConcurrentHashMap<>();

    public SAML2SP4UILogic(
            final SAML2SP4UILoader loader,
            final AccessTokenDataBinder accessTokenDataBinder,
            final SAML2ClientCache saml2ClientCache,
            final SAML2SP4UIUserManager userManager,
            final SAML2SP4UIIdPDAO idpDAO,
            final AuthDataAccessor authDataAccessor) {

        this.loader = loader;
        this.accessTokenDataBinder = accessTokenDataBinder;
        this.saml2ClientCache = saml2ClientCache;
        this.userManager = userManager;
        this.idpDAO = idpDAO;
        this.authDataAccessor = authDataAccessor;
    }

    protected static String validateUrl(final String url) {
        boolean isValid = true;
        if (url.contains("..")) {
            isValid = false;
        }
        if (isValid) {
            isValid = ResourceUtils.isUrl(url);
        }

        if (!isValid) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add("Invalid URL: " + url);
            throw sce;
        }

        return url;
    }

    protected static String getCallbackUrl(final String spEntityID, final String urlContext) {
        return validateUrl(spEntityID + urlContext + "/assertion-consumer");
    }

    @PreAuthorize("isAuthenticated()")
    public void getMetadata(final String spEntityID, final String urlContext, final OutputStream os) {
        String metadata = metadataCache.get(spEntityID + urlContext);
        if (metadata == null) {
            SAML2Configuration cfg = loader.newSAML2Configuration();
            cfg.setServiceProviderEntityId(spEntityID);
            cfg.setCallbackUrl(getCallbackUrl(spEntityID, urlContext));
            SAML2ClientCache.getSPMetadataPath(spEntityID).ifPresent(cfg::setServiceProviderMetadataResourceFilepath);

            EntityDescriptor entityDescriptor =
                    (EntityDescriptor) new SAML2ServiceProviderMetadataResolver(cfg).getEntityDescriptorElement();

            AssertionConsumerService postACS = entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).
                    getAssertionConsumerServices().get(0);

            AssertionConsumerService redirectACS = new AssertionConsumerServiceBuilder().buildObject();
            BeanUtils.copyProperties(postACS, redirectACS);
            postACS.setBinding(SAML2BindingType.REDIRECT.getUri());
            postACS.setIndex(1);
            entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).
                    getAssertionConsumerServices().add(redirectACS);

            entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleLogoutServices().
                    removeIf(slo -> !SAML2BindingType.POST.getUri().equals(slo.getBinding())
                    && !SAML2BindingType.REDIRECT.getUri().equals(slo.getBinding()));
            entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleLogoutServices().
                    forEach(slo -> slo.setLocation(
                    getCallbackUrl(spEntityID, urlContext).replace("/assertion-consumer", "/logout")));

            try {
                metadata = cfg.toMetadataGenerator().getMetadata(entityDescriptor);
                metadataCache.put(spEntityID + urlContext, metadata);
            } catch (Exception e) {
                LOG.error("While generating SP metadata", e);
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                sce.getElements().add(e.getMessage());
                throw sce;
            }
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(os)) {
            osw.write(metadata);
        } catch (Exception e) {
            LOG.error("While getting SP metadata", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    protected SAML2Client getSAML2Client(final SAML2SP4UIIdP idp, final String spEntityID, final String urlContext) {
        return saml2ClientCache.get(idp.getEntityID(), spEntityID).
                orElseGet(() -> saml2ClientCache.add(
                idp, loader.newSAML2Configuration(), spEntityID, getCallbackUrl(spEntityID, urlContext)));
    }

    protected SAML2Client getSAML2Client(final String idpEntityID, final String spEntityID, final String urlContext) {
        SAML2SP4UIIdP idp = idpDAO.findByEntityID(idpEntityID);
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + idpEntityID + '\'');
        }

        return getSAML2Client(idp, spEntityID, urlContext);
    }

    protected static SAML2Request buildRequest(final String idpEntityID, final RedirectionAction action) {
        SAML2Request requestTO = new SAML2Request();
        requestTO.setIdpEntityID(idpEntityID);
        if (action instanceof WithLocationAction) {
            WithLocationAction withLocationAction = (WithLocationAction) action;

            requestTO.setBindingType(SAML2BindingType.REDIRECT);
            requestTO.setContent(withLocationAction.getLocation());
        } else if (action instanceof WithContentAction) {
            WithContentAction withContentAction = (WithContentAction) action;

            requestTO.setBindingType(SAML2BindingType.POST);
            requestTO.setContent(Base64.getMimeEncoder().encodeToString(withContentAction.getContent().getBytes()));
        }
        return requestTO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2Request createLoginRequest(
            final String spEntityID,
            final String urlContext,
            final String idpEntityID) {

        // 0. look for IdP
        SAML2SP4UIIdP idp = idpDAO.findByEntityID(idpEntityID);
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + idpEntityID + '\'');
        }

        // 1. look for configured client
        SAML2Client saml2Client = getSAML2Client(idp, spEntityID, urlContext);

        if (idp.getRequestedAuthnContextProvider() != null) {
            RequestedAuthnContextProvider requestedAuthnContextProvider = null;
            try {
                requestedAuthnContextProvider = ImplementationManager.build(idp.getRequestedAuthnContextProvider());
            } catch (Exception e) {
                LOG.warn("Cannot instantiate '{}', reverting to default behavior",
                        idp.getRequestedAuthnContextProvider(), e);
            }

            if (requestedAuthnContextProvider != null) {
                RequestedAuthnContext requestedAuthnContext = requestedAuthnContextProvider.get();
                saml2Client.setRedirectionActionBuilder(new SAML2RedirectionActionBuilder(saml2Client) {

                    @Override
                    public Optional<RedirectionAction> getRedirectionAction(
                            final WebContext wc, final SessionStore sessionStore) {

                        this.saml2ObjectBuilder = new SAML2AuthnRequestBuilder() {

                            @Override
                            public AuthnRequest build(final SAML2MessageContext context) {
                                AuthnRequest authnRequest = super.build(context);
                                authnRequest.setRequestedAuthnContext(requestedAuthnContext);
                                return authnRequest;
                            }
                        };
                        return super.getRedirectionAction(wc, sessionStore);
                    }
                });
            }
        }

        // 2. create AuthnRequest
        SAML2SP4UIContext ctx = new SAML2SP4UIContext(
                saml2Client.getConfiguration().getAuthnRequestBindingType(),
                null);
        RedirectionAction action = saml2Client.getRedirectionAction(ctx, NoOpSessionStore.INSTANCE).
                orElseThrow(() -> {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                    sce.getElements().add("No RedirectionAction generated for AuthnRequest");
                    return sce;
                });
        return buildRequest(idpEntityID, action);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2LoginResponse validateLoginResponse(final SAML2Response saml2Response) {
        // 0. look for IdP
        SAML2SP4UIIdP idp = idpDAO.findByEntityID(saml2Response.getIdpEntityID());
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + saml2Response.getIdpEntityID() + '\'');
        }

        // 1. look for configured client
        SAML2Client saml2Client = getSAML2Client(idp, saml2Response.getSpEntityID(), saml2Response.getUrlContext());

        // 2. validate the provided SAML response
        SAML2Credentials credentials;
        try {
            SAML2SP4UIContext ctx = new SAML2SP4UIContext(
                    saml2Client.getConfiguration().getAuthnRequestBindingType(),
                    saml2Response);

            credentials = (SAML2Credentials) saml2Client.getCredentialsExtractor().
                    extract(ctx, NoOpSessionStore.INSTANCE).
                    orElseThrow(() -> new IllegalStateException("No AuthnResponse found"));

            saml2Client.getAuthenticator().validate(credentials, ctx, NoOpSessionStore.INSTANCE);
        } catch (Exception e) {
            LOG.error("While validating AuthnResponse", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 3. prepare the result: find matching user (if any) and return the received attributes
        SAML2LoginResponse loginResp = new SAML2LoginResponse();
        loginResp.setIdp(saml2Client.getIdentityProviderResolvedEntityId());
        loginResp.setSloSupported(!(saml2Client.getLogoutActionBuilder() instanceof NoLogoutActionBuilder));

        SAML2Credentials.SAMLNameID nameID = credentials.getNameId();

        SAML2SP4UIIdPItem connObjectKeyItem = idp.getConnObjectKeyItem().orElse(null);

        String keyValue = null;
        if (StringUtils.isNotBlank(nameID.getValue())
                && connObjectKeyItem != null
                && connObjectKeyItem.getExtAttrName().equals(NameID.DEFAULT_ELEMENT_LOCAL_NAME)) {

            keyValue = nameID.getValue();
        }

        loginResp.setNotOnOrAfter(new Date(credentials.getConditions().getNotOnOrAfter().toInstant().toEpochMilli()));

        loginResp.setSessionIndex(credentials.getSessionIndex());

        for (SAML2Credentials.SAMLAttribute attr : credentials.getAttributes()) {
            if (!attr.getAttributeValues().isEmpty()) {
                String attrName = attr.getFriendlyName() == null ? attr.getName() : attr.getFriendlyName();
                if (connObjectKeyItem != null && attrName.equals(connObjectKeyItem.getExtAttrName())) {
                    keyValue = attr.getAttributeValues().get(0);
                }

                loginResp.getAttrs().add(new Attr.Builder(attrName).values(attr.getAttributeValues()).build());
            }
        }

        List<String> matchingUsers = keyValue == null
                ? List.of()
                : userManager.findMatchingUser(keyValue, idp.getKey());
        LOG.debug("Found {} matching users for {}", matchingUsers.size(), keyValue);

        String username;
        if (matchingUsers.isEmpty()) {
            if (idp.isCreateUnmatching()) {
                LOG.debug("No user matching {}, about to create", keyValue);

                username = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                        () -> userManager.create(idp, loginResp, nameID.getValue()));
            } else if (idp.isSelfRegUnmatching()) {
                loginResp.setNameID(nameID.getValue());
                UserTO userTO = new UserTO();

                userManager.fill(idp.getKey(), loginResp, userTO);

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
                throw new NotFoundException("User matching the provided value " + keyValue);
            }
        } else if (matchingUsers.size() > 1) {
            throw new IllegalArgumentException("Several users match the provided value " + keyValue);
        } else {
            if (idp.isUpdateMatching()) {
                LOG.debug("About to update {} for {}", matchingUsers.get(0), keyValue);

                username = AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(),
                        () -> userManager.update(matchingUsers.get(0), idp, loginResp));
            } else {
                username = matchingUsers.get(0);
            }
        }

        loginResp.setUsername(username);
        loginResp.setNameID(nameID.getValue());

        // 4. generate JWT for further access
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_IDP_ENTITYID, idp.getEntityID());
        claims.put(JWT_CLAIM_NAMEID_FORMAT, nameID.getFormat());
        claims.put(JWT_CLAIM_NAMEID_VALUE, nameID.getValue());
        claims.put(JWT_CLAIM_SESSIONINDEX, loginResp.getSessionIndex());

        byte[] authorities = null;
        try {
            authorities = ENCRYPTOR.encode(POJOHelper.serialize(
                    authDataAccessor.getAuthorities(loginResp.getUsername(), null)), CipherAlgorithm.AES).getBytes();
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
    public SAML2Request createLogoutRequest(
            final String accessToken,
            final String spEntityID,
            final String urlContext) {

        // 1. fetch the current JWT used for Syncope authentication
        JWTClaimsSet claimsSet;
        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);
            claimsSet = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAccessToken);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 2. look for SAML2Client
        String idpEntityID = (String) claimsSet.getClaim(JWT_CLAIM_IDP_ENTITYID);
        if (idpEntityID == null) {
            throw new NotFoundException("No SAML 2.0 IdP information found in the access token");
        }
        SAML2Client saml2Client = getSAML2Client(idpEntityID, spEntityID, urlContext);
        if (saml2Client.getLogoutActionBuilder() instanceof NoLogoutActionBuilder) {
            throw new IllegalArgumentException("No SingleLogoutService available for "
                    + saml2Client.getIdentityProviderResolvedEntityId());
        }

        // 3. create LogoutRequest
        SAML2Profile saml2Profile = new SAML2Profile();
        saml2Profile.setId((String) claimsSet.getClaim(JWT_CLAIM_NAMEID_VALUE));
        saml2Profile.addAuthenticationAttribute(
                SAML2Authenticator.SAML_NAME_ID_FORMAT,
                claimsSet.getClaim(JWT_CLAIM_NAMEID_FORMAT));
        saml2Profile.addAuthenticationAttribute(
                SAML2Authenticator.SESSION_INDEX,
                claimsSet.getClaim(JWT_CLAIM_SESSIONINDEX));

        SAML2SP4UIContext ctx = new SAML2SP4UIContext(
                saml2Client.getConfiguration().getSpLogoutRequestBindingType(), null);
        RedirectionAction action = saml2Client.getLogoutAction(
                ctx,
                NoOpSessionStore.INSTANCE,
                saml2Profile, null).
                orElseThrow(() -> {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                    sce.getElements().add("No RedirectionAction generated for LogoutRequest");
                    return sce;
                });
        return buildRequest(idpEntityID, action);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void validateLogoutResponse(final SAML2Response saml2Response) {
        // 1. look for SAML2Client
        if (saml2Response.getIdpEntityID() == null) {
            LOG.error("No SAML 2.0 IdP entityID provided, ignoring");
            return;
        }
        SAML2Client saml2Client = getSAML2Client(
                saml2Response.getIdpEntityID(), saml2Response.getSpEntityID(), saml2Response.getUrlContext());

        SAML2SP4UIIdP idp = idpDAO.findByEntityID(saml2Client.getIdentityProviderResolvedEntityId());
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + saml2Client.getIdentityProviderResolvedEntityId() + '\'');
        }

        // 2. validate the provided SAML response
        SAML2SP4UIContext ctx = new SAML2SP4UIContext(
                saml2Client.getConfiguration().getSpLogoutRequestBindingType(),
                saml2Response);

        LogoutResponse logoutResponse;
        try {
            SAML2MessageContext saml2Ctx = saml2Client.getContextProvider().
                    buildContext(saml2Client, ctx, NoOpSessionStore.INSTANCE);
            saml2Client.getLogoutProfileHandler().receive(saml2Ctx);

            logoutResponse = (LogoutResponse) saml2Ctx.getMessageContext().getMessage();
        } catch (Exception e) {
            LOG.error("Could not validate LogoutResponse", e);
            return;
        }

        // 3. finally check for logout status
        if (!StatusCode.SUCCESS.equals(logoutResponse.getStatus().getStatusCode().getValue())) {
            LOG.warn("Logout from SAML 2.0 IdP '{}' was not successful",
                    saml2Client.getIdentityProviderResolvedEntityId());
        }
    }

    @Override
    protected EntityTO resolveReference(
            final Method method, final Object... args) throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
