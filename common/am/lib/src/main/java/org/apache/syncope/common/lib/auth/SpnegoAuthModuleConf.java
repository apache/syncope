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

import java.io.Serializable;
import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;

/**
 * SPNEGO is an authentication technology that is primarily used to provide transparent CAS authentication to browsers
 * running on Windows running under Active Directory domain credentials. There are three actors involved: the client,
 * the CAS server, and the Active Directory Domain Controller/KDC.
 */
public class SpnegoAuthModuleConf implements LDAPDependantAuthModuleConf {

    private static final long serialVersionUID = -7775771400312303131L;

    public static class LDAP extends AbstractLDAPConf implements Serializable {

        private static final long serialVersionUID = -7274446267090678730L;

    }

    /**
     * The Login conf.Absolute path to the jaas login configuration file.
     * This should define the spnego authentication details.
     * Make sure you have at least specified the JCIFS Service Principal defined.
     */
    private String loginConf;

    /**
     * The Kerberos conf.
     * As with all Kerberos installations, a Kerberos Key Distribution Center (KDC) is required.
     * It needs to contain the user name and password you will use to be authenticated to Kerberos.
     * As with most Kerberos installations, a Kerberos configuration file krb5.conf is
     * consulted to determine such things as the default realm and KDC.
     * Typically, the default realm and the KDC for that realm are indicated in
     * the Kerberos krb5.conf configuration file.
     * The path to the configuration file must typically be defined
     * as an absolute path.
     */
    private String kerberosConf;

    /**
     * The Kerberos kdc.
     */
    private String kerberosKdc = "172.10.1.10";

    /**
     * The Jcifs service principal.
     */
    private String jcifsServicePrincipal;

    /**
     * The Kerberos realm.
     */
    private String kerberosRealm = "EXAMPLE.COM";

    /**
     * The Kerberos debug.
     */
    private boolean kerberosDebug;

    /**
     * The Use subject creds only.
     */
    private boolean useSubjectCredsOnly;

    /**
     * If specified, will create the principal by ths name on successful authentication.
     */
    private boolean principalWithDomainName;

    /**
     * Allows authentication if spnego credential is marked as NTLM.
     */
    private boolean ntlmAllowed = true;

    /**
     * If the authenticated principal cannot be determined from the spegno credential,
     * will set the http status code to 401.
     */
    private boolean send401OnAuthenticationFailure = true;

    /**
     * The bean id of a webflow action whose job is to evaluate the client host
     * to see if the request is authorized for spnego.
     * Supported strategies include {@code hostnameSpnegoClientAction} where
     * CAS checks to see if the request’s remote hostname matches a predefine pattern.
     * and {@code ldapSpnegoClientAction} where
     * CAS checks an LDAP instance for the remote hostname, to locate a pre-defined attribute whose
     * mere existence would allow the webflow to resume to SPNEGO.
     */
    private String hostNameClientActionStrategy = "hostnameSpnegoClientAction";

    /**
     * LDAP settings for spnego to validate clients, etc.
     */
    private LDAP ldap;

    /**
     * When validating clients, specifies the DNS timeout used to look up an address.
     */
    private String dnsTimeout = "PT2S";

    /**
     * A regex pattern that indicates whether the client host name is allowed for spnego.
     */
    private String hostNamePatternString = ".+";

    /**
     * A regex pattern that indicates whether the client IP is allowed for spnego.
     */
    private String ipsToCheckPattern = "127.+";

    /**
     * Alternative header name to use in order to find the host address.
     */
    private String alternativeRemoteHostAttribute = "alternateRemoteHeader";

    /**
     * In case LDAP is used to validate clients, this is the attribute that indicates the host.
     */
    private String spnegoAttributeName = "distinguishedName";

    /**
     * If true, does not terminate authentication and allows CAS to resume
     * and fallback to normal authentication means such as uid/psw via the login page.
     * If disallowed, considers spnego authentication to be final in the event of failures.
     */
    private boolean mixedModeAuthentication;

    /**
     * Begins negotiating spnego if the user-agent is one of the supported browsers.
     */
    private String supportedBrowsers = "MSIE,Trident,Firefox,AppleWebKit";

    /**
     * The size of the pool used to validate SPNEGO tokens.
     * A pool is used to provider better performance than what was previously offered by the simple Lombok
     * {@code Synchronized} annotation.
     */
    private int poolSize = 10;

    /**
     * The timeout of the pool used to validate SPNEGO tokens.
     */
    private String poolTimeout = "PT2S";

    /**
     * Activated attribute repository identifiers that should be used for fetching attributes if attribute resolution is
     * enabled.
     * The list here may include identifiers separated by comma.
     */
    private String attributeRepoId;

    @Override
    public AbstractLDAPConf ldapInstance() {
        return new SpnegoAuthModuleConf.LDAP();
    }

    public String getJcifsServicePrincipal() {
        return jcifsServicePrincipal;
    }

    public void setJcifsServicePrincipal(final String jcifsServicePrincipal) {
        this.jcifsServicePrincipal = jcifsServicePrincipal;
    }

    public String getAttributeRepoId() {
        return attributeRepoId;
    }

    public void setAttributeRepoId(final String attributeRepoId) {
        this.attributeRepoId = attributeRepoId;
    }

    public String getLoginConf() {
        return loginConf;
    }

    public void setLoginConf(final String loginConf) {
        this.loginConf = loginConf;
    }

    public String getKerberosConf() {
        return kerberosConf;
    }

    public void setKerberosConf(final String kerberosConf) {
        this.kerberosConf = kerberosConf;
    }

    public String getKerberosKdc() {
        return kerberosKdc;
    }

    public void setKerberosKdc(final String kerberosKdc) {
        this.kerberosKdc = kerberosKdc;
    }

    public String getKerberosRealm() {
        return kerberosRealm;
    }

    public void setKerberosRealm(final String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    public boolean isKerberosDebug() {
        return kerberosDebug;
    }

    public void setKerberosDebug(final boolean kerberosDebug) {
        this.kerberosDebug = kerberosDebug;
    }

    public boolean isUseSubjectCredsOnly() {
        return useSubjectCredsOnly;
    }

    public void setUseSubjectCredsOnly(final boolean useSubjectCredsOnly) {
        this.useSubjectCredsOnly = useSubjectCredsOnly;
    }

    public boolean isPrincipalWithDomainName() {
        return principalWithDomainName;
    }

    public void setPrincipalWithDomainName(final boolean principalWithDomainName) {
        this.principalWithDomainName = principalWithDomainName;
    }

    public boolean isNtlmAllowed() {
        return ntlmAllowed;
    }

    public void setNtlmAllowed(final boolean ntlmAllowed) {
        this.ntlmAllowed = ntlmAllowed;
    }

    public boolean isSend401OnAuthenticationFailure() {
        return send401OnAuthenticationFailure;
    }

    public void setSend401OnAuthenticationFailure(final boolean send401OnAuthenticationFailure) {
        this.send401OnAuthenticationFailure = send401OnAuthenticationFailure;
    }

    public String getHostNameClientActionStrategy() {
        return hostNameClientActionStrategy;
    }

    public void setHostNameClientActionStrategy(final String hostNameClientActionStrategy) {
        this.hostNameClientActionStrategy = hostNameClientActionStrategy;
    }

    public LDAP getLdap() {
        return ldap;
    }

    public void setLdap(final LDAP ldap) {
        this.ldap = ldap;
    }

    public String getDnsTimeout() {
        return dnsTimeout;
    }

    public void setDnsTimeout(final String dnsTimeout) {
        this.dnsTimeout = dnsTimeout;
    }

    public String getHostNamePatternString() {
        return hostNamePatternString;
    }

    public void setHostNamePatternString(final String hostNamePatternString) {
        this.hostNamePatternString = hostNamePatternString;
    }

    public String getIpsToCheckPattern() {
        return ipsToCheckPattern;
    }

    public void setIpsToCheckPattern(final String ipsToCheckPattern) {
        this.ipsToCheckPattern = ipsToCheckPattern;
    }

    public String getAlternativeRemoteHostAttribute() {
        return alternativeRemoteHostAttribute;
    }

    public void setAlternativeRemoteHostAttribute(final String alternativeRemoteHostAttribute) {
        this.alternativeRemoteHostAttribute = alternativeRemoteHostAttribute;
    }

    public String getSpnegoAttributeName() {
        return spnegoAttributeName;
    }

    public void setSpnegoAttributeName(final String spnegoAttributeName) {
        this.spnegoAttributeName = spnegoAttributeName;
    }

    public boolean isMixedModeAuthentication() {
        return mixedModeAuthentication;
    }

    public void setMixedModeAuthentication(final boolean mixedModeAuthentication) {
        this.mixedModeAuthentication = mixedModeAuthentication;
    }

    public String getSupportedBrowsers() {
        return supportedBrowsers;
    }

    public void setSupportedBrowsers(final String supportedBrowsers) {
        this.supportedBrowsers = supportedBrowsers;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(final int poolSize) {
        this.poolSize = poolSize;
    }

    public String getPoolTimeout() {
        return poolTimeout;
    }

    public void setPoolTimeout(final String poolTimeout) {
        this.poolTimeout = poolTimeout;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
