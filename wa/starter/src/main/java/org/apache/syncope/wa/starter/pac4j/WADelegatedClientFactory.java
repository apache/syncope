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
package org.apache.syncope.wa.starter.pac4j;

import com.github.benmanes.caffeine.cache.Cache;
import java.time.Period;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.CasSSLContext;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.support.pac4j.authentication.clients.DefaultDelegatedClientFactory;
import org.apereo.cas.support.pac4j.authentication.clients.DelegatedClientFactoryCustomizer;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.spring.SpringExpressionLanguageValueResolver;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.profile.converter.AttributeConverter;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.DefaultSAML2MetadataSigner;
import org.pac4j.saml.metadata.SAML2ServiceProviderRequestedAttribute;
import org.pac4j.saml.store.EmptyStoreFactory;
import org.pac4j.saml.store.HttpSessionStoreFactory;
import org.pac4j.saml.store.SAMLMessageStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

public class WADelegatedClientFactory extends DefaultDelegatedClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WADelegatedClientFactory.class);

    private final CasSSLContext casSSLContext;

    private final ObjectProvider<SAMLMessageStoreFactory> samlMessageStoreFactory;

    @SuppressWarnings("rawtypes")
    public WADelegatedClientFactory(
            final CasConfigurationProperties casProperties,
            final Collection<DelegatedClientFactoryCustomizer> customizers,
            final CasSSLContext casSSLContext,
            final ObjectProvider<SAMLMessageStoreFactory> samlMessageStoreFactory,
            final Cache<String, Collection<IndirectClient>> clientsCache) {

        super(casProperties, customizers, casSSLContext, samlMessageStoreFactory, clientsCache);
        this.casSSLContext = casSSLContext;
        this.samlMessageStoreFactory = samlMessageStoreFactory;
    }

    @Override
    protected Collection<IndirectClient> buildSaml2IdentityProviders(final CasConfigurationProperties casProperties) {
        var pac4jProperties = casProperties.getAuthn().getPac4j();
        return pac4jProperties
                .getSaml()
                .stream()
                .filter(saml -> saml.isEnabled()
                && StringUtils.isNotBlank(saml.getKeystorePath())
                && StringUtils.isNotBlank(saml.getMetadata().getIdentityProviderMetadataPath())
                && StringUtils.isNotBlank(saml.getServiceProviderEntityId()))
                .map(saml -> {
                    var keystorePath = SpringExpressionLanguageValueResolver.getInstance().
                            resolve(saml.getKeystorePath());
                    var identityProviderMetadataPath = SpringExpressionLanguageValueResolver.getInstance()
                            .resolve(saml.getMetadata().getIdentityProviderMetadataPath());

                    var cfg = new SAML2Configuration(keystorePath, saml.getKeystorePassword(),
                            saml.getPrivateKeyPassword(), identityProviderMetadataPath);
                    cfg.setForceKeystoreGeneration(saml.isForceKeystoreGeneration());

                    FunctionUtils.doIf(saml.getCertificateExpirationDays() > 0,
                            __ -> cfg.setCertificateExpirationPeriod(
                                    Period.ofDays(saml.getCertificateExpirationDays()))).accept(saml);
                    FunctionUtils.doIfNotNull(saml.getResponseBindingType(), cfg::setResponseBindingType);
                    FunctionUtils.doIfNotNull(saml.getCertificateSignatureAlg(), cfg::setCertificateSignatureAlg);

                    cfg.setPartialLogoutTreatedAsSuccess(saml.isPartialLogoutAsSuccess());
                    cfg.setResponseDestinationAttributeMandatory(saml.isResponseDestinationMandatory());
                    cfg.setSupportedProtocols(saml.getSupportedProtocols());

                    FunctionUtils.doIfNotBlank(saml.getRequestInitiatorUrl(), __ -> cfg.setRequestInitiatorUrl(saml.
                            getRequestInitiatorUrl()));
                    FunctionUtils.doIfNotBlank(saml.getSingleLogoutServiceUrl(), __ -> cfg.setSingleSignOutServiceUrl(
                            saml.getSingleLogoutServiceUrl()));
                    FunctionUtils.doIfNotBlank(saml.getLogoutResponseBindingType(), __ -> cfg.
                            setSpLogoutResponseBindingType(saml.getLogoutResponseBindingType()));

                    cfg.setCertificateNameToAppend(
                            StringUtils.defaultIfBlank(saml.getCertificateNameToAppend(), saml.getClientName()));
                    cfg.setMaximumAuthenticationLifetime(
                            Beans.newDuration(saml.getMaximumAuthenticationLifetime()).toSeconds());
                    var serviceProviderEntityId = SpringExpressionLanguageValueResolver.getInstance().
                            resolve(saml.getServiceProviderEntityId());
                    cfg.setServiceProviderEntityId(serviceProviderEntityId);

                    FunctionUtils.doIfNotNull(saml.getMetadata().getServiceProvider().getFileSystem().getLocation(),
                            location -> {
                                var resource = ResourceUtils.getRawResourceFrom(location);
                                cfg.setServiceProviderMetadataResource(resource);
                            });

                    cfg.setAuthnRequestBindingType(saml.getDestinationBinding());
                    cfg.setSpLogoutRequestBindingType(saml.getLogoutRequestBinding());
                    cfg.setForceAuth(saml.isForceAuth());
                    cfg.setPassive(saml.isPassive());
                    cfg.setSignMetadata(saml.isSignServiceProviderMetadata());
                    cfg.setMetadataSigner(new DefaultSAML2MetadataSigner(cfg));
                    cfg.setAuthnRequestSigned(saml.isSignAuthnRequest());
                    cfg.setSpLogoutRequestSigned(saml.isSignServiceProviderLogoutRequest());
                    cfg.setAcceptedSkew(Beans.newDuration(saml.getAcceptedSkew()).toSeconds());
                    cfg.setSslSocketFactory(casSSLContext.getSslContext().getSocketFactory());
                    cfg.setHostnameVerifier(casSSLContext.getHostnameVerifier());

                    FunctionUtils.doIfNotBlank(saml.getPrincipalIdAttribute(), __ -> cfg.setAttributeAsId(saml.
                            getPrincipalIdAttribute()));
                    FunctionUtils.doIfNotBlank(saml.getNameIdAttribute(), __ -> cfg.setNameIdAttribute(saml.
                            getNameIdAttribute()));

                    cfg.setWantsAssertionsSigned(saml.isWantsAssertionsSigned());
                    cfg.setWantsResponsesSigned(saml.isWantsResponsesSigned());
                    cfg.setAllSignatureValidationDisabled(saml.isAllSignatureValidationDisabled());
                    cfg.setUseNameQualifier(saml.isUseNameQualifier());
                    cfg.setAttributeConsumingServiceIndex(saml.getAttributeConsumingServiceIndex());

                    Optional.ofNullable(samlMessageStoreFactory.getIfAvailable())
                            .ifPresentOrElse(cfg::setSamlMessageStoreFactory, () -> {
                                FunctionUtils.doIf("EMPTY".equalsIgnoreCase(saml.getMessageStoreFactory()),
                                        ig -> cfg.setSamlMessageStoreFactory(new EmptyStoreFactory())).accept(saml);
                                FunctionUtils.doIf("SESSION".equalsIgnoreCase(saml.getMessageStoreFactory()),
                                        ig -> cfg.setSamlMessageStoreFactory(new HttpSessionStoreFactory())).
                                        accept(saml);
                                if (saml.getMessageStoreFactory().contains(".")) {
                                    FunctionUtils.doAndHandle(__ -> {
                                        var clazz = ClassUtils.getClass(
                                                getClass().getClassLoader(),
                                                saml.getMessageStoreFactory());
                                        var factory = (SAMLMessageStoreFactory) clazz.getDeclaredConstructor().
                                                newInstance();
                                        cfg.setSamlMessageStoreFactory(factory);
                                    });
                                }
                            });

                    FunctionUtils.doIf(saml.getAssertionConsumerServiceIndex() >= 0,
                            __ -> cfg.setAssertionConsumerServiceIndex(saml.getAssertionConsumerServiceIndex())).
                            accept(saml);

                    if (!saml.getAuthnContextClassRef().isEmpty()) {
                        cfg.setComparisonType(saml.getAuthnContextComparisonType().toUpperCase(Locale.ENGLISH));
                        cfg.setAuthnContextClassRefs(saml.getAuthnContextClassRef());
                    }

                    FunctionUtils.doIfNotBlank(saml.getNameIdPolicyFormat(), __ -> cfg.setNameIdPolicyFormat(saml.
                            getNameIdPolicyFormat()));

                    if (!saml.getRequestedAttributes().isEmpty()) {
                        saml.getRequestedAttributes().stream()
                                .map(attribute -> new SAML2ServiceProviderRequestedAttribute(attribute.getName(),
                                attribute.getFriendlyName(),
                                attribute.getNameFormat(), attribute.isRequired()))
                                .forEach(attribute -> cfg.getRequestedServiceProviderAttributes().add(attribute));
                    }

                    if (!saml.getBlockedSignatureSigningAlgorithms().isEmpty()) {
                        cfg.setBlackListedSignatureSigningAlgorithms(saml.getBlockedSignatureSigningAlgorithms());
                    }
                    if (!saml.getSignatureAlgorithms().isEmpty()) {
                        cfg.setSignatureAlgorithms(saml.getSignatureAlgorithms());
                    }
                    if (!saml.getSignatureReferenceDigestMethods().isEmpty()) {
                        cfg.setSignatureReferenceDigestMethods(saml.getSignatureReferenceDigestMethods());
                    }

                    FunctionUtils.doIfNotBlank(
                            saml.getSignatureCanonicalizationAlgorithm(),
                            __ -> cfg.setSignatureCanonicalizationAlgorithm(
                                    saml.getSignatureCanonicalizationAlgorithm()));
                    cfg.setProviderName(saml.getProviderName());
                    cfg.setNameIdPolicyAllowCreate(saml.getNameIdPolicyAllowCreate().toBoolean());

                    if (StringUtils.isNotBlank(saml.getSaml2AttributeConverter())) {
                        FunctionUtils.doAndHandle(__ -> {
                            var clazz = ClassUtils.getClass(
                                    getClass().getClassLoader(), saml.getSaml2AttributeConverter());
                            var converter = (AttributeConverter) clazz.getDeclaredConstructor().newInstance();
                            cfg.setSamlAttributeConverter(converter);
                        });
                    }

                    var mappedAttributes = saml.getMappedAttributes();
                    if (!mappedAttributes.isEmpty()) {
                        cfg.setMappedAttributes(CollectionUtils.convertDirectedListToMap(mappedAttributes));
                    }

                    var client = new SAML2Client(cfg);
                    configureClient(client, saml, casProperties);

                    LOG.debug("Created delegated client [{}]", client);
                    return client;
                }).collect(Collectors.toList());
    }
}
