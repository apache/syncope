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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;

public class LDAPAuthModuleConf extends AbstractLDAPConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    /**
     * The enum Authentication types.
     */
    public enum AuthenticationType {

        /**
         * Active Directory.
         */
        AD,
        /**
         * Authenticated Search.
         */
        AUTHENTICATED,
        /**
         * Direct Bind.
         */
        DIRECT,
        /**
         * Anonymous Search.
         */
        ANONYMOUS

    }

    public enum DerefAliasesType {
        NEVER,
        SEARCHING,
        FINDING,
        ALWAYS

    }

    /**
     * The authentication type.
     * <ul>
     * <li>{@code AD} - Users authenticate with {@code sAMAccountName}. </li>
     *
     * <li>{@code AUTHENTICATED} - Manager bind/search type of authentication.
     * If {@code} principalAttributePassword}
     * is empty then a user simple bind is done to validate credentials. Otherwise the given
     * attribute is compared with the given {@code principalAttributePassword} using
     * the {@code SHA} encrypted value of it.</li>
     *
     * <li>{@code ANONYMOUS}: Similar semantics as {@code AUTHENTICATED} except no {@code bindDn}
     * and {@code bindCredential} may be specified to initialize the connection.
     * If {@code principalAttributePassword} is empty then a user simple bind is done
     * to validate credentials. Otherwise the given attribute is compared with
     * the given {@code principalAttributePassword} using the {@code SHA} encrypted value of it.</li>
     *
     * <li>DIRECT: Direct Bind - Compute user DN from format string and perform simple bind.
     * This is relevant when no search is required to compute the DN needed for a bind operation.
     * Use cases for this type are:
     * 1) All users are under a single branch in the directory, {@code e.g. ou=Users,dc=example,dc=org.}
     * 2) The username provided on the CAS login form is part of the DN, e.g.
     * {@code uid=%s,ou=Users,dc=example,dc=org}.</li>
     *
     * </ul>
     */
    private AuthenticationType authenticationType = AuthenticationType.AUTHENTICATED;

    /**
     * Specify the dn format accepted by the AD authenticator, etc.
     * Example format might be {@code uid=%s,ou=people,dc=example,dc=org}.
     */
    private String dnFormat;

    /**
     * Whether specific search entry resolvers need to be set
     * on the authenticator, or the default should be used.
     */
    private boolean enhanceWithEntryResolver = true;

    /**
     * Define how aliases are de-referenced.
     * Accepted values are:
     * <ul>
     * <li>{@code NEVER}</li>
     * <li>{@code SEARCHING}: dereference when searching the entries beneath the starting point but not when searching
     * for the starting entry.</li>
     * <li>{@code FINDING}: dereference when searching for the starting entry but not when searching the entries beneath
     * the starting point.</li>
     * <li>{@code ALWAYS}: dereference when searching for the starting entry and when searching the entries beneath the
     * starting point.</li>
     * </ul>
     */
    private DerefAliasesType derefAliases;

    /**
     * If this attribute is set, the value found in the first attribute
     * value will be used in place of the DN.
     */
    private String resolveFromAttribute;

    /**
     * The attribute value that should be used for the authenticated username, upon a successful authentication attempt.
     */
    private String principalAttributeId;

    /**
     * Name of attribute to be used for principal's DN.
     */
    private String principalDnAttributeName = "principalLdapDn";

    /**
     * Sets a flag that determines whether multiple values are allowed for the {@link #principalAttributeId}.
     * This flag only has an effect if {@link #principalAttributeId} is configured. If multiple values are detected
     * when the flag is false, the first value is used and a warning is logged. If multiple values are detected
     * when the flag is true, an exception is raised.
     */
    private boolean allowMultiplePrincipalAttributeValues;

    /**
     * List of additional attributes to retrieve, if any.
     */
    private final List<String> additionalAttributes = new ArrayList<>();

    /**
     * Flag to indicate whether CAS should block authentication
     * if a specific/configured principal id attribute is not found.
     */
    private boolean allowMissingPrincipalAttributeValue = true;

    /**
     * When entry DN should be called as an attribute and stored into the principal.
     */
    private boolean collectDnAttribute;

    /**
     * A number of authentication handlers are allowed to determine whether they can operate on the provided credential
     * and as such lend themselves to be tried and tested during the authentication handler selection phase.
     * The credential criteria may be one of the following options:<ul>
     * <li>A regular expression pattern that is tested against the credential identifier.</li>
     * <li>A fully qualified class name of your own design that implements {@code Predicate}.</li>
     * <li>Path to an external Groovy script that implements the same interface.</li>
     * </ul>
     */
    private String credentialCriteria;
    
    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(final AuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getDnFormat() {
        return dnFormat;
    }

    public void setDnFormat(final String dnFormat) {
        this.dnFormat = dnFormat;
    }

    public boolean isEnhanceWithEntryResolver() {
        return enhanceWithEntryResolver;
    }

    public void setEnhanceWithEntryResolver(final boolean enhanceWithEntryResolver) {
        this.enhanceWithEntryResolver = enhanceWithEntryResolver;
    }

    public DerefAliasesType getDerefAliases() {
        return derefAliases;
    }

    public void setDerefAliases(final DerefAliasesType derefAliases) {
        this.derefAliases = derefAliases;
    }

    public String getResolveFromAttribute() {
        return resolveFromAttribute;
    }

    public void setResolveFromAttribute(final String resolveFromAttribute) {
        this.resolveFromAttribute = resolveFromAttribute;
    }

    public String getPrincipalAttributeId() {
        return principalAttributeId;
    }

    public void setPrincipalAttributeId(final String principalAttributeId) {
        this.principalAttributeId = principalAttributeId;
    }

    public String getPrincipalDnAttributeName() {
        return principalDnAttributeName;
    }

    public void setPrincipalDnAttributeName(final String principalDnAttributeName) {
        this.principalDnAttributeName = principalDnAttributeName;
    }

    @JacksonXmlElementWrapper(localName = "additionalAttributes")
    @JacksonXmlProperty(localName = "additionalAttribute")
    public List<String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public boolean isAllowMultiplePrincipalAttributeValues() {
        return allowMultiplePrincipalAttributeValues;
    }

    public void setAllowMultiplePrincipalAttributeValues(final boolean allowMultiplePrincipalAttributeValues) {
        this.allowMultiplePrincipalAttributeValues = allowMultiplePrincipalAttributeValues;
    }

    public boolean isAllowMissingPrincipalAttributeValue() {
        return allowMissingPrincipalAttributeValue;
    }

    public void setAllowMissingPrincipalAttributeValue(final boolean allowMissingPrincipalAttributeValue) {
        this.allowMissingPrincipalAttributeValue = allowMissingPrincipalAttributeValue;
    }

    public boolean isCollectDnAttribute() {
        return collectDnAttribute;
    }

    public void setCollectDnAttribute(final boolean collectDnAttribute) {
        this.collectDnAttribute = collectDnAttribute;
    }

    public String getCredentialCriteria() {
        return credentialCriteria;
    }

    public void setCredentialCriteria(final String credentialCriteria) {
        this.credentialCriteria = credentialCriteria;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
