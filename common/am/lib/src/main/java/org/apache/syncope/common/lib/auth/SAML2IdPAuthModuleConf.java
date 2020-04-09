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
package org.apache.syncope.common.lib.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "saml2IdPAuthModuleConf")
@XmlType
public class SAML2IdPAuthModuleConf extends AbstractAuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    /**
     * The attribute value that should be used
     * for the authenticated username, upon a successful authentication
     * attempt.
     */
    private String userIdAttribute;

    /**
     * The destination binding to use
     * when creating authentication requests.
     */
    private String destinationBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    /**
     * The password to use when generating the SP keystore.
     */
    private String keystorePassword;

    /**
     * The password to use when generating the private key for the SP keystore.
     */
    private String privateKeyPassword;

    /**
     * Location of the keystore to use and generate the SP keystore.
     */
    private String keystorePath;

    /**
     * The metadata location of the identity provider that is to handle authentications.
     */
    private String identityProviderMetadataPath;

    /**
     * Flag to indicate whether the allow-create flags
     * for nameid policies should be set to true, false or ignored/defined.
     * Accepted values are true, false or undefined.
     */
    private String nameIdPolicyAllowCreate = "undefined";

    /**
     * Once you have an authenticated session on the identity provider, usually it won't prompt you again to enter your
     * credentials and it will automatically generate a new assertion for you. By default, the SAML client
     * will accept assertions based on a previous authentication for one hour.
     * You can adjust this behavior by modifying this setting. The unit of time here is seconds.
     */
    private int maximumAuthenticationLifetime = 3600;

    /**
     * Maximum skew in seconds between SP and IDP clocks.
     * This skew is added onto the {@code NotOnOrAfter} field in seconds
     * for the SAML response validation.
     */
    private int acceptedSkew = 300;

    /**
     * The entity id of the SP that is used in the SP metadata generation process.
     */
    private String serviceProviderEntityId;

    /**
     * Location of the SP metadata to use and generate.
     */
    private String serviceProviderMetadataPath;

    /**
     * Whether authentication requests should be tagged as forced auth.
     */
    private boolean forceAuth;

    /**
     * Whether authentication requests should be tagged as passive.
     */
    private boolean passive;

    /**
     * Requested authentication context class in authn requests.
     */
    private final List<String> authnContextClassRefs = new ArrayList<>(0);

    /**
     * Specifies the comparison rule that should be used to evaluate the specified authentication methods.
     * For example, if exact is specified, the authentication method used must match one of the authentication
     * methods specified by the AuthnContextClassRef elements.
     * AuthContextClassRef element require comparison rule to be used to evaluate the specified
     * authentication methods. If not explicitly specified "exact" rule will be used by default.
     * Other acceptable values are minimum, maximum, better.
     */
    private String authnContextComparisonType = "exact";

    /**
     * The key alias used in the keystore.
     */
    private String keystoreAlias;

    /**
     * NameID policy to request in the authentication requests.
     */
    private String nameIdPolicyFormat;

    /**
     * Whether metadata should be marked to request sign assertions.
     */
    private boolean wantsAssertionsSigned;

    /**
     * AttributeConsumingServiceIndex attribute of AuthnRequest element.
     * The given index points out a specific AttributeConsumingService structure, declared into the
     * Service Provider (SP)'s metadata, to be used to specify all the attributes that the Service Provider
     * is asking to be released within the authentication assertion returned by the Identity Provider (IdP).
     * This attribute won't be sent with the request unless a positive value (including 0) is defined.
     */
    private int attributeConsumingServiceIndex;

    /**
     * Allows the SAML client to select a specific ACS url from the metadata, if defined.
     * A negative value de-activates the selection process and is the default.
     */
    private int assertionConsumerServiceIndex = -1;

    /**
     * Whether name qualifiers should be produced
     * in the final saml response.
     */
    private boolean useNameQualifier = true;

    /**
     * Whether or not SAML SP metadata should be signed when generated.
     */
    private boolean signServiceProviderMetadata;

    /**
     * Whether or not the authnRequest should be signed.
     */
    private boolean signAuthnRequest;

    /**
     * Whether or not the Logout Request sent from the SP should be signed.
     */
    private boolean signServiceProviderLogoutRequest;

    /**
     * Collection of signing signature blacklisted algorithms, if any, to override the global defaults.
     */
    private final List<String> blackListedSignatureSigningAlgorithms = new ArrayList<>(0);

    /**
     * Collection of signing signature algorithms, if any, to override the global defaults.
     */
    private final List<String> signatureAlgorithms = new ArrayList<>(0);

    /**
     * Collection of signing signature reference digest methods, if any, to override the global defaults.
     */
    private final List<String> signatureReferenceDigestMethods = new ArrayList<>(0);

    /**
     * The signing signature canonicalization algorithm, if any, to override the global defaults.
     */
    private String signatureCanonicalizationAlgorithm;

    /**
     * Provider name set for the saml authentication request.
     * Sets the human-readable name of the requester for use by
     * the presenter's user agent or the identity provider.
     */
    private String providerName;

    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    public void setUserIdAttribute(final String userIdAttribute) {
        this.userIdAttribute = userIdAttribute;
    }

    public String getDestinationBinding() {
        return destinationBinding;
    }

    public void setDestinationBinding(final String destinationBinding) {
        this.destinationBinding = destinationBinding;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(final String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public void setPrivateKeyPassword(final String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(final String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getIdentityProviderMetadataPath() {
        return identityProviderMetadataPath;
    }

    public void setIdentityProviderMetadataPath(final String identityProviderMetadataPath) {
        this.identityProviderMetadataPath = identityProviderMetadataPath;
    }

    public int getMaximumAuthenticationLifetime() {
        return maximumAuthenticationLifetime;
    }

    public void setMaximumAuthenticationLifetime(final int maximumAuthenticationLifetime) {
        this.maximumAuthenticationLifetime = maximumAuthenticationLifetime;
    }

    public int getAcceptedSkew() {
        return acceptedSkew;
    }

    public void setAcceptedSkew(final int acceptedSkew) {
        this.acceptedSkew = acceptedSkew;
    }

    public String getServiceProviderEntityId() {
        return serviceProviderEntityId;
    }

    public void setServiceProviderEntityId(final String serviceProviderEntityId) {
        this.serviceProviderEntityId = serviceProviderEntityId;
    }

    public String getServiceProviderMetadataPath() {
        return serviceProviderMetadataPath;
    }

    public void setServiceProviderMetadataPath(final String serviceProviderMetadataPath) {
        this.serviceProviderMetadataPath = serviceProviderMetadataPath;
    }

    public boolean isForceAuth() {
        return forceAuth;
    }

    public void setForceAuth(final boolean forceAuth) {
        this.forceAuth = forceAuth;
    }

    public boolean isPassive() {
        return passive;
    }

    public void setPassive(final boolean passive) {
        this.passive = passive;
    }

    public String getNameIdPolicyAllowCreate() {
        return nameIdPolicyAllowCreate;
    }

    public void setNameIdPolicyAllowCreate(final String nameIdPolicyAllowCreate) {
        this.nameIdPolicyAllowCreate = nameIdPolicyAllowCreate;
    }

    @XmlElementWrapper(name = "authnContextClassRefs")
    @XmlElement(name = "authnContextClassRef")
    @JsonProperty("authnContextClassRefs")
    public List<String> getAuthnContextClassRefs() {
        return authnContextClassRefs;
    }

    public String getAuthnContextComparisonType() {
        return authnContextComparisonType;
    }

    public void setAuthnContextComparisonType(final String authnContextComparisonType) {
        this.authnContextComparisonType = authnContextComparisonType;
    }

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public void setKeystoreAlias(final String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
    }

    public String getNameIdPolicyFormat() {
        return nameIdPolicyFormat;
    }

    public void setNameIdPolicyFormat(final String nameIdPolicyFormat) {
        this.nameIdPolicyFormat = nameIdPolicyFormat;
    }

    public boolean isWantsAssertionsSigned() {
        return wantsAssertionsSigned;
    }

    public void setWantsAssertionsSigned(final boolean wantsAssertionsSigned) {
        this.wantsAssertionsSigned = wantsAssertionsSigned;
    }

    public int getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    public void setAttributeConsumingServiceIndex(final int attributeConsumingServiceIndex) {
        this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
    }

    public int getAssertionConsumerServiceIndex() {
        return assertionConsumerServiceIndex;
    }

    public void setAssertionConsumerServiceIndex(final int assertionConsumerServiceIndex) {
        this.assertionConsumerServiceIndex = assertionConsumerServiceIndex;
    }

    public boolean isUseNameQualifier() {
        return useNameQualifier;
    }

    public void setUseNameQualifier(final boolean useNameQualifier) {
        this.useNameQualifier = useNameQualifier;
    }

    public boolean isSignServiceProviderMetadata() {
        return signServiceProviderMetadata;
    }

    public void setSignServiceProviderMetadata(final boolean signServiceProviderMetadata) {
        this.signServiceProviderMetadata = signServiceProviderMetadata;
    }

    public boolean isSignAuthnRequest() {
        return signAuthnRequest;
    }

    public void setSignAuthnRequest(final boolean signAuthnRequest) {
        this.signAuthnRequest = signAuthnRequest;
    }

    public boolean isSignServiceProviderLogoutRequest() {
        return signServiceProviderLogoutRequest;
    }

    public void setSignServiceProviderLogoutRequest(final boolean signServiceProviderLogoutRequest) {
        this.signServiceProviderLogoutRequest = signServiceProviderLogoutRequest;
    }

    @XmlElementWrapper(name = "blackListedSignatureSigningAlgorithms")
    @XmlElement(name = "blackListedSignatureSigningAlgorithm")
    @JsonProperty("blackListedSignatureSigningAlgorithms")
    public List<String> getBlackListedSignatureSigningAlgorithms() {
        return blackListedSignatureSigningAlgorithms;
    }

    @XmlElementWrapper(name = "signatureAlgorithms")
    @XmlElement(name = "signatureAlgorithm")
    @JsonProperty("signatureAlgorithms")
    public List<String> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    @XmlElementWrapper(name = "signatureReferenceDigestMethods")
    @XmlElement(name = "signatureReferenceDigestMethod")
    @JsonProperty("signatureReferenceDigestMethods")
    public List<String> getSignatureReferenceDigestMethods() {
        return signatureReferenceDigestMethods;
    }

    public String getSignatureCanonicalizationAlgorithm() {
        return signatureCanonicalizationAlgorithm;
    }

    public void setSignatureCanonicalizationAlgorithm(final String signatureCanonicalizationAlgorithm) {
        this.signatureCanonicalizationAlgorithm = signatureCanonicalizationAlgorithm;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(final String providerName) {
        this.providerName = providerName;
    }
}
