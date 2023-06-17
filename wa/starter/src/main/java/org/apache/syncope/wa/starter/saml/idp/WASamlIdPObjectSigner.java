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
package org.apache.syncope.wa.starter.saml.idp;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import net.shibboleth.shared.resolver.CriteriaSet;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.saml.SamlIdPUtils;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataCredentialResolver;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataLocator;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPSamlRegisteredServiceCriterion;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.web.idp.profile.builders.enc.DefaultSamlIdPObjectSigner;
import org.apereo.cas.util.DigestUtils;
import org.apereo.cas.util.RegexUtils;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.criteria.entity.impl.EvaluableEntityRoleEntityDescriptorCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.credential.AbstractCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.xmlsec.SignatureSigningConfiguration;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.criterion.SignatureSigningConfigurationCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WASamlIdPObjectSigner extends DefaultSamlIdPObjectSigner {

    protected static final Logger LOG = LoggerFactory.getLogger(WASamlIdPObjectSigner.class);

    protected static boolean doesCredentialFingerprintMatch(
            final AbstractCredential credential,
            final SamlRegisteredService samlRegisteredService) {

        var fingerprint = samlRegisteredService.getSigningCredentialFingerprint();
        if (StringUtils.isNotBlank(fingerprint)) {
            var digest = DigestUtils.digest("SHA-1", Objects.requireNonNull(credential.getPublicKey()).getEncoded());
            var pattern = RegexUtils.createPattern(fingerprint, Pattern.CASE_INSENSITIVE);
            LOG.debug("Matching credential fingerprint [{}] against filter [{}] for service [{}]",
                    digest, fingerprint, samlRegisteredService.getName());
            return pattern.matcher(digest).find();
        }
        return true;
    }

    protected final MetadataResolver samlIdPMetadataResolver;

    protected final CasConfigurationProperties casProperties;

    protected final SamlIdPMetadataLocator samlIdPMetadataLocator;

    public WASamlIdPObjectSigner(
            final MetadataResolver samlIdPMetadataResolver,
            final CasConfigurationProperties casProperties,
            final SamlIdPMetadataLocator samlIdPMetadataLocator) {

        super(samlIdPMetadataResolver, casProperties, samlIdPMetadataLocator);
        this.samlIdPMetadataResolver = samlIdPMetadataResolver;
        this.casProperties = casProperties;
        this.samlIdPMetadataLocator = samlIdPMetadataLocator;
    }

    @Override
    protected SignatureSigningConfiguration getSignatureSigningConfiguration(final SamlRegisteredService service)
            throws Exception {

        var config = configureSignatureSigningSecurityConfiguration(service);

        var samlIdp = casProperties.getAuthn().getSamlIdp();
        var privateKey = getSigningPrivateKey(service);

        var mdCredentialResolver = new SamlIdPMetadataCredentialResolver();
        var roleDescriptorResolver = SamlIdPUtils.getRoleDescriptorResolver(
                samlIdPMetadataResolver,
                samlIdp.getMetadata().getCore().isRequireValidMetadata());
        mdCredentialResolver.setRoleDescriptorResolver(roleDescriptorResolver);
        mdCredentialResolver.setKeyInfoCredentialResolver(
                DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver());
        mdCredentialResolver.initialize();

        var criteriaSet = new CriteriaSet();
        criteriaSet.add(new SignatureSigningConfigurationCriterion(config));
        criteriaSet.add(new UsageCriterion(UsageType.SIGNING));

        var entityIdCriteriaSet = new CriteriaSet(
                new EvaluableEntityRoleEntityDescriptorCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new SamlIdPSamlRegisteredServiceCriterion(service));
        LOG.trace("Resolving entity id from SAML2 IdP metadata for signature signing configuration is [{}]", service.
                getName());
        var entityId = Objects.requireNonNull(samlIdPMetadataResolver.resolveSingle(entityIdCriteriaSet)).getEntityID();
        LOG.trace("Resolved entity id from SAML2 IdP metadata is [{}]", entityId);
        criteriaSet.add(new EntityIdCriterion(entityId));
        criteriaSet.add(new EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
        criteriaSet.add(new SamlIdPSamlRegisteredServiceCriterion(service));

        LOG.trace("Resolved signing credentials based on criteria [{}]", criteriaSet);
        var credentials = Sets.newLinkedHashSet(mdCredentialResolver.resolve(criteriaSet));
        LOG.trace("Resolved [{}] signing credentials", credentials.size());

        var finalCredentials = new ArrayList<Credential>();
        credentials.stream()
                .map(creds -> getResolvedSigningCredential(creds, privateKey, service))
                .filter(Objects::nonNull)
                .filter(creds -> doesCredentialFingerprintMatch(creds, service))
                .forEach(finalCredentials::add);

        if (finalCredentials.isEmpty()) {
            LOG.error("Unable to locate any signing credentials for service [{}]", service.getName());
            throw new IllegalArgumentException("Unable to locate signing credentials");
        }

        config.setSigningCredentials(finalCredentials);
        LOG.trace("Signature signing credentials configured with [{}] credentials", finalCredentials.size());
        return config;
    }
}
