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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.CASSPClientAppTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRPClientApp;

public class ClientAppDataBinderImpl implements ClientAppDataBinder {

    protected final PolicyDAO policyDAO;

    protected final EntityFactory entityFactory;

    public ClientAppDataBinderImpl(final PolicyDAO policyDAO, final EntityFactory entityFactory) {
        this.policyDAO = policyDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClientApp> T create(final ClientAppTO clientAppTO) {
        if (clientAppTO instanceof SAML2SPClientAppTO) {
            return (T) doCreate((SAML2SPClientAppTO) clientAppTO);
        } else if (clientAppTO instanceof OIDCRPClientAppTO) {
            return (T) doCreate((OIDCRPClientAppTO) clientAppTO);
        } else if (clientAppTO instanceof CASSPClientAppTO) {
            return (T) doCreate((CASSPClientAppTO) clientAppTO);
        } else {
            throw new IllegalArgumentException("Unsupported client app: " + clientAppTO.getClass().getName());
        }
    }

    @Override
    public <T extends ClientApp> void update(final T clientApp, final ClientAppTO clientAppTO) {
        if (clientAppTO instanceof SAML2SPClientAppTO) {
            doUpdate((SAML2SPClientApp) clientApp, (SAML2SPClientAppTO) clientAppTO);
        } else if (clientAppTO instanceof OIDCRPClientAppTO) {
            doUpdate((OIDCRPClientApp) clientApp, (OIDCRPClientAppTO) clientAppTO);
        } else if (clientAppTO instanceof CASSPClientAppTO) {
            doUpdate((CASSPClientApp) clientApp, (CASSPClientAppTO) clientAppTO);
        } else {
            throw new IllegalArgumentException("Unsupported client app: " + clientAppTO.getClass().getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClientAppTO> T getClientAppTO(final ClientApp clientApp) {
        if (clientApp instanceof SAML2SPClientApp) {
            return (T) getSAMLClientAppTO((SAML2SPClientApp) clientApp);
        }
        if (clientApp instanceof OIDCRPClientApp) {
            return (T) getOIDCClientAppTO((OIDCRPClientApp) clientApp);
        }
        if (clientApp instanceof CASSPClientApp) {
            return (T) getCASClientAppTO((CASSPClientApp) clientApp);
        }
        throw new IllegalArgumentException("Unsupported client app: " + clientApp.getClass().getName());
    }

    protected SAML2SPClientApp doCreate(final SAML2SPClientAppTO clientAppTO) {
        SAML2SPClientApp saml2sp = entityFactory.newEntity(SAML2SPClientApp.class);
        update(saml2sp, clientAppTO);
        return saml2sp;
    }

    protected CASSPClientApp doCreate(final CASSPClientAppTO clientAppTO) {
        CASSPClientApp saml2sp = entityFactory.newEntity(CASSPClientApp.class);
        update(saml2sp, clientAppTO);
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

    protected static void copyToTO(final ClientApp clientApp, final ClientAppTO clientAppTO) {
        clientAppTO.setName(clientApp.getName());
        clientAppTO.setKey(clientApp.getKey());
        clientAppTO.setDescription(clientApp.getDescription());
        clientAppTO.setClientAppId(clientApp.getClientAppId());
        clientAppTO.setTheme(clientApp.getTheme());

        if (clientApp.getAuthPolicy() != null) {
            clientAppTO.setAuthPolicy(clientApp.getAuthPolicy().getKey());
        }
        if (clientApp.getAccessPolicy() != null) {
            clientAppTO.setAccessPolicy(clientApp.getAccessPolicy().getKey());
        }
        if (clientApp.getAttrReleasePolicy() != null) {
            clientAppTO.setAttrReleasePolicy(clientApp.getAttrReleasePolicy().getKey());
        }

        clientAppTO.getProperties().addAll(clientApp.getProperties());
    }

    protected static SAML2SPClientAppTO getSAMLClientAppTO(final SAML2SPClientApp clientApp) {
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
        update(oidcrp, clientAppTO);
        return oidcrp;
    }

    protected void doUpdate(final OIDCRPClientApp clientApp, final OIDCRPClientAppTO clientAppTO) {
        copyToEntity(clientApp, clientAppTO);

        clientApp.setClientSecret(clientAppTO.getClientSecret());
        clientApp.setClientId(clientAppTO.getClientId());
        clientApp.setSignIdToken(clientAppTO.isSignIdToken());
        clientApp.setJwtAccessToken(clientAppTO.isJwtAccessToken());
        clientApp.setSubjectType(clientAppTO.getSubjectType());
        clientApp.getRedirectUris().clear();
        clientApp.getRedirectUris().addAll(clientAppTO.getRedirectUris());
        clientApp.getSupportedGrantTypes().clear();
        clientApp.getSupportedGrantTypes().addAll(clientAppTO.getSupportedGrantTypes());
        clientApp.getSupportedResponseTypes().clear();
        clientApp.getSupportedResponseTypes().addAll(clientAppTO.getSupportedResponseTypes());

        clientApp.setLogoutUri(clientAppTO.getLogoutUri());
    }

    protected static OIDCRPClientAppTO getOIDCClientAppTO(final OIDCRPClientApp clientApp) {
        OIDCRPClientAppTO clientAppTO = new OIDCRPClientAppTO();
        copyToTO(clientApp, clientAppTO);

        clientAppTO.setClientId(clientApp.getClientId());
        clientAppTO.setClientSecret(clientApp.getClientSecret());
        clientAppTO.setSignIdToken(clientApp.isSignIdToken());
        clientAppTO.setSubjectType(clientApp.getSubjectType());
        clientAppTO.getRedirectUris().addAll(clientApp.getRedirectUris());
        clientAppTO.getSupportedGrantTypes().addAll(clientApp.getSupportedGrantTypes());
        clientAppTO.getSupportedResponseTypes().addAll(clientApp.getSupportedResponseTypes());
        clientAppTO.setLogoutUri(clientApp.getLogoutUri());
        clientAppTO.setJwtAccessToken(clientApp.isJwtAccessToken());
        return clientAppTO;
    }

    protected void doUpdate(final CASSPClientApp clientApp, final CASSPClientAppTO clientAppTO) {
        copyToEntity(clientApp, clientAppTO);

        clientApp.setServiceId(clientAppTO.getServiceId());
    }

    protected static CASSPClientAppTO getCASClientAppTO(final CASSPClientApp clientApp) {
        CASSPClientAppTO clientAppTO = new CASSPClientAppTO();
        copyToTO(clientApp, clientAppTO);
        clientAppTO.setServiceId(clientApp.getServiceId());
        return clientAppTO;
    }

    protected void copyToEntity(final ClientApp clientApp, final ClientAppTO clientAppTO) {
        clientApp.setName(clientAppTO.getName());
        clientApp.setClientAppId(clientAppTO.getClientAppId());
        clientApp.setDescription(clientAppTO.getDescription());
        clientApp.setTheme(clientAppTO.getTheme());

        if (clientAppTO.getAuthPolicy() == null) {
            clientApp.setAuthPolicy(null);
        } else {
            Policy policy = policyDAO.find(clientAppTO.getAuthPolicy());
            if (policy instanceof AuthPolicy) {
                clientApp.setAuthPolicy((AuthPolicy) policy);
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
            Policy policy = policyDAO.find(clientAppTO.getAccessPolicy());
            if (policy instanceof AccessPolicy) {
                clientApp.setAccessPolicy((AccessPolicy) policy);
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
            Policy policy = policyDAO.find(clientAppTO.getAttrReleasePolicy());
            if (policy instanceof AttrReleasePolicy) {
                clientApp.setAttrReleasePolicy((AttrReleasePolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AttrReleasePolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        clientApp.setProperties(clientAppTO.getProperties());
    }
}
