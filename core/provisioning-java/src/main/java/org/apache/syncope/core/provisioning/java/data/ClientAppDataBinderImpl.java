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

import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.CASSPClientAppTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;

public class ClientAppDataBinderImpl implements ClientAppDataBinder {

    protected final PolicyDAO policyDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final EntityFactory entityFactory;

    public ClientAppDataBinderImpl(
            final PolicyDAO policyDAO,
            final RealmSearchDAO realmSearchDAO,
            final EntityFactory entityFactory) {

        this.policyDAO = policyDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClientApp> T create(final ClientAppTO clientAppTO) {
        if (clientAppTO instanceof SAML2SPClientAppTO sAML2SPClientAppTO) {
            return (T) doCreate(sAML2SPClientAppTO);
        }
        if (clientAppTO instanceof OIDCRPClientAppTO oIDCRPClientAppTO) {
            return (T) doCreate(oIDCRPClientAppTO);
        }
        if (clientAppTO instanceof CASSPClientAppTO cASSPClientAppTO) {
            return (T) doCreate(cASSPClientAppTO);
        }

        throw new IllegalArgumentException("Unsupported client app: " + clientAppTO.getClass().getName());
    }

    @Override
    public <T extends ClientApp> void update(final T clientApp, final ClientAppTO clientAppTO) {
        if (clientAppTO instanceof SAML2SPClientAppTO sAML2SPClientAppTO) {
            doUpdate((SAML2SPClientApp) clientApp, sAML2SPClientAppTO);
        } else if (clientAppTO instanceof OIDCRPClientAppTO oIDCRPClientAppTO) {
            doUpdate((OIDCRPClientApp) clientApp, oIDCRPClientAppTO);
        } else if (clientAppTO instanceof CASSPClientAppTO cASSPClientAppTO) {
            doUpdate((CASSPClientApp) clientApp, cASSPClientAppTO);
        } else {
            throw new IllegalArgumentException("Unsupported client app: " + clientAppTO.getClass().getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClientAppTO> T getClientAppTO(final ClientApp clientApp) {
        if (clientApp instanceof SAML2SPClientApp sAML2SPClientApp) {
            return (T) getSAMLClientAppTO(sAML2SPClientApp);
        }
        if (clientApp instanceof OIDCRPClientApp oIDCRPClientApp) {
            return (T) getOIDCClientAppTO(oIDCRPClientApp);
        }
        if (clientApp instanceof CASSPClientApp cASSPClientApp) {
            return (T) getCASClientAppTO(cASSPClientApp);
        }
        throw new IllegalArgumentException("Unsupported client app: " + clientApp.getClass().getName());
    }

    protected SAML2SPClientApp doCreate(final SAML2SPClientAppTO clientAppTO) {
        SAML2SPClientApp saml2sp = entityFactory.newEntity(SAML2SPClientApp.class);
        doUpdate(saml2sp, clientAppTO);
        return saml2sp;
    }

    protected CASSPClientApp doCreate(final CASSPClientAppTO clientAppTO) {
        CASSPClientApp saml2sp = entityFactory.newEntity(CASSPClientApp.class);
        doUpdate(saml2sp, clientAppTO);
        return saml2sp;
    }

    protected void doUpdate(final SAML2SPClientApp clientApp, final SAML2SPClientAppTO clientAppTO) {
        copyToEntity(clientApp, clientAppTO);

        clientApp.setEntityId(clientAppTO.getEntityId());
        clientApp.setMetadataLocation(clientAppTO.getMetadataLocation());
        clientApp.setMetadataSignatureLocation(clientAppTO.getMetadataSignatureLocation());
        clientApp.setSignAssertions(clientAppTO.isSignAssertions());
        clientApp.setSignResponses(clientAppTO.isSignResponses());
        clientApp.setEncryptionOptional(clientAppTO.isEncryptionOptional());
        clientApp.setEncryptAssertions(clientAppTO.isEncryptAssertions());
        clientApp.setRequiredAuthenticationContextClass(clientAppTO.getRequiredAuthenticationContextClass());
        clientApp.setRequiredNameIdFormat(clientAppTO.getRequiredNameIdFormat());
        clientApp.setSkewAllowance(clientAppTO.getSkewAllowance());
        clientApp.setNameIdQualifier(clientAppTO.getNameIdQualifier());
        clientApp.getAssertionAudiences().clear();
        clientApp.getAssertionAudiences().addAll(clientAppTO.getAssertionAudiences());
        clientApp.setServiceProviderNameIdQualifier(clientAppTO.getServiceProviderNameIdQualifier());

        clientApp.getSigningSignatureAlgorithms().clear();
        clientApp.getSigningSignatureAlgorithms().addAll(
                clientAppTO.getSigningSignatureAlgorithms());
        clientApp.getSigningSignatureReferenceDigestMethods().clear();
        clientApp.getSigningSignatureReferenceDigestMethods().addAll(
                clientAppTO.getSigningSignatureReferenceDigestMethods());
        clientApp.getEncryptionKeyAlgorithms().clear();
        clientApp.getEncryptionKeyAlgorithms().addAll(
                clientAppTO.getEncryptionKeyAlgorithms());
        clientApp.getEncryptionDataAlgorithms().clear();
        clientApp.getEncryptionDataAlgorithms().addAll(
                clientAppTO.getEncryptionDataAlgorithms());

        clientApp.getSigningSignatureBlackListedAlgorithms().clear();
        clientApp.getSigningSignatureBlackListedAlgorithms().
                addAll(clientAppTO.getSigningSignatureBlackListedAlgorithms());
        clientApp.getEncryptionBlackListedAlgorithms().clear();
        clientApp.getEncryptionBlackListedAlgorithms().addAll(
                clientAppTO.getEncryptionBlackListedAlgorithms());
    }

    protected void copyToTO(final ClientApp clientApp, final ClientAppTO clientAppTO) {
        clientAppTO.setKey(clientApp.getKey());
        clientAppTO.setRealm(Optional.ofNullable(clientApp.getRealm()).map(Realm::getFullPath).orElse(null));
        clientAppTO.setName(clientApp.getName());
        clientAppTO.setClientAppId(clientApp.getClientAppId());
        clientAppTO.setEvaluationOrder(clientApp.getEvaluationOrder());
        clientAppTO.setDescription(clientApp.getDescription());
        clientAppTO.setLogo(clientApp.getLogo());
        clientAppTO.setTheme(clientApp.getTheme());
        clientAppTO.setInformationUrl(clientApp.getInformationUrl());
        clientAppTO.setPrivacyUrl(clientApp.getPrivacyUrl());
        clientAppTO.setUsernameAttributeProviderConf(clientApp.getUsernameAttributeProviderConf());

        clientAppTO.setAuthPolicy(Optional.ofNullable(clientApp.getAuthPolicy()).
                map(AuthPolicy::getKey).orElse(null));
        clientAppTO.setAccessPolicy(Optional.ofNullable(clientApp.getAccessPolicy()).
                map(AccessPolicy::getKey).orElse(null));
        clientAppTO.setAttrReleasePolicy(Optional.ofNullable(clientApp.getAttrReleasePolicy()).
                map(AttrReleasePolicy::getKey).orElse(null));
        clientAppTO.setTicketExpirationPolicy(Optional.ofNullable(clientApp.getTicketExpirationPolicy()).
                map(TicketExpirationPolicy::getKey).orElse(null));

        clientAppTO.getProperties().addAll(clientApp.getProperties());
        clientAppTO.setLogoutType(clientApp.getLogoutType());
    }

    protected SAML2SPClientAppTO getSAMLClientAppTO(final SAML2SPClientApp clientApp) {
        SAML2SPClientAppTO clientAppTO = new SAML2SPClientAppTO();
        copyToTO(clientApp, clientAppTO);

        clientAppTO.setEntityId(clientApp.getEntityId());
        clientAppTO.setMetadataLocation(clientApp.getMetadataLocation());
        clientAppTO.setMetadataSignatureLocation(clientApp.getMetadataSignatureLocation());
        clientAppTO.setSignAssertions(clientApp.isSignAssertions());
        clientAppTO.setSignResponses(clientApp.isSignResponses());
        clientAppTO.setEncryptionOptional(clientApp.isEncryptionOptional());
        clientAppTO.setEncryptAssertions(clientApp.isEncryptAssertions());
        clientAppTO.setRequiredAuthenticationContextClass(clientApp.getRequiredAuthenticationContextClass());
        clientAppTO.setRequiredNameIdFormat(clientApp.getRequiredNameIdFormat());
        clientAppTO.setSkewAllowance(clientApp.getSkewAllowance());
        clientAppTO.setNameIdQualifier(clientApp.getNameIdQualifier());
        clientAppTO.getAssertionAudiences().addAll(clientApp.getAssertionAudiences());
        clientAppTO.setServiceProviderNameIdQualifier(clientApp.getServiceProviderNameIdQualifier());

        clientAppTO.getSigningSignatureAlgorithms().addAll(
                clientApp.getSigningSignatureAlgorithms());
        clientAppTO.getSigningSignatureReferenceDigestMethods().addAll(
                clientApp.getSigningSignatureReferenceDigestMethods());
        clientAppTO.getEncryptionKeyAlgorithms().addAll(
                clientApp.getEncryptionKeyAlgorithms());
        clientAppTO.getEncryptionDataAlgorithms().addAll(
                clientApp.getEncryptionDataAlgorithms());

        clientAppTO.getSigningSignatureBlackListedAlgorithms().addAll(
                clientApp.getSigningSignatureBlackListedAlgorithms());
        clientAppTO.getEncryptionBlackListedAlgorithms().addAll(
                clientApp.getEncryptionBlackListedAlgorithms());

        return clientAppTO;
    }

    protected OIDCRPClientApp doCreate(final OIDCRPClientAppTO clientAppTO) {
        OIDCRPClientApp oidcrp = entityFactory.newEntity(OIDCRPClientApp.class);
        doUpdate(oidcrp, clientAppTO);
        return oidcrp;
    }

    protected void doUpdate(final OIDCRPClientApp clientApp, final OIDCRPClientAppTO clientAppTO) {
        copyToEntity(clientApp, clientAppTO);

        clientApp.setClientId(clientAppTO.getClientId());
        clientApp.setClientSecret(clientAppTO.getClientSecret());
        clientApp.setIdTokenIssuer(clientAppTO.getIdTokenIssuer());
        clientApp.setSignIdToken(clientAppTO.isSignIdToken());
        clientApp.setIdTokenSigningAlg(clientAppTO.getIdTokenSigningAlg());
        clientApp.setEncryptIdToken(clientAppTO.isEncryptIdToken());
        clientApp.setIdTokenEncryptionAlg(clientAppTO.getIdTokenEncryptionAlg());
        clientApp.setIdTokenEncryptionEncoding(clientAppTO.getIdTokenEncryptionEncoding());
        clientApp.setUserInfoSigningAlg(clientAppTO.getUserInfoSigningAlg());
        clientApp.setUserInfoEncryptedResponseAlg(clientAppTO.getUserInfoEncryptedResponseAlg());
        clientApp.setUserInfoEncryptedResponseEncoding(clientAppTO.getUserInfoEncryptedResponseEncoding());
        clientApp.setJwtAccessToken(clientAppTO.isJwtAccessToken());
        clientApp.setBypassApprovalPrompt(clientAppTO.isBypassApprovalPrompt());
        clientApp.setGenerateRefreshToken(clientAppTO.isGenerateRefreshToken());
        clientApp.setSubjectType(clientAppTO.getSubjectType());
        clientApp.setApplicationType(clientAppTO.getApplicationType());
        clientApp.getRedirectUris().clear();
        clientApp.getRedirectUris().addAll(clientAppTO.getRedirectUris());
        clientApp.getSupportedGrantTypes().clear();
        clientApp.getSupportedGrantTypes().addAll(clientAppTO.getSupportedGrantTypes());
        clientApp.getSupportedResponseTypes().clear();
        clientApp.getSupportedResponseTypes().addAll(clientAppTO.getSupportedResponseTypes());
        clientApp.getScopes().clear();
        clientApp.getScopes().addAll(clientAppTO.getScopes());
        clientApp.setLogoutUri(clientAppTO.getLogoutUri());
        clientApp.setJwks(clientAppTO.getJwks());
        clientApp.setJwksUri(clientAppTO.getJwksUri());
        clientApp.setTokenEndpointAuthenticationMethod(clientAppTO.getTokenEndpointAuthenticationMethod());
    }

    protected OIDCRPClientAppTO getOIDCClientAppTO(final OIDCRPClientApp clientApp) {
        OIDCRPClientAppTO clientAppTO = new OIDCRPClientAppTO();
        copyToTO(clientApp, clientAppTO);

        clientAppTO.setClientId(clientApp.getClientId());
        clientAppTO.setClientSecret(clientApp.getClientSecret());
        clientAppTO.setIdTokenIssuer(clientApp.getIdTokenIssuer());
        clientAppTO.setSignIdToken(clientApp.isSignIdToken());
        clientAppTO.setIdTokenSigningAlg(clientApp.getIdTokenSigningAlg());
        clientAppTO.setEncryptIdToken(clientApp.isEncryptIdToken());
        clientAppTO.setIdTokenEncryptionAlg(clientApp.getIdTokenEncryptionAlg());
        clientAppTO.setIdTokenEncryptionEncoding(clientApp.getIdTokenEncryptionEncoding());
        clientAppTO.setUserInfoSigningAlg(clientApp.getUserInfoSigningAlg());
        clientAppTO.setUserInfoEncryptedResponseAlg(clientApp.getUserInfoEncryptedResponseAlg());
        clientAppTO.setUserInfoEncryptedResponseEncoding(clientApp.getUserInfoEncryptedResponseEncoding());
        clientAppTO.setJwtAccessToken(clientApp.isJwtAccessToken());
        clientAppTO.setBypassApprovalPrompt(clientApp.isBypassApprovalPrompt());
        clientAppTO.setGenerateRefreshToken(clientApp.isGenerateRefreshToken());
        clientAppTO.setSubjectType(clientApp.getSubjectType());
        clientAppTO.setApplicationType(clientApp.getApplicationType());
        clientAppTO.getRedirectUris().addAll(clientApp.getRedirectUris());
        clientAppTO.getSupportedGrantTypes().clear();
        clientAppTO.getSupportedGrantTypes().addAll(clientApp.getSupportedGrantTypes());
        clientAppTO.getSupportedResponseTypes().clear();
        clientAppTO.getSupportedResponseTypes().addAll(clientApp.getSupportedResponseTypes());
        clientAppTO.getScopes().addAll(clientApp.getScopes());
        clientAppTO.setLogoutUri(clientApp.getLogoutUri());
        clientAppTO.setJwks(clientApp.getJwks());
        clientAppTO.setJwksUri(clientApp.getJwksUri());
        clientAppTO.setTokenEndpointAuthenticationMethod(clientApp.getTokenEndpointAuthenticationMethod());

        return clientAppTO;
    }

    protected void doUpdate(final CASSPClientApp clientApp, final CASSPClientAppTO clientAppTO) {
        copyToEntity(clientApp, clientAppTO);

        clientApp.setServiceId(clientAppTO.getServiceId());
    }

    protected CASSPClientAppTO getCASClientAppTO(final CASSPClientApp clientApp) {
        CASSPClientAppTO clientAppTO = new CASSPClientAppTO();
        copyToTO(clientApp, clientAppTO);
        clientAppTO.setServiceId(clientApp.getServiceId());
        return clientAppTO;
    }

    protected void copyToEntity(final ClientApp clientApp, final ClientAppTO clientAppTO) {
        Optional.ofNullable(clientAppTO.getRealm()).
                flatMap(realmSearchDAO::findByFullPath).
                ifPresentOrElse(clientApp::setRealm, () -> clientApp.setRealm(null));

        clientApp.setName(clientAppTO.getName());
        clientApp.setClientAppId(clientAppTO.getClientAppId());
        clientApp.setEvaluationOrder(clientAppTO.getEvaluationOrder());
        clientApp.setDescription(clientAppTO.getDescription());
        clientApp.setLogo(clientAppTO.getLogo());
        clientApp.setTheme(clientAppTO.getTheme());
        clientApp.setInformationUrl(clientAppTO.getInformationUrl());
        clientApp.setPrivacyUrl(clientAppTO.getPrivacyUrl());
        clientApp.setUsernameAttributeProviderConf(clientAppTO.getUsernameAttributeProviderConf());

        if (clientAppTO.getAuthPolicy() == null) {
            clientApp.setAuthPolicy(null);
        } else {
            Policy policy = policyDAO.findById(clientAppTO.getAuthPolicy()).orElse(null);
            if (policy instanceof AuthPolicy authPolicy) {
                clientApp.setAuthPolicy(authPolicy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AuthPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        if (clientAppTO.getAccessPolicy() == null) {
            clientApp.setAccessPolicy(null);
        } else {
            Policy policy = policyDAO.findById(clientAppTO.getAccessPolicy()).orElse(null);
            if (policy instanceof AccessPolicy accessPolicy) {
                clientApp.setAccessPolicy(accessPolicy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AccessPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        if (clientAppTO.getAttrReleasePolicy() == null) {
            clientApp.setAttrReleasePolicy(null);
        } else {
            Policy policy = policyDAO.findById(clientAppTO.getAttrReleasePolicy()).orElse(null);
            if (policy instanceof AttrReleasePolicy attrReleasePolicy) {
                clientApp.setAttrReleasePolicy(attrReleasePolicy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AttrReleasePolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        if (clientAppTO.getTicketExpirationPolicy() == null) {
            clientApp.setTicketExpirationPolicy(null);
        } else {
            Policy policy = policyDAO.findById(clientAppTO.getTicketExpirationPolicy()).orElse(null);
            if (policy instanceof TicketExpirationPolicy ticketExpirationPolicy) {
                clientApp.setTicketExpirationPolicy(ticketExpirationPolicy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + TicketExpirationPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        clientApp.setProperties(clientAppTO.getProperties());
        clientApp.setLogoutType(clientAppTO.getLogoutType());
    }
}
