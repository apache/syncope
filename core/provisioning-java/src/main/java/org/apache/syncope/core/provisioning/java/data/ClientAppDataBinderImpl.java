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
import org.apache.syncope.common.lib.to.client.CASSPTO;
import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.CASSP;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientAppDataBinderImpl implements ClientAppDataBinder {

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClientApp> T create(final ClientAppTO clientAppTO) {
        if (clientAppTO instanceof SAML2SPTO) {
            return (T) doCreate((SAML2SPTO) clientAppTO);
        } else if (clientAppTO instanceof OIDCRPTO) {
            return (T) doCreate((OIDCRPTO) clientAppTO);
        } else if (clientAppTO instanceof CASSPTO) {
            return (T) doCreate((CASSPTO) clientAppTO);
        } else {
            throw new IllegalArgumentException("Unsupported client app: " + clientAppTO.getClass().getName());
        }
    }

    @Override
    public <T extends ClientApp> void update(final T clientApp, final ClientAppTO clientAppTO) {
        if (clientAppTO instanceof SAML2SPTO) {
            doUpdate((SAML2SP) clientApp, (SAML2SPTO) clientAppTO);
        } else if (clientAppTO instanceof OIDCRPTO) {
            doUpdate((OIDCRP) clientApp, (OIDCRPTO) clientAppTO);
        } else if (clientAppTO instanceof CASSPTO) {
            doUpdate((CASSP) clientApp, (CASSPTO) clientAppTO);
        } else {
            throw new IllegalArgumentException("Unsupported client app: " + clientAppTO.getClass().getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClientAppTO> T getClientAppTO(final ClientApp clientApp) {
        if (clientApp instanceof SAML2SP) {
            return (T) getSAMLClientAppTO((SAML2SP) clientApp);
        } 
        if (clientApp instanceof OIDCRP) {
            return (T) getOIDCClientAppTO((OIDCRP) clientApp);
        } 
        if (clientApp instanceof CASSP) {
            return (T) getCASClientAppTO((CASSP) clientApp);
        }
        throw new IllegalArgumentException("Unsupported client app: " + clientApp.getClass().getName());
    }

    private SAML2SP doCreate(final SAML2SPTO clientAppTO) {
        SAML2SP saml2sp = entityFactory.newEntity(SAML2SP.class);
        update(saml2sp, clientAppTO);
        return saml2sp;
    }

    private CASSP doCreate(final CASSPTO clientAppTO) {
        CASSP saml2sp = entityFactory.newEntity(CASSP.class);
        update(saml2sp, clientAppTO);
        return saml2sp;
    }

    private void doUpdate(final SAML2SP clientApp, final SAML2SPTO clientAppTO) {
        doUpdateCommon(clientApp, clientAppTO);

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
        clientApp.setAssertionAudiences(clientAppTO.getAssertionAudiences());
        clientApp.setServiceProviderNameIdQualifier(clientAppTO.getServiceProviderNameIdQualifier());

        clientApp.setSigningSignatureAlgorithms(clientAppTO.getSigningSignatureAlgorithms());
        clientApp.setSigningSignatureReferenceDigestMethods(clientAppTO.getSigningSignatureReferenceDigestMethods());
        clientApp.setEncryptionKeyAlgorithms(clientAppTO.getEncryptionKeyAlgorithms());
        clientApp.setEncryptionDataAlgorithms(clientAppTO.getEncryptionDataAlgorithms());

        clientApp.setSigningSignatureBlackListedAlgorithms(clientAppTO.getSigningSignatureBlackListedAlgorithms());
        clientApp.setEncryptionBlackListedAlgorithms(clientAppTO.getEncryptionBlackListedAlgorithms());
    }

    private static SAML2SPTO getSAMLClientAppTO(final SAML2SP clientApp) {
        SAML2SPTO clientAppTO = new SAML2SPTO();
        updateCommonClientAppTO(clientApp, clientAppTO);

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
        clientAppTO.setAssertionAudiences(clientApp.getAssertionAudiences());
        clientAppTO.setServiceProviderNameIdQualifier(clientApp.getServiceProviderNameIdQualifier());

        clientAppTO.setSigningSignatureAlgorithms(clientApp.getSigningSignatureAlgorithms());
        clientAppTO.setSigningSignatureReferenceDigestMethods(clientApp.getSigningSignatureReferenceDigestMethods());
        clientAppTO.setEncryptionKeyAlgorithms(clientApp.getEncryptionKeyAlgorithms());
        clientAppTO.setEncryptionDataAlgorithms(clientApp.getEncryptionDataAlgorithms());

        clientAppTO.setSigningSignatureBlackListedAlgorithms(clientApp.getSigningSignatureBlackListedAlgorithms());
        clientAppTO.setEncryptionBlackListedAlgorithms(clientApp.getEncryptionBlackListedAlgorithms());

        return clientAppTO;
    }

    private OIDCRP doCreate(final OIDCRPTO clientAppTO) {
        OIDCRP oidcrp = entityFactory.newEntity(OIDCRP.class);
        update(oidcrp, clientAppTO);
        return oidcrp;
    }

    private void doUpdate(final OIDCRP clientApp, final OIDCRPTO clientAppTO) {
        doUpdateCommon(clientApp, clientAppTO);

        clientApp.setClientSecret(clientAppTO.getClientSecret());
        clientApp.setClientId(clientAppTO.getClientId());
        clientApp.setSignIdToken(clientAppTO.isSignIdToken());
        clientApp.setSubjectType(clientAppTO.getSubjectType());
        clientApp.getRedirectUris().addAll(clientAppTO.getRedirectUris());
        clientApp.getSupportedGrantTypes().addAll(clientAppTO.getSupportedGrantTypes());
        clientApp.getSupportedResponseTypes().addAll(clientAppTO.getSupportedResponseTypes());

        clientApp.setLogoutUri(clientAppTO.getLogoutUri());
    }

    private static OIDCRPTO getOIDCClientAppTO(final OIDCRP clientApp) {
        OIDCRPTO clientAppTO = new OIDCRPTO();
        updateCommonClientAppTO(clientApp, clientAppTO);

        clientAppTO.setClientId(clientApp.getClientId());
        clientAppTO.setClientSecret(clientApp.getClientSecret());
        clientAppTO.setSignIdToken(clientApp.isSignIdToken());
        clientAppTO.setSubjectType(clientApp.getSubjectType());
        clientAppTO.getRedirectUris().addAll(clientApp.getRedirectUris());
        clientAppTO.getSupportedGrantTypes().addAll(clientApp.getSupportedGrantTypes());
        clientAppTO.getSupportedResponseTypes().addAll(clientApp.getSupportedResponseTypes());
        clientAppTO.setLogoutUri(clientApp.getLogoutUri());

        return clientAppTO;
    }

    private void doUpdate(final CASSP clientApp, final CASSPTO clientAppTO) {
        doUpdateCommon(clientApp, clientAppTO);

        clientApp.setServiceId(clientAppTO.getServiceId());
    }

    private static CASSPTO getCASClientAppTO(final CASSP clientApp) {
        CASSPTO clientAppTO = new CASSPTO();
        updateCommonClientAppTO(clientApp, clientAppTO);
        clientAppTO.setServiceId(clientApp.getServiceId());
        return clientAppTO;
    }

    private static void updateCommonClientAppTO(final ClientApp clientApp, final ClientAppTO clientAppTO) {
        clientAppTO.setName(clientApp.getName());
        clientAppTO.setKey(clientApp.getKey());
        clientAppTO.setDescription(clientApp.getDescription());
        clientAppTO.setClientAppId(clientApp.getClientAppId());
        clientAppTO.setTheme(clientApp.getTheme());

        clientAppTO.setAuthPolicy(clientApp.getAuthPolicy() == null
            ? null : clientApp.getAuthPolicy().getKey());
        clientAppTO.setAccessPolicy(clientApp.getAccessPolicy() == null
            ? null : clientApp.getAccessPolicy().getKey());
        clientAppTO.setAttrReleasePolicy(clientApp.getAttrReleasePolicy() == null
            ? null : clientApp.getAttrReleasePolicy().getKey());
    }

    private void doUpdateCommon(final ClientApp clientApp, final ClientAppTO clientAppTO) {
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
    }
}
