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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;
import org.apache.syncope.common.lib.XmlSecAlgorithms;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = JPASAML2SP.TABLE)
public class JPASAML2SP extends AbstractClientApp implements SAML2SP {

    public static final String TABLE = "SAML2SP";

    private static final long serialVersionUID = 6422422526695279794L;

    @Column(unique = true, nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String metadataLocation;

    private String metadataSignatureLocation;

    private boolean signAssertions;

    private boolean signResponses;

    private boolean encryptionOptional;

    private boolean encryptAssertions;

    @Column(name = "reqAuthnContextClass")
    private String requiredAuthenticationContextClass;

    private SAML2SPNameId requiredNameIdFormat;

    private Integer skewAllowance;

    private String nameIdQualifier;

    private String assertionAudiences;

    @Column(name = "spNameIdQualifier")
    private String serviceProviderNameIdQualifier;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    @CollectionTable(name = "SAML2SP_SigningSignatureAlgs",
        joinColumns =
        @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithms> signingSignatureAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    @CollectionTable(name = "SAML2SP_SigningSignatureRefDigestAlgs",
        joinColumns =
        @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithms> signingSignatureReferenceDigestMethods = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    @CollectionTable(name = "SAML2SP_EncryptionDataAlgs",
        joinColumns =
        @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithms> encryptionDataAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    @CollectionTable(name = "SAML2SP_EncryptionKeyAlgs",
        joinColumns =
        @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithms> encryptionKeyAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    @CollectionTable(name = "SAML2SP_BlacklistedSigningAlgs",
        joinColumns =
        @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithms> signingSignatureBlackListedAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    @CollectionTable(name = "SAML2SP_BlacklistedEncryptionAlgs",
        joinColumns =
        @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithms> encryptionBlackListedAlgorithms = new ArrayList<>();

    @Override
    public String getEntityId() {
        return entityId;
    }

    @Override
    public void setEntityId(final String entityId) {
        this.entityId = entityId;
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
    public void setEncryptAssertions(final boolean encryptAssertions) {
        this.encryptAssertions = encryptAssertions;
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
    public String getAssertionAudiences() {
        return assertionAudiences;
    }

    @Override
    public void setAssertionAudiences(final String assertionAudiences) {
        this.assertionAudiences = assertionAudiences;
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
    public List<XmlSecAlgorithms> getSigningSignatureAlgorithms() {
        return signingSignatureAlgorithms;
    }

    @Override
    public void setSigningSignatureAlgorithms(final List<XmlSecAlgorithms> signingSignatureAlgorithms) {
        this.signingSignatureAlgorithms = signingSignatureAlgorithms;
    }

    @Override
    public List<XmlSecAlgorithms> getSigningSignatureReferenceDigestMethods() {
        return signingSignatureReferenceDigestMethods;
    }

    @Override
    public void setSigningSignatureReferenceDigestMethods(final List<XmlSecAlgorithms> algorithms) {
        this.signingSignatureReferenceDigestMethods = algorithms;
    }

    @Override
    public List<XmlSecAlgorithms> getEncryptionDataAlgorithms() {
        return encryptionDataAlgorithms;
    }

    @Override
    public void setEncryptionDataAlgorithms(final List<XmlSecAlgorithms> algorithms) {
        this.encryptionDataAlgorithms = algorithms;
    }

    @Override
    public List<XmlSecAlgorithms> getEncryptionKeyAlgorithms() {
        return encryptionKeyAlgorithms;
    }

    @Override
    public void setEncryptionKeyAlgorithms(final List<XmlSecAlgorithms> algorithms) {
        this.encryptionKeyAlgorithms = algorithms;
    }

    @Override
    public List<XmlSecAlgorithms> getSigningSignatureBlackListedAlgorithms() {
        return signingSignatureBlackListedAlgorithms;
    }

    @Override
    public void setSigningSignatureBlackListedAlgorithms(final List<XmlSecAlgorithms> algorithms) {
        this.signingSignatureBlackListedAlgorithms = algorithms;
    }

    @Override
    public List<XmlSecAlgorithms> getEncryptionBlackListedAlgorithms() {
        return encryptionBlackListedAlgorithms;
    }

    @Override
    public void setEncryptionBlackListedAlgorithms(final List<XmlSecAlgorithms> algorithms) {
        this.encryptionBlackListedAlgorithms = algorithms;
    }
}
