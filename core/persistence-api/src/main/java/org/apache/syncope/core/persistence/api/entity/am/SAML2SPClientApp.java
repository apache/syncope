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
package org.apache.syncope.core.persistence.api.entity.am;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.MetadataCriteriaDirection;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.SigningCredentialType;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;

public interface SAML2SPClientApp extends ClientApp {

    String getEntityId();

    void setEntityId(String id);

    Optional<String> getIdp();

    void setIdp(String idp);

    String getMetadataLocation();

    void setMetadataLocation(String location);

    void setMetadataSignatureLocation(String location);

    String getMetadataSignatureLocation();

    void setSignAssertions(boolean location);

    boolean isSignAssertions();

    void setSignResponses(boolean location);

    boolean isSignResponses();

    void setEncryptionOptional(boolean location);

    boolean isEncryptionOptional();

    void setEncryptAssertions(boolean location);

    boolean isEncryptAssertions();

    void setMetadataCriteriaPattern(String metadataCriteriaPattern);

    String getMetadataCriteriaPattern();

    void setSubjectLocality(String subjectLocality);

    String getSubjectLocality();

    void setMetadataCriteriaDirection(MetadataCriteriaDirection metadataCriteriaDirection);

    MetadataCriteriaDirection getMetadataCriteriaDirection();

    void setSigningCredentialType(SigningCredentialType signingCredentialType);

    SigningCredentialType getSigningCredentialType();

    void setLogoutResponseBinding(SAML2BindingType logoutResponseBinding);

    SAML2BindingType getLogoutResponseBinding();

    void setRequireSignedRoot(boolean requireSignedRoot);

    boolean isRequireSignedRoot();

    void setLogoutResponseEnabled(boolean logoutResponseEnabled);

    boolean isLogoutResponseEnabled();

    boolean isEncryptAttributes();

    void setEncryptAttributes(boolean encryptAttributes);

    boolean isSkipGeneratingAssertionNameId();

    void setSkipGeneratingAssertionNameId(boolean skipGeneratingAssertionNameId);

    boolean isSkipGeneratingSubjectConfirmationInResponseTo();

    void setSkipGeneratingSubjectConfirmationInResponseTo(boolean skipGeneratingSubjectConfirmationInResponseTo);

    boolean isSkipGeneratingResponseInResponseTo();

    void setSkipGeneratingResponseInResponseTo(boolean skipGeneratingResponseInResponseTo);

    boolean isSkipGeneratingSubjectConfirmationNotOnOrAfter();

    void setSkipGeneratingSubjectConfirmationNotOnOrAfter(boolean skipGeneratingSubjectConfirmationNotOnOrAfter);

    boolean isSkipGeneratingSubjectConfirmationRecipient();

    void setSkipGeneratingSubjectConfirmationRecipient(boolean skipGeneratingSubjectConfirmationRecipient);

    boolean isSkipGeneratingSubjectConfirmationAddress();

    void setSkipGeneratingSubjectConfirmationAddress(boolean skipGeneratingSubjectConfirmationAddress);

    boolean isSkipGeneratingSubjectConfirmationNotBefore();

    void setSkipGeneratingSubjectConfirmationNotBefore(boolean skipGeneratingSubjectConfirmationNotBefore);

    boolean isSkipGeneratingSubjectConfirmationNameId();

    void setSkipGeneratingSubjectConfirmationNameId(boolean skipGeneratingSubjectConfirmationNameId);

    boolean isSkipGeneratingNameIdQualifiers();

    void setSkipGeneratingNameIdQualifiers(boolean skipGeneratingNameIdQualifiers);

    boolean isSkipGeneratingTransientNameId();

    void setSkipGeneratingTransientNameId(boolean skipGeneratingTransientNameId);

    boolean isSkipValidatingAuthnRequest();

    void setSkipValidatingAuthnRequest(boolean skipValidatingAuthnRequest);

    boolean isSkipGeneratingServiceProviderNameIdQualifier();

    void setSkipGeneratingServiceProviderNameIdQualifier(boolean skipGeneratingServiceProviderNameIdQualifier);

    boolean isSkipGeneratingAuthenticatingAuthority();

    void setSkipGeneratingAuthenticatingAuthority(boolean skipGeneratingAuthenticatingAuthority);

    boolean isSkipGeneratingNameIdQualifier();

    void setSkipGeneratingNameIdQualifier(boolean skipGeneratingNameIdQualifier);

    boolean isSkipGeneratingSessionNotOnOrAfter();

    void setSkipGeneratingSessionNotOnOrAfter(boolean skipGeneratingSessionNotOnOrAfter);

    boolean isValidateMetadataCertificates();

    void setValidateMetadataCertificates(boolean validateMetadataCertificates);

    void setRequiredAuthenticationContextClass(String location);

    String getRequiredAuthenticationContextClass();

    void setRequiredNameIdFormat(SAML2SPNameId location);

    SAML2SPNameId getRequiredNameIdFormat();

    void setSkewAllowance(String skewAllowance);

    String getSkewAllowance();

    void setValidityUntil(String validityUntil);

    String getValidityUntil();

    void setNameIdQualifier(String location);

    String getNameIdQualifier();

    Set<String> getAssertionAudiences();

    void setServiceProviderNameIdQualifier(String location);

    String getServiceProviderNameIdQualifier();

    List<XmlSecAlgorithm> getSigningSignatureAlgorithms();

    List<XmlSecAlgorithm> getSigningSignatureReferenceDigestMethods();

    List<XmlSecAlgorithm> getEncryptionDataAlgorithms();

    List<XmlSecAlgorithm> getEncryptionKeyAlgorithms();

    List<XmlSecAlgorithm> getSigningSignatureBlackListedAlgorithms();

    List<XmlSecAlgorithm> getEncryptionBlackListedAlgorithms();
}
