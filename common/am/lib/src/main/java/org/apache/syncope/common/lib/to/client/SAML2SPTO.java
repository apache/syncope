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
package org.apache.syncope.common.lib.to.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;

@XmlRootElement(name = "saml2SP")
@XmlType
@Schema(allOf = { ClientAppTO.class })
public class SAML2SPTO extends ClientAppTO {

    private static final long serialVersionUID = -6370888503924521351L;

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

    private String serviceProviderNameIdQualifier;

    @XmlTransient
    @JsonProperty("@class")
    @Schema(name = "@class", required = true,
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

    public String getAssertionAudiences() {
        return assertionAudiences;
    }

    public void setAssertionAudiences(final String assertionAudiences) {
        this.assertionAudiences = assertionAudiences;
    }

    public String getServiceProviderNameIdQualifier() {
        return serviceProviderNameIdQualifier;
    }

    public void setServiceProviderNameIdQualifier(final String serviceProviderNameIdQualifier) {
        this.serviceProviderNameIdQualifier = serviceProviderNameIdQualifier;
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
        SAML2SPTO rhs = (SAML2SPTO) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.entityId, rhs.entityId)
                .append(this.metadataLocation, rhs.metadataLocation)
                .append(this.metadataSignatureLocation, rhs.metadataSignatureLocation)
                .append(this.signAssertions, rhs.signAssertions)
                .append(this.signResponses, rhs.signResponses)
                .append(this.encryptionOptional, rhs.encryptionOptional)
                .append(this.encryptAssertions, rhs.encryptAssertions)
                .append(this.requiredAuthenticationContextClass, rhs.requiredAuthenticationContextClass)
                .append(this.requiredNameIdFormat, rhs.requiredNameIdFormat)
                .append(this.skewAllowance, rhs.skewAllowance)
                .append(this.nameIdQualifier, rhs.nameIdQualifier)
                .append(this.assertionAudiences, rhs.assertionAudiences)
                .append(this.serviceProviderNameIdQualifier, rhs.serviceProviderNameIdQualifier)
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
                .append(encryptAssertions)
                .append(requiredAuthenticationContextClass)
                .append(requiredNameIdFormat)
                .append(skewAllowance)
                .append(nameIdQualifier)
                .append(assertionAudiences)
                .append(serviceProviderNameIdQualifier)
                .toHashCode();
    }
}
