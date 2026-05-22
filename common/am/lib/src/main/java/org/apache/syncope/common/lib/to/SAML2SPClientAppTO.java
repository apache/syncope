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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.MetadataCriteriaDirection;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.SigningCredentialType;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;

@Schema(allOf = { ClientAppTO.class })
public class SAML2SPClientAppTO extends ClientAppTO {

    private static final long serialVersionUID = -6370888503924521351L;

    private String entityId;

    private String idp;

    private String metadataLocation;

    private String metadataSignatureLocation;

    private String metadataCriteriaPattern;
    
    private String subjectLocality;

    private MetadataCriteriaDirection metadataCriteriaDirection;

    private SigningCredentialType signingCredentialType;

    private SAML2BindingType logoutResponseBinding; 

    private boolean signAssertions;

    private boolean signResponses;

    private boolean encryptionOptional;

    private boolean encryptAssertions;

    private boolean requireSignedRoot;

    private boolean logoutResponseEnabled;

    private boolean encryptAttributes;

    private boolean skipGeneratingAssertionNameId;

    private boolean skipGeneratingSubjectConfirmationInResponseTo;

    private boolean skipGeneratingResponseInResponseTo;

    private boolean skipGeneratingSubjectConfirmationNotOnOrAfter;

    private boolean skipGeneratingSubjectConfirmationRecipient;

    private boolean skipGeneratingSubjectConfirmationAddress;

    private boolean skipGeneratingSubjectConfirmationNotBefore;

    private boolean skipGeneratingSubjectConfirmationNameId;

    private boolean skipGeneratingNameIdQualifiers;

    private boolean skipGeneratingTransientNameId;

    private boolean skipValidatingAuthnRequest;

    private boolean skipGeneratingServiceProviderNameIdQualifier;

    private boolean skipGeneratingAuthenticatingAuthority;

    private boolean skipGeneratingNameIdQualifier;

    private boolean skipGeneratingSessionNotOnOrAfter;

    private boolean validateMetadataCertificates;

    private String requiredAuthenticationContextClass;

    private SAML2SPNameId requiredNameIdFormat;

    private Integer skewAllowance;

    private String nameIdQualifier;

    private final List<String> assertionAudiences = new ArrayList<>();

    private String serviceProviderNameIdQualifier;

    private final List<XmlSecAlgorithm> signingSignatureAlgorithms = new ArrayList<>();

    private final List<XmlSecAlgorithm> signingSignatureReferenceDigestMethods = new ArrayList<>();

    private final List<XmlSecAlgorithm> encryptionDataAlgorithms = new ArrayList<>();

    private final List<XmlSecAlgorithm> encryptionKeyAlgorithms = new ArrayList<>();

    private final List<XmlSecAlgorithm> signingSignatureBlackListedAlgorithms = new ArrayList<>();

    private final List<XmlSecAlgorithm> encryptionBlackListedAlgorithms = new ArrayList<>();

    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.client.SAML2SPTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    public String getIdp() {
        return idp;
    }

    public void setIdp(final String idp) {
        this.idp = idp;
    }

    public String getMetadataLocation() {
        return metadataLocation;
    }

    public void setMetadataLocation(final String metadataLocation) {
        this.metadataLocation = metadataLocation;
    }

    public String getMetadataSignatureLocation() {
        return metadataSignatureLocation;
    }

    public void setMetadataSignatureLocation(final String metadataSignatureLocation) {
        this.metadataSignatureLocation = metadataSignatureLocation;
    }

    public String getSubjectLocality() {
        return subjectLocality;
    }

    public void setSubjectLocality(final String subjectLocality) {
        this.subjectLocality = subjectLocality;
    }

    public MetadataCriteriaDirection getMetadataCriteriaDirection() {
        return metadataCriteriaDirection;
    }

    public void setMetadataCriteriaDirection(final MetadataCriteriaDirection metadataCriteriaDirection) {
        this.metadataCriteriaDirection = metadataCriteriaDirection;
    }

    public String getMetadataCriteriaPattern() {
        return metadataCriteriaPattern;
    }

    public void setMetadataCriteriaPattern(final String metadataCriteriaPattern) {
        this.metadataCriteriaPattern = metadataCriteriaPattern;
    }

    public SigningCredentialType getSigningCredentialType() {
        return signingCredentialType;
    }

    public void setSigningCredentialType(final SigningCredentialType signingCredentialType) {
        this.signingCredentialType = signingCredentialType;
    }

    public SAML2BindingType getLogoutResponseBinding() {
        return logoutResponseBinding;
    }

    public void setLogoutResponseBinding(final SAML2BindingType logoutResponseBinding) {
        this.logoutResponseBinding = logoutResponseBinding;
    }

    public boolean isSignAssertions() {
        return signAssertions;
    }

    public void setSignAssertions(final boolean signAssertions) {
        this.signAssertions = signAssertions;
    }

    public boolean isSignResponses() {
        return signResponses;
    }

    public void setSignResponses(final boolean signResponses) {
        this.signResponses = signResponses;
    }

    public boolean isEncryptionOptional() {
        return encryptionOptional;
    }

    public void setEncryptionOptional(final boolean encryptionOptional) {
        this.encryptionOptional = encryptionOptional;
    }

    public boolean isEncryptAssertions() {
        return encryptAssertions;
    }

    public void setEncryptAssertions(final boolean encryptAssertions) {
        this.encryptAssertions = encryptAssertions;
    }

    public boolean isRequireSignedRoot() {
        return requireSignedRoot;
    }

    public void setRequireSignedRoot(final boolean requireSignedRoot) {
        this.requireSignedRoot = requireSignedRoot;
    }

    public boolean isLogoutResponseEnabled() {
        return logoutResponseEnabled;
    }

    public void setLogoutResponseEnabled(final boolean logoutResponseEnabled) {
        this.logoutResponseEnabled = logoutResponseEnabled;
    }

    public boolean isEncryptAttributes() {
        return encryptAttributes;
    }

    public void setEncryptAttributes(final boolean encryptAttributes) {
        this.encryptAttributes = encryptAttributes;
    }

    public boolean isSkipGeneratingAssertionNameId() {
        return skipGeneratingAssertionNameId;
    }

    public void setSkipGeneratingAssertionNameId(final boolean skipGeneratingAssertionNameId) {
        this.skipGeneratingAssertionNameId = skipGeneratingAssertionNameId;
    }

    public boolean isSkipGeneratingSubjectConfirmationInResponseTo() {
        return skipGeneratingSubjectConfirmationInResponseTo;
    }

    public void setSkipGeneratingSubjectConfirmationInResponseTo(
        final boolean skipGeneratingSubjectConfirmationInResponseTo) {
        this.skipGeneratingSubjectConfirmationInResponseTo = skipGeneratingSubjectConfirmationInResponseTo;
    }

    public boolean isSkipGeneratingResponseInResponseTo() {
        return skipGeneratingResponseInResponseTo;
    }

    public void setSkipGeneratingResponseInResponseTo(final boolean skipGeneratingResponseInResponseTo) {
        this.skipGeneratingResponseInResponseTo = skipGeneratingResponseInResponseTo;
    }

    public boolean isSkipGeneratingSubjectConfirmationNotOnOrAfter() {
        return skipGeneratingSubjectConfirmationNotOnOrAfter;
    }

    public void setSkipGeneratingSubjectConfirmationNotOnOrAfter(
        final boolean skipGeneratingSubjectConfirmationNotOnOrAfter) {
        this.skipGeneratingSubjectConfirmationNotOnOrAfter = skipGeneratingSubjectConfirmationNotOnOrAfter;
    }

    public boolean isSkipGeneratingSubjectConfirmationRecipient() {
        return skipGeneratingSubjectConfirmationRecipient;
    }

    public void setSkipGeneratingSubjectConfirmationRecipient(
        final boolean skipGeneratingSubjectConfirmationRecipient) {
        this.skipGeneratingSubjectConfirmationRecipient = skipGeneratingSubjectConfirmationRecipient;
    }

    public boolean isSkipGeneratingSubjectConfirmationAddress() {
        return skipGeneratingSubjectConfirmationAddress;
    }

    public void setSkipGeneratingSubjectConfirmationAddress(final boolean skipGeneratingSubjectConfirmationAddress) {
        this.skipGeneratingSubjectConfirmationAddress = skipGeneratingSubjectConfirmationAddress;
    }

    public boolean isSkipGeneratingSubjectConfirmationNotBefore() {
        return skipGeneratingSubjectConfirmationNotBefore;
    }

    public void setSkipGeneratingSubjectConfirmationNotBefore(
        final boolean skipGeneratingSubjectConfirmationNotBefore) {
        this.skipGeneratingSubjectConfirmationNotBefore = skipGeneratingSubjectConfirmationNotBefore;
    }

    public boolean isSkipGeneratingSubjectConfirmationNameId() {
        return skipGeneratingSubjectConfirmationNameId;
    }

    public void setSkipGeneratingSubjectConfirmationNameId(final boolean skipGeneratingSubjectConfirmationNameId) {
        this.skipGeneratingSubjectConfirmationNameId = skipGeneratingSubjectConfirmationNameId;
    }

    public boolean isSkipGeneratingNameIdQualifiers() {
        return skipGeneratingNameIdQualifiers;
    }

    public void setSkipGeneratingNameIdQualifiers(final boolean skipGeneratingNameIdQualifiers) {
        this.skipGeneratingNameIdQualifiers = skipGeneratingNameIdQualifiers;
    }

    public boolean isSkipGeneratingTransientNameId() {
        return skipGeneratingTransientNameId;
    }

    public void setSkipGeneratingTransientNameId(final boolean skipGeneratingTransientNameId) {
        this.skipGeneratingTransientNameId = skipGeneratingTransientNameId;
    }

    public boolean isSkipValidatingAuthnRequest() {
        return skipValidatingAuthnRequest;
    }

    public void setSkipValidatingAuthnRequest(final boolean skipValidatingAuthnRequest) {
        this.skipValidatingAuthnRequest = skipValidatingAuthnRequest;
    }

    public boolean isSkipGeneratingServiceProviderNameIdQualifier() {
        return skipGeneratingServiceProviderNameIdQualifier;
    }

    public void setSkipGeneratingServiceProviderNameIdQualifier(
        final boolean skipGeneratingServiceProviderNameIdQualifier) {
        this.skipGeneratingServiceProviderNameIdQualifier = skipGeneratingServiceProviderNameIdQualifier;
    }

    public boolean isSkipGeneratingAuthenticatingAuthority() {
        return skipGeneratingAuthenticatingAuthority;
    }

    public void setSkipGeneratingAuthenticatingAuthority(final boolean skipGeneratingAuthenticatingAuthority) {
        this.skipGeneratingAuthenticatingAuthority = skipGeneratingAuthenticatingAuthority;
    }

    public boolean isSkipGeneratingNameIdQualifier() {
        return skipGeneratingNameIdQualifier;
    }

    public void setSkipGeneratingNameIdQualifier(final boolean skipGeneratingNameIdQualifier) {
        this.skipGeneratingNameIdQualifier = skipGeneratingNameIdQualifier;
    }

    public boolean isSkipGeneratingSessionNotOnOrAfter() {
        return skipGeneratingSessionNotOnOrAfter;
    }

    public void setSkipGeneratingSessionNotOnOrAfter(final boolean skipGeneratingSessionNotOnOrAfter) {
        this.skipGeneratingSessionNotOnOrAfter = skipGeneratingSessionNotOnOrAfter;
    }

    public boolean isValidateMetadataCertificates() {
        return validateMetadataCertificates;
    }

    public void setValidateMetadataCertificates(final boolean validateMetadataCertificates) {
        this.validateMetadataCertificates = validateMetadataCertificates;
    }

    public String getRequiredAuthenticationContextClass() {
        return requiredAuthenticationContextClass;
    }

    public void setRequiredAuthenticationContextClass(final String requiredAuthenticationContextClass) {
        this.requiredAuthenticationContextClass = requiredAuthenticationContextClass;
    }

    public SAML2SPNameId getRequiredNameIdFormat() {
        return requiredNameIdFormat;
    }

    public void setRequiredNameIdFormat(final SAML2SPNameId requiredNameIdFormat) {
        this.requiredNameIdFormat = requiredNameIdFormat;
    }

    public Integer getSkewAllowance() {
        return skewAllowance;
    }

    public void setSkewAllowance(final Integer skewAllowance) {
        this.skewAllowance = skewAllowance;
    }

    public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    public void setNameIdQualifier(final String nameIdQualifier) {
        this.nameIdQualifier = nameIdQualifier;
    }

    public List<String> getAssertionAudiences() {
        return assertionAudiences;
    }

    public String getServiceProviderNameIdQualifier() {
        return serviceProviderNameIdQualifier;
    }

    public void setServiceProviderNameIdQualifier(final String serviceProviderNameIdQualifier) {
        this.serviceProviderNameIdQualifier = serviceProviderNameIdQualifier;
    }

    public List<XmlSecAlgorithm> getSigningSignatureAlgorithms() {
        return signingSignatureAlgorithms;
    }

    public List<XmlSecAlgorithm> getSigningSignatureReferenceDigestMethods() {
        return signingSignatureReferenceDigestMethods;
    }

    public List<XmlSecAlgorithm> getEncryptionDataAlgorithms() {
        return encryptionDataAlgorithms;
    }

    public List<XmlSecAlgorithm> getEncryptionKeyAlgorithms() {
        return encryptionKeyAlgorithms;
    }

    public List<XmlSecAlgorithm> getSigningSignatureBlackListedAlgorithms() {
        return signingSignatureBlackListedAlgorithms;
    }

    public List<XmlSecAlgorithm> getEncryptionBlackListedAlgorithms() {
        return encryptionBlackListedAlgorithms;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        SAML2SPClientAppTO rhs = (SAML2SPClientAppTO) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.entityId, rhs.entityId)
                .append(this.metadataLocation, rhs.metadataLocation)
                .append(this.metadataSignatureLocation, rhs.metadataSignatureLocation)
                .append(this.signAssertions, rhs.signAssertions)
                .append(this.signResponses, rhs.signResponses)
                .append(this.metadataCriteriaPattern, rhs.metadataCriteriaPattern)
                .append(this.subjectLocality, rhs.subjectLocality)
                .append(this.metadataCriteriaDirection, rhs.metadataCriteriaDirection)
                .append(this.logoutResponseBinding, rhs.logoutResponseBinding)
                .append(this.requireSignedRoot, rhs.requireSignedRoot)
                .append(this.logoutResponseEnabled, rhs.logoutResponseEnabled)
                .append(this.encryptionOptional, rhs.encryptionOptional)
                .append(this.signingCredentialType, rhs.signingCredentialType)
                .append(this.encryptAttributes, rhs.encryptAttributes)
                .append(this.skipGeneratingAssertionNameId, rhs.skipGeneratingAssertionNameId)
                .append(this.skipGeneratingSubjectConfirmationInResponseTo,
                    rhs.skipGeneratingSubjectConfirmationInResponseTo)
                .append(this.skipGeneratingResponseInResponseTo, rhs.skipGeneratingResponseInResponseTo)
                .append(this.skipGeneratingSubjectConfirmationNotOnOrAfter,
                    rhs.skipGeneratingSubjectConfirmationNotOnOrAfter)
                .append(this.skipGeneratingSubjectConfirmationRecipient, rhs.skipGeneratingSubjectConfirmationRecipient)
                .append(this.skipGeneratingSubjectConfirmationAddress, rhs.skipGeneratingSubjectConfirmationAddress)
                .append(this.skipGeneratingSubjectConfirmationNotBefore, rhs.skipGeneratingSubjectConfirmationNotBefore)
                .append(this.skipGeneratingSubjectConfirmationNameId, rhs.skipGeneratingSubjectConfirmationNameId)
                .append(this.skipGeneratingNameIdQualifiers, rhs.skipGeneratingNameIdQualifiers)
                .append(this.skipGeneratingTransientNameId, rhs.skipGeneratingTransientNameId)
                .append(this.skipValidatingAuthnRequest, rhs.skipValidatingAuthnRequest)
                .append(this.skipGeneratingServiceProviderNameIdQualifier,
                    rhs.skipGeneratingServiceProviderNameIdQualifier)
                .append(this.skipGeneratingAuthenticatingAuthority, rhs.skipGeneratingAuthenticatingAuthority)
                .append(this.skipGeneratingNameIdQualifier, rhs.skipGeneratingNameIdQualifier)
                .append(this.skipGeneratingSessionNotOnOrAfter, rhs.skipGeneratingSessionNotOnOrAfter)
                .append(this.validateMetadataCertificates, rhs.validateMetadataCertificates)
                .append(this.encryptAssertions, rhs.encryptAssertions)
                .append(this.requiredAuthenticationContextClass, rhs.requiredAuthenticationContextClass)
                .append(this.requiredNameIdFormat, rhs.requiredNameIdFormat)
                .append(this.skewAllowance, rhs.skewAllowance)
                .append(this.nameIdQualifier, rhs.nameIdQualifier)
                .append(this.assertionAudiences, rhs.assertionAudiences)
                .append(this.serviceProviderNameIdQualifier, rhs.serviceProviderNameIdQualifier)
                .append(this.signingSignatureAlgorithms, rhs.signingSignatureAlgorithms)
                .append(this.signingSignatureReferenceDigestMethods, rhs.signingSignatureReferenceDigestMethods)
                .append(this.encryptionDataAlgorithms, rhs.encryptionDataAlgorithms)
                .append(this.encryptionKeyAlgorithms, rhs.encryptionKeyAlgorithms)
                .append(this.encryptionBlackListedAlgorithms, rhs.encryptionBlackListedAlgorithms)
                .append(this.signingSignatureBlackListedAlgorithms, rhs.signingSignatureBlackListedAlgorithms)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(entityId)
                .append(metadataLocation)
                .append(metadataSignatureLocation)
                .append(signAssertions)
                .append(signResponses)
                .append(encryptionOptional)
                .append(metadataCriteriaPattern)
                .append(subjectLocality)
                .append(metadataCriteriaDirection)
                .append(logoutResponseBinding)
                .append(requireSignedRoot)
                .append(logoutResponseEnabled)
                .append(signingCredentialType)
                .append(encryptAttributes)
                .append(skipGeneratingAssertionNameId)
                .append(skipGeneratingSubjectConfirmationInResponseTo)
                .append(skipGeneratingResponseInResponseTo)
                .append(skipGeneratingSubjectConfirmationNotOnOrAfter)
                .append(skipGeneratingSubjectConfirmationRecipient)
                .append(skipGeneratingSubjectConfirmationAddress)
                .append(skipGeneratingSubjectConfirmationNotBefore)
                .append(skipGeneratingSubjectConfirmationNameId)
                .append(skipGeneratingNameIdQualifiers)
                .append(skipGeneratingTransientNameId)
                .append(skipValidatingAuthnRequest)
                .append(skipGeneratingServiceProviderNameIdQualifier)
                .append(skipGeneratingAuthenticatingAuthority)
                .append(skipGeneratingNameIdQualifier)
                .append(skipGeneratingSessionNotOnOrAfter)
                .append(validateMetadataCertificates)
                .append(encryptAssertions)
                .append(requiredAuthenticationContextClass)
                .append(requiredNameIdFormat)
                .append(skewAllowance)
                .append(nameIdQualifier)
                .append(assertionAudiences)
                .append(serviceProviderNameIdQualifier)
                .append(signingSignatureAlgorithms)
                .append(signingSignatureReferenceDigestMethods)
                .append(encryptionDataAlgorithms)
                .append(encryptionKeyAlgorithms)
                .append(signingSignatureBlackListedAlgorithms)
                .append(encryptionBlackListedAlgorithms)
                .toHashCode();
    }
}
