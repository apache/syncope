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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.X509PolicySetting;
import org.apache.syncope.common.lib.types.X509PrincipalType;
import org.apache.syncope.common.lib.types.X509RevocationCheckerType;
import org.apache.syncope.common.lib.types.X509RevocationFetcherType;
import org.apache.syncope.common.lib.types.X509SubjectDnFormat;

public class X509AuthModuleConf implements LDAPDependantAuthModuleConf {

    private static final long serialVersionUID = 1915254775199296906L;

    public static class LDAP extends AbstractLDAPConf implements Serializable {

        private static final long serialVersionUID = -7274446267090678730L;

        /**
         * The LDAP attribute that holds the certificate revocation list.
         */
        private String certificateAttribute = "certificateRevocationList";

        public String getCertificateAttribute() {
            return certificateAttribute;
        }

        public void setCertificateAttribute(final String certificateAttribute) {
            this.certificateAttribute = certificateAttribute;
        }
    }

    /**
     * The authentication handler name.
     */
    private String name;

    /**
     * The order of the authentication handler in the chain.
     */
    private int order = Integer.MAX_VALUE;

    /**
     * Threshold value if expired CRL revocation policy is to be handled via threshold.
     */
    private int revocationPolicyThreshold = 172_800;

    /**
     * Whether revocation checking should check all resources, or stop at first one.
     */
    private boolean checkAll;

    /**
     * The refresh interval of the internal scheduler in cases where CRL revocation checking
     * is done via resources.
     */
    private int refreshIntervalSeconds = 3_600;

    /**
     * When CRL revocation checking is done via distribution points,
     * decide if fetch failures should throw errors.
     */
    private boolean throwOnFetchFailure;

    private X509PrincipalType principalType = X509PrincipalType.SUBJECT_DN;

    /**
     * Relevant for {@code CN_EDIPI}, {@code RFC822_EMAIL}, {@code SUBJECT}, {@code SUBJECT_ALT_NAME} principal types.
     */
    private String principalAlternateAttribute;

    /**
     * Relevant for {@code SUBJECT_DN} principal type.
     */
    private X509SubjectDnFormat principalTypeSubjectDnFormat = X509SubjectDnFormat.DEFAULT;

    /**
     * Relevant for {@code SERIAL_NO_DN} principal type.
     * The serial number prefix used for principal resolution.
     */
    private String principalTypeSerialNoDnSerialNumberPrefix = "SERIALNUMBER=";

    /**
     * Relevant for {@code SERIAL_NO_DN} principal type.
     * Value delimiter used for principal resolution.
     */
    private String principalTypeSerialNoDnValueDelimiter = ", ";

    /**
     * Relevant for {@code SERIAL_NO} principal type.
     * Radix used.
     */
    private int principalTypeSerialNoSNRadix;

    /**
     * Relevant for {@code SERIAL_NO} principal type.
     * If radix hex padding should be used.
     */
    private boolean principalTypeSerialNoHexSNZeroPadding;

    /**
     * Revocation certificate checking is carried out according to this setting.
     */
    private X509RevocationCheckerType revocationChecker = X509RevocationCheckerType.NONE;

    /**
     * Options to describe how to fetch CRL resources.
     */
    private X509RevocationFetcherType crlFetcher = X509RevocationFetcherType.RESOURCE;

    /**
     * List of CRL resources to use for fetching.
     */
    private final List<String> crlResources = new ArrayList<>(0);

    /**
     * When CRLs are cached, indicate maximum number of elements kept in memory.
     */
    private int cacheMaxElementsInMemory = 1_000;

    /**
     * Determine whether X509 authentication should allow other forms of authentication such as username/password.
     * If this setting is turned off, typically the ability to view the login form as the primary form of
     * authentication is turned off.
     */
    private boolean mixedMode = true;

    /**
     * When CRLs are cached, indicate the time-to-live of cache items.
     */
    private String cacheTimeToLiveSeconds = "PT4H";

    /**
     * If the CRL resource is unavailable, activate the this policy.
     */
    private X509PolicySetting crlResourceUnavailablePolicy = X509PolicySetting.DENY;

    /**
     * If the CRL resource has expired, activate the this policy.
     * Activated if {@link #revocationChecker} is {@code RESOURCE}.
     */
    private X509PolicySetting crlResourceExpiredPolicy = X509PolicySetting.DENY;

    /**
     * If the CRL is unavailable, activate the this policy.
     * Activated if {@link #revocationChecker} is {@code CRL}.
     */
    private X509PolicySetting crlUnavailablePolicy = X509PolicySetting.DENY;

    /**
     * If the CRL has expired, activate the this policy.
     * Activated if {@link #revocationChecker} is {@code CRL}.
     */
    private X509PolicySetting crlExpiredPolicy = X509PolicySetting.DENY;

    /**
     * The compiled pattern supplied by the deployer.
     */
    private String regExTrustedIssuerDnPattern;

    /**
     * Deployer supplied setting for maximum pathLength in a SUPPLIED
     * certificate.
     */
    private int maxPathLength = 1;

    /**
     * Deployer supplied setting to allow unlimited pathLength in a SUPPLIED
     * certificate.
     */
    private boolean maxPathLengthAllowUnspecified = false;

    /**
     * Deployer supplied setting to check the KeyUsage extension.
     */
    private boolean checkKeyUsage = false;

    /**
     * Deployer supplied setting to force require the correct KeyUsage
     * extension.
     */
    private boolean requireKeyUsage = false;

    /**
     * The pattern that authorizes an acceptable certificate by its subject dn.
     */
    private String regExSubjectDnPattern = ".+";

    /**
     * Whether to extract certificate from request.
     * The default implementation extracts certificate from header via Tomcat SSLValve parsing logic
     * and using the {@link #sslHeaderName} header.
     * Must be false by default because if someone enables it they need to make sure they are
     * behind proxy that won't let the header arrive directly from the browser.
     */
    private boolean extractCert;

    /**
     * The name of the header to consult for an X509 cert (e.g. when behind proxy).
     */
    private String sslHeaderName = "ssl_client_cert";

    private LDAP ldap;

    @Override
    public AbstractLDAPConf ldapInstance() {
        return new X509AuthModuleConf.LDAP();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public int getRevocationPolicyThreshold() {
        return revocationPolicyThreshold;
    }

    public void setRevocationPolicyThreshold(final int revocationPolicyThreshold) {
        this.revocationPolicyThreshold = revocationPolicyThreshold;
    }

    public boolean isCheckAll() {
        return checkAll;
    }

    public void setCheckAll(final boolean checkAll) {
        this.checkAll = checkAll;
    }

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(final int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    public boolean isThrowOnFetchFailure() {
        return throwOnFetchFailure;
    }

    public void setThrowOnFetchFailure(final boolean throwOnFetchFailure) {
        this.throwOnFetchFailure = throwOnFetchFailure;
    }

    public X509PrincipalType getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(final X509PrincipalType principalType) {
        this.principalType = principalType;
    }

    public String getPrincipalAlternateAttribute() {
        return principalAlternateAttribute;
    }

    public void setPrincipalAlternateAttribute(final String principalAlternateAttribute) {
        this.principalAlternateAttribute = principalAlternateAttribute;
    }

    public X509SubjectDnFormat getPrincipalTypeSubjectDnFormat() {
        return principalTypeSubjectDnFormat;
    }

    public void setPrincipalTypeSubjectDnFormat(final X509SubjectDnFormat principalTypeSubjectDnFormat) {
        this.principalTypeSubjectDnFormat = principalTypeSubjectDnFormat;
    }

    public String getPrincipalTypeSerialNoDnSerialNumberPrefix() {
        return principalTypeSerialNoDnSerialNumberPrefix;
    }

    public void setPrincipalTypeSerialNoDnSerialNumberPrefix(final String principalTypeSerialNoDnSerialNumberPrefix) {
        this.principalTypeSerialNoDnSerialNumberPrefix = principalTypeSerialNoDnSerialNumberPrefix;
    }

    public String getPrincipalTypeSerialNoDnValueDelimiter() {
        return principalTypeSerialNoDnValueDelimiter;
    }

    public void setPrincipalTypeSerialNoDnValueDelimiter(final String principalTypeSerialNoDnValueDelimiter) {
        this.principalTypeSerialNoDnValueDelimiter = principalTypeSerialNoDnValueDelimiter;
    }

    public int getPrincipalTypeSerialNoSNRadix() {
        return principalTypeSerialNoSNRadix;
    }

    public void setPrincipalTypeSerialNoSNRadix(final int principalTypeSerialNoSNRadix) {
        this.principalTypeSerialNoSNRadix = principalTypeSerialNoSNRadix;
    }

    public boolean isPrincipalTypeSerialNoHexSNZeroPadding() {
        return principalTypeSerialNoHexSNZeroPadding;
    }

    public void setPrincipalTypeSerialNoHexSNZeroPadding(final boolean principalTypeSerialNoHexSNZeroPadding) {
        this.principalTypeSerialNoHexSNZeroPadding = principalTypeSerialNoHexSNZeroPadding;
    }

    public X509RevocationCheckerType getRevocationChecker() {
        return revocationChecker;
    }

    public void setRevocationChecker(final X509RevocationCheckerType revocationChecker) {
        this.revocationChecker = revocationChecker;
    }

    public X509RevocationFetcherType getCrlFetcher() {
        return crlFetcher;
    }

    public void setCrlFetcher(final X509RevocationFetcherType crlFetcher) {
        this.crlFetcher = crlFetcher;
    }

    public int getCacheMaxElementsInMemory() {
        return cacheMaxElementsInMemory;
    }

    public void setCacheMaxElementsInMemory(final int cacheMaxElementsInMemory) {
        this.cacheMaxElementsInMemory = cacheMaxElementsInMemory;
    }

    public boolean isMixedMode() {
        return mixedMode;
    }

    public void setMixedMode(final boolean mixedMode) {
        this.mixedMode = mixedMode;
    }

    public String getCacheTimeToLiveSeconds() {
        return cacheTimeToLiveSeconds;
    }

    public void setCacheTimeToLiveSeconds(final String cacheTimeToLiveSeconds) {
        this.cacheTimeToLiveSeconds = cacheTimeToLiveSeconds;
    }

    public X509PolicySetting getCrlResourceUnavailablePolicy() {
        return crlResourceUnavailablePolicy;
    }

    public void setCrlResourceUnavailablePolicy(final X509PolicySetting crlResourceUnavailablePolicy) {
        this.crlResourceUnavailablePolicy = crlResourceUnavailablePolicy;
    }

    public X509PolicySetting getCrlResourceExpiredPolicy() {
        return crlResourceExpiredPolicy;
    }

    public void setCrlResourceExpiredPolicy(final X509PolicySetting crlResourceExpiredPolicy) {
        this.crlResourceExpiredPolicy = crlResourceExpiredPolicy;
    }

    public X509PolicySetting getCrlUnavailablePolicy() {
        return crlUnavailablePolicy;
    }

    public void setCrlUnavailablePolicy(final X509PolicySetting crlUnavailablePolicy) {
        this.crlUnavailablePolicy = crlUnavailablePolicy;
    }

    public X509PolicySetting getCrlExpiredPolicy() {
        return crlExpiredPolicy;
    }

    public void setCrlExpiredPolicy(final X509PolicySetting crlExpiredPolicy) {
        this.crlExpiredPolicy = crlExpiredPolicy;
    }

    public List<String> getCrlResources() {
        return crlResources;
    }

    public String getRegExTrustedIssuerDnPattern() {
        return regExTrustedIssuerDnPattern;
    }

    public void setRegExTrustedIssuerDnPattern(final String regExTrustedIssuerDnPattern) {
        this.regExTrustedIssuerDnPattern = regExTrustedIssuerDnPattern;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(final int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public boolean isMaxPathLengthAllowUnspecified() {
        return maxPathLengthAllowUnspecified;
    }

    public void setMaxPathLengthAllowUnspecified(final boolean maxPathLengthAllowUnspecified) {
        this.maxPathLengthAllowUnspecified = maxPathLengthAllowUnspecified;
    }

    public boolean isCheckKeyUsage() {
        return checkKeyUsage;
    }

    public void setCheckKeyUsage(final boolean checkKeyUsage) {
        this.checkKeyUsage = checkKeyUsage;
    }

    public boolean isRequireKeyUsage() {
        return requireKeyUsage;
    }

    public void setRequireKeyUsage(final boolean requireKeyUsage) {
        this.requireKeyUsage = requireKeyUsage;
    }

    public String getRegExSubjectDnPattern() {
        return regExSubjectDnPattern;
    }

    public void setRegExSubjectDnPattern(final String regExSubjectDnPattern) {
        this.regExSubjectDnPattern = regExSubjectDnPattern;
    }

    public boolean isExtractCert() {
        return extractCert;
    }

    public void setExtractCert(final boolean extractCert) {
        this.extractCert = extractCert;
    }

    public String getSslHeaderName() {
        return sslHeaderName;
    }

    public void setSslHeaderName(final String sslHeaderName) {
        this.sslHeaderName = sslHeaderName;
    }

    public LDAP getLdap() {
        return ldap;
    }

    public void setLdap(final LDAP ldap) {
        this.ldap = ldap;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
