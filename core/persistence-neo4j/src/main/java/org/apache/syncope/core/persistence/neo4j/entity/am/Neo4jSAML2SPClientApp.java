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
package org.apache.syncope.core.persistence.neo4j.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;

@Node(Neo4jSAML2SPClientApp.NODE)
public class Neo4jSAML2SPClientApp extends AbstractClientApp implements SAML2SPClientApp {

    private static final long serialVersionUID = 6422422526695279794L;

    public static final String NODE = "SAML2SPClientApp";

    protected static final TypeReference<Set<String>> STRING_TYPEREF = new TypeReference<Set<String>>() {
    };

    protected static final TypeReference<List<XmlSecAlgorithm>> XMLSECAGO_TYPEREF =
            new TypeReference<List<XmlSecAlgorithm>>() {
    };

    @NotNull
    private String entityId;

    private String metadataLocation;

    private String metadataSignatureLocation;

    private boolean signAssertions;

    private boolean signResponses;

    private boolean encryptionOptional;

    private boolean encryptAssertions;

    private String requiredAuthenticationContextClass;

    private SAML2SPNameId requiredNameIdFormat;

    private Integer skewAllowance;

    private String nameIdQualifier;

    private String assertionAudiences;

    @Transient
    private Set<String> assertionAudiencesSet = new HashSet<>();

    private String serviceProviderNameIdQualifier;

    private String signingSignatureAlgorithms;

    @Transient
    private List<XmlSecAlgorithm> signingSignatureAlgorithmsList = new ArrayList<>();

    private String signingSignatureReferenceDigestMethods;

    @Transient
    private List<XmlSecAlgorithm> signingSignatureReferenceDigestMethodsList = new ArrayList<>();

    private String encryptionDataAlgorithms;

    @Transient
    private List<XmlSecAlgorithm> encryptionDataAlgorithmsList = new ArrayList<>();

    private String encryptionKeyAlgorithms;

    @Transient
    private List<XmlSecAlgorithm> encryptionKeyAlgorithmsList = new ArrayList<>();

    private String signingSignatureBlackListedAlgorithms;

    @Transient
    private List<XmlSecAlgorithm> signingSignatureBlackListedAlgorithmsList = new ArrayList<>();

    private String encryptionBlackListedAlgorithms;

    @Transient
    private List<XmlSecAlgorithm> encryptionBlackListedAlgorithmsList = new ArrayList<>();

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
        return assertionAudiencesSet;
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
        return signingSignatureAlgorithmsList;
    }

    @Override
    public List<XmlSecAlgorithm> getSigningSignatureReferenceDigestMethods() {
        return signingSignatureReferenceDigestMethodsList;
    }

    @Override
    public List<XmlSecAlgorithm> getEncryptionDataAlgorithms() {
        return encryptionDataAlgorithmsList;
    }

    @Override
    public List<XmlSecAlgorithm> getEncryptionKeyAlgorithms() {
        return encryptionKeyAlgorithmsList;
    }

    @Override
    public List<XmlSecAlgorithm> getSigningSignatureBlackListedAlgorithms() {
        return signingSignatureBlackListedAlgorithmsList;
    }

    @Override
    public List<XmlSecAlgorithm> getEncryptionBlackListedAlgorithms() {
        return encryptionBlackListedAlgorithmsList;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getAssertionAudiences().clear();
            getSigningSignatureAlgorithms().clear();
            getSigningSignatureReferenceDigestMethods().clear();
            getEncryptionDataAlgorithms().clear();
            getEncryptionKeyAlgorithms().clear();
            getSigningSignatureBlackListedAlgorithms().clear();
            getEncryptionBlackListedAlgorithms().clear();
        }
        if (assertionAudiences != null) {
            getAssertionAudiences().addAll(
                    POJOHelper.deserialize(assertionAudiences, STRING_TYPEREF));
        }
        if (signingSignatureAlgorithms != null) {
            getSigningSignatureAlgorithms().addAll(
                    POJOHelper.deserialize(signingSignatureAlgorithms, XMLSECAGO_TYPEREF));
        }
        if (signingSignatureReferenceDigestMethods != null) {
            getSigningSignatureReferenceDigestMethods().addAll(
                    POJOHelper.deserialize(signingSignatureReferenceDigestMethods, XMLSECAGO_TYPEREF));
        }
        if (encryptionDataAlgorithms != null) {
            getEncryptionDataAlgorithms().addAll(
                    POJOHelper.deserialize(encryptionDataAlgorithms, XMLSECAGO_TYPEREF));
        }
        if (encryptionKeyAlgorithms != null) {
            getEncryptionKeyAlgorithms().addAll(
                    POJOHelper.deserialize(encryptionKeyAlgorithms, XMLSECAGO_TYPEREF));
        }
        if (signingSignatureBlackListedAlgorithms != null) {
            getSigningSignatureBlackListedAlgorithms().addAll(
                    POJOHelper.deserialize(signingSignatureBlackListedAlgorithms, XMLSECAGO_TYPEREF));
        }
        if (encryptionBlackListedAlgorithms != null) {
            getEncryptionBlackListedAlgorithms().addAll(
                    POJOHelper.deserialize(encryptionBlackListedAlgorithms, XMLSECAGO_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        assertionAudiences = POJOHelper.serialize(getAssertionAudiences());
        signingSignatureAlgorithms = POJOHelper.serialize(getSigningSignatureAlgorithms());
        signingSignatureReferenceDigestMethods = POJOHelper.serialize(getSigningSignatureReferenceDigestMethods());
        encryptionDataAlgorithms = POJOHelper.serialize(getEncryptionDataAlgorithms());
        encryptionKeyAlgorithms = POJOHelper.serialize(getEncryptionKeyAlgorithms());
        signingSignatureBlackListedAlgorithms = POJOHelper.serialize(getSigningSignatureBlackListedAlgorithms());
        encryptionBlackListedAlgorithms = POJOHelper.serialize(getEncryptionBlackListedAlgorithms());
    }
}
