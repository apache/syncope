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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPClientApp;

@Entity
@Table(name = JPASAML2SPClientApp.TABLE)
public class JPASAML2SPClientApp extends AbstractClientApp implements SAML2SPClientApp {

    public static final String TABLE = "SAML2SPClientApp";

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

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "assertionAudience")
    @CollectionTable(name = "SAML2SPClientApp_AssertionAudiences",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private Set<String> assertionAudiences = new HashSet<>();

    @Column(name = "spNameIdQualifier")
    private String serviceProviderNameIdQualifier;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "signingSignatureAlgorithm")
    @CollectionTable(name = "SAML2SPClientApp_SigningSignatureAlgs",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithm> signingSignatureAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "signingSignatureReferenceDigestMethod")
    @CollectionTable(name = "SAML2SPClientApp_SigningSignatureRefDigestAlgs",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithm> signingSignatureReferenceDigestMethods = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "encryptionDataAlgorithm")
    @CollectionTable(name = "SAML2SPClientApp_EncryptionDataAlgs",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithm> encryptionDataAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "encryptionKeyAlgorithm")
    @CollectionTable(name = "SAML2SPClientApp_EncryptionKeyAlgs",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithm> encryptionKeyAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "signingSignatureBlackListedAlgorithm")
    @CollectionTable(name = "SAML2SPClientApp_BlacklistedSigningAlgs",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithm> signingSignatureBlackListedAlgorithms = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "encryptionBlackListedAlgorithm")
    @CollectionTable(name = "SAML2SPClientApp_BlacklistedEncryptionAlgs",
            joinColumns =
            @JoinColumn(name = "client_app_id", referencedColumnName = "id"))
    private List<XmlSecAlgorithm> encryptionBlackListedAlgorithms = new ArrayList<>();

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
