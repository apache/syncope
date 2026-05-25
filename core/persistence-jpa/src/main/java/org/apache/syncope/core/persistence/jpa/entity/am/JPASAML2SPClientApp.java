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
package org.apache.syncope.core.persistence.jpa.entity.am;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.MetadataCriteriaDirection;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.SigningCredentialType;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.jpa.converters.StringSetConverter;
import org.apache.syncope.core.persistence.jpa.converters.XmlSecAlgorithmListConverter;

@Entity
@Table(name = JPASAML2SPClientApp.TABLE)
public class JPASAML2SPClientApp extends AbstractClientApp implements SAML2SPClientApp {

    private static final long serialVersionUID = 6422422526695279794L;

    public static final String TABLE = "SAML2SPClientApp";

    @Column(unique = true, nullable = false)
    private String entityId;

    private String idp;

    @Column(nullable = false)
    private String metadataLocation;

    private String metadataSignatureLocation;

    private boolean signAssertions;

    private boolean signResponses;

    private boolean encryptionOptional;

    private boolean encryptAssertions;

    private boolean requireSignedRoot;

    private boolean encryptAttributes;

    private boolean skipGeneratingAssertionNameId;
    
    private boolean logoutResponseEnable;

    @Column(name = "skipGeneSubjectConfInRespTo")
    private boolean skipGeneratingSubjectConfirmationInResponseTo;

    @Column(name = "skipGeneRespInResponseTo")
    private boolean skipGeneratingResponseInResponseTo;

    @Column(name = "skipGeneSubConfNotOnOrAfter")
    private boolean skipGeneratingSubjectConfirmationNotOnOrAfter;

    @Column(name = "skipGeneSubConfRecipient")
    private boolean skipGeneratingSubjectConfirmationRecipient;

    @Column(name = "skipGeneSubConfAddress")
    private boolean skipGeneratingSubjectConfirmationAddress;

    @Column(name = "skipGeneSubConfNotBefore")
    private boolean skipGeneratingSubjectConfirmationNotBefore;

    @Column(name = "skipGeneSubConfNameId")
    private boolean skipGeneratingSubjectConfirmationNameId;

    private boolean skipGeneratingNameIdQualifiers;

    private boolean skipGeneratingTransientNameId;

    private boolean skipValidatingAuthnRequest;

    @Column(name = "skipGeneSerProvNameIdQualifier")
    private boolean skipGeneratingServiceProviderNameIdQualifier;

    @Column(name = "skipGeneAuthAuthority")
    private boolean skipGeneratingAuthenticatingAuthority;

    private boolean skipGeneratingNameIdQualifier;

    @Column(name = "skipGeneSessionNotOnOrAfter")
    private boolean skipGeneratingSessionNotOnOrAfter;

    private boolean validateMetadataCertificates;

    @Column(name = "reqAuthnContextClass")
    private String requiredAuthenticationContextClass;

    private SAML2SPNameId requiredNameIdFormat;

    private Integer skewAllowance;

    private String nameIdQualifier;

    @Convert(converter = StringSetConverter.class)
    @Lob
    private Set<String> assertionAudiences = new HashSet<>();

    @Column(name = "spNameIdQualifier")
    private String serviceProviderNameIdQualifier;

    @Convert(converter = XmlSecAlgorithmListConverter.class)
    @Column(name = "sigAlgs")
    @Lob
    private List<XmlSecAlgorithm> signingSignatureAlgorithms = new ArrayList<>();

    @Convert(converter = XmlSecAlgorithmListConverter.class)
    @Column(name = "sigRefDigestMethod")
    @Lob
    private List<XmlSecAlgorithm> signingSignatureReferenceDigestMethods = new ArrayList<>();

    @Convert(converter = XmlSecAlgorithmListConverter.class)
    @Column(name = "encDataAlg")
    @Lob
    private List<XmlSecAlgorithm> encryptionDataAlgorithms = new ArrayList<>();

    @Convert(converter = XmlSecAlgorithmListConverter.class)
    @Column(name = "encKeyAlg")
    @Lob
    private List<XmlSecAlgorithm> encryptionKeyAlgorithms = new ArrayList<>();

    @Convert(converter = XmlSecAlgorithmListConverter.class)
    @Column(name = "sigBlAlg")
    @Lob
    private List<XmlSecAlgorithm> signingSignatureBlackListedAlgorithms = new ArrayList<>();

    @Convert(converter = XmlSecAlgorithmListConverter.class)
    @Column(name = "encBlAlg")
    @Lob
    private List<XmlSecAlgorithm> encryptionBlackListedAlgorithms = new ArrayList<>();

    private String metadataCriteriaPattern;

    private String subjectLocality;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private MetadataCriteriaDirection metadataCriteriaDirection;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private SigningCredentialType signingCredentialType;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private SAML2BindingType logoutResponseBinding;

    @Override
    public String getEntityId() {
        return entityId;
    }

    @Override
    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    @Override
    public Optional<String> getIdp() {
        return Optional.ofNullable(idp);
    }

    @Override
    public void setIdp(final String idp) {
        this.idp = idp;
    }

    @Override
    public String getMetadataLocation() {
        return metadataLocation;
    }

    @Override
    public void setMetadataLocation(final String metadataLocation) {
        this.metadataLocation = metadataLocation;
    }

    @Override
    public String getMetadataSignatureLocation() {
        return metadataSignatureLocation;
    }

    @Override
    public void setMetadataSignatureLocation(final String metadataSignatureLocation) {
        this.metadataSignatureLocation = metadataSignatureLocation;
    }

    @Override
    public boolean isSignAssertions() {
        return signAssertions;
    }

    @Override
    public void setSignAssertions(final boolean signAssertions) {
        this.signAssertions = signAssertions;
    }

    @Override
    public boolean isSignResponses() {
        return signResponses;
    }

    @Override
    public void setSignResponses(final boolean signResponses) {
        this.signResponses = signResponses;
    }

    @Override
    public boolean isEncryptionOptional() {
        return encryptionOptional;
    }

    @Override
    public void setEncryptionOptional(final boolean encryptionOptional) {
        this.encryptionOptional = encryptionOptional;
    }

    @Override
    public boolean isEncryptAssertions() {
        return encryptAssertions;
    }

    @Override
    public void setMetadataCriteriaPattern(final String metadataCriteriaPattern) {
        this.metadataCriteriaPattern = metadataCriteriaPattern;
    }

    @Override
    public String getMetadataCriteriaPattern() {
        return metadataCriteriaPattern;
    }

    @Override
    public void setSubjectLocality(final String subjectLocality) {
        this.subjectLocality =  subjectLocality;
    }

    @Override
    public String getSubjectLocality() {
        return subjectLocality;
    }

    @Override
    public void setMetadataCriteriaDirection(final MetadataCriteriaDirection metadataCriteriaDirection) {
        this.metadataCriteriaDirection = metadataCriteriaDirection;
    }

    @Override
    public MetadataCriteriaDirection getMetadataCriteriaDirection() {
        return metadataCriteriaDirection;
    }

    @Override
    public void setSigningCredentialType(final SigningCredentialType signingCredentialType) {
        this.signingCredentialType = signingCredentialType;
    }

    @Override
    public SigningCredentialType getSigningCredentialType() {
        return signingCredentialType;
    }

    @Override
    public void setLogoutResponseBinding(final SAML2BindingType logoutResponseBinding) {
        this.logoutResponseBinding = logoutResponseBinding;
    }

    @Override
    public SAML2BindingType getLogoutResponseBinding() {
        return logoutResponseBinding;
    }

    @Override
    public void setEncryptAssertions(final boolean encryptAssertions) {
        this.encryptAssertions = encryptAssertions;
    }

    @Override
    public void setRequireSignedRoot(final boolean requireSignedRoot) {
        this.requireSignedRoot = requireSignedRoot;
    }

    @Override
    public boolean isRequireSignedRoot() {
        return requireSignedRoot;
    }

    @Override
    public void setLogoutResponseEnabled(final boolean logoutResponseEnabled) {
        this.logoutResponseEnable = logoutResponseEnabled;
    }

    @Override
    public boolean isLogoutResponseEnabled() {
        return logoutResponseEnable;
    }

    @Override
    public boolean isEncryptAttributes() {
        return encryptAttributes;
    }

    @Override
    public void setEncryptAttributes(final boolean encryptAttributes) {
        this.encryptAttributes = encryptAttributes;
    }

    @Override
    public boolean isSkipGeneratingAssertionNameId() {
        return skipGeneratingAssertionNameId;
    }

    @Override
    public void setSkipGeneratingAssertionNameId(final boolean skipGeneratingAssertionNameId) {
        this.skipGeneratingAssertionNameId = skipGeneratingAssertionNameId;
    }

    @Override
    public boolean isSkipGeneratingSubjectConfirmationInResponseTo() {
        return skipGeneratingSubjectConfirmationInResponseTo;
    }

    @Override
    public void setSkipGeneratingSubjectConfirmationInResponseTo(
        final boolean skipGeneratingSubjectConfirmationInResponseTo) {
        this.skipGeneratingSubjectConfirmationInResponseTo = skipGeneratingSubjectConfirmationInResponseTo;
    }

    @Override
    public boolean isSkipGeneratingResponseInResponseTo() {
        return skipGeneratingResponseInResponseTo;
    }

    @Override
    public void setSkipGeneratingResponseInResponseTo(final boolean skipGeneratingResponseInResponseTo) {
        this.skipGeneratingResponseInResponseTo = skipGeneratingResponseInResponseTo;
    }

    @Override
    public boolean isSkipGeneratingSubjectConfirmationNotOnOrAfter() {
        return skipGeneratingSubjectConfirmationNotOnOrAfter;
    }

    @Override
    public void setSkipGeneratingSubjectConfirmationNotOnOrAfter(
        final boolean skipGeneratingSubjectConfirmationNotOnOrAfter) {
        this.skipGeneratingSubjectConfirmationNotOnOrAfter = skipGeneratingSubjectConfirmationNotOnOrAfter;
    }

    @Override
    public boolean isSkipGeneratingSubjectConfirmationRecipient() {
        return skipGeneratingSubjectConfirmationRecipient;
    }

    @Override
    public void setSkipGeneratingSubjectConfirmationRecipient(
        final boolean skipGeneratingSubjectConfirmationRecipient) {
        this.skipGeneratingSubjectConfirmationRecipient = skipGeneratingSubjectConfirmationRecipient;
    }

    @Override
    public boolean isSkipGeneratingSubjectConfirmationAddress() {
        return skipGeneratingSubjectConfirmationAddress;
    }

    @Override
    public void setSkipGeneratingSubjectConfirmationAddress(final boolean skipGeneratingSubjectConfirmationAddress) {
        this.skipGeneratingSubjectConfirmationAddress = skipGeneratingSubjectConfirmationAddress;
    }

    @Override
    public boolean isSkipGeneratingSubjectConfirmationNotBefore() {
        return skipGeneratingSubjectConfirmationNotBefore;
    }

    @Override
    public void setSkipGeneratingSubjectConfirmationNotBefore(
        final boolean skipGeneratingSubjectConfirmationNotBefore) {
        this.skipGeneratingSubjectConfirmationNotBefore = skipGeneratingSubjectConfirmationNotBefore;
    }

    @Override
    public boolean isSkipGeneratingSubjectConfirmationNameId() {
        return skipGeneratingSubjectConfirmationNameId;
    }

    @Override
    public void setSkipGeneratingSubjectConfirmationNameId(final boolean skipGeneratingSubjectConfirmationNameId) {
        this.skipGeneratingSubjectConfirmationNameId = skipGeneratingSubjectConfirmationNameId;
    }

    @Override
    public boolean isSkipGeneratingNameIdQualifiers() {
        return skipGeneratingNameIdQualifiers;
    }

    @Override
    public void setSkipGeneratingNameIdQualifiers(final boolean skipGeneratingNameIdQualifiers) {
        this.skipGeneratingNameIdQualifiers = skipGeneratingNameIdQualifiers;
    }

    @Override
    public boolean isSkipGeneratingTransientNameId() {
        return skipGeneratingTransientNameId;
    }

    @Override
    public void setSkipGeneratingTransientNameId(final boolean skipGeneratingTransientNameId) {
        this.skipGeneratingTransientNameId = skipGeneratingTransientNameId;
    }

    @Override
    public boolean isSkipValidatingAuthnRequest() {
        return skipValidatingAuthnRequest;
    }

    @Override
    public void setSkipValidatingAuthnRequest(final boolean skipValidatingAuthnRequest) {
        this.skipValidatingAuthnRequest = skipValidatingAuthnRequest;
    }

    @Override
    public boolean isSkipGeneratingServiceProviderNameIdQualifier() {
        return skipGeneratingServiceProviderNameIdQualifier;
    }

    @Override
    public void setSkipGeneratingServiceProviderNameIdQualifier(
        final boolean skipGeneratingServiceProviderNameIdQualifier) {
        this.skipGeneratingServiceProviderNameIdQualifier = skipGeneratingServiceProviderNameIdQualifier;
    }

    @Override
    public boolean isSkipGeneratingAuthenticatingAuthority() {
        return skipGeneratingAuthenticatingAuthority;
    }

    @Override
    public void setSkipGeneratingAuthenticatingAuthority(final boolean skipGeneratingAuthenticatingAuthority) {
        this.skipGeneratingAuthenticatingAuthority = skipGeneratingAuthenticatingAuthority;
    }

    @Override
    public boolean isSkipGeneratingNameIdQualifier() {
        return skipGeneratingNameIdQualifier;
    }

    @Override
    public void setSkipGeneratingNameIdQualifier(final boolean skipGeneratingNameIdQualifier) {
        this.skipGeneratingNameIdQualifier = skipGeneratingNameIdQualifier;
    }

    @Override
    public boolean isSkipGeneratingSessionNotOnOrAfter() {
        return skipGeneratingSessionNotOnOrAfter;
    }

    @Override
    public void setSkipGeneratingSessionNotOnOrAfter(final boolean skipGeneratingSessionNotOnOrAfter) {
        this.skipGeneratingSessionNotOnOrAfter = skipGeneratingSessionNotOnOrAfter;
    }

    @Override
    public boolean isValidateMetadataCertificates() {
        return validateMetadataCertificates;
    }

    @Override
    public void setValidateMetadataCertificates(final boolean validateMetadataCertificates) {
        this.validateMetadataCertificates = validateMetadataCertificates;
    }

    @Override
    public String getRequiredAuthenticationContextClass() {
        return requiredAuthenticationContextClass;
    }

    @Override
    public void setRequiredAuthenticationContextClass(final String requiredAuthenticationContextClass) {
        this.requiredAuthenticationContextClass = requiredAuthenticationContextClass;
    }

    @Override
    public SAML2SPNameId getRequiredNameIdFormat() {
        return requiredNameIdFormat;
    }

    @Override
    public void setRequiredNameIdFormat(final SAML2SPNameId requiredNameIdFormat) {
        this.requiredNameIdFormat = requiredNameIdFormat;
    }

    @Override
    public Integer getSkewAllowance() {
        return skewAllowance;
    }

    @Override
    public void setSkewAllowance(final Integer skewAllowance) {
        this.skewAllowance = skewAllowance;
    }

    @Override
    public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    @Override
    public void setNameIdQualifier(final String nameIdQualifier) {
        this.nameIdQualifier = nameIdQualifier;
    }

    @Override
    public Set<String> getAssertionAudiences() {
        return assertionAudiences;
    }

    @Override
    public String getServiceProviderNameIdQualifier() {
        return serviceProviderNameIdQualifier;
    }

    @Override
    public void setServiceProviderNameIdQualifier(final String serviceProviderNameIdQualifier) {
        this.serviceProviderNameIdQualifier = serviceProviderNameIdQualifier;
    }

    @Override
    public List<XmlSecAlgorithm> getSigningSignatureAlgorithms() {
        return signingSignatureAlgorithms;
    }

    @Override
    public List<XmlSecAlgorithm> getSigningSignatureReferenceDigestMethods() {
        return signingSignatureReferenceDigestMethods;
    }

    @Override
    public List<XmlSecAlgorithm> getEncryptionDataAlgorithms() {
        return encryptionDataAlgorithms;
    }

    @Override
    public List<XmlSecAlgorithm> getEncryptionKeyAlgorithms() {
        return encryptionKeyAlgorithms;
    }

    @Override
    public List<XmlSecAlgorithm> getSigningSignatureBlackListedAlgorithms() {
        return signingSignatureBlackListedAlgorithms;
    }

    @Override
    public List<XmlSecAlgorithm> getEncryptionBlackListedAlgorithms() {
        return encryptionBlackListedAlgorithms;
    }
}
