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
package org.apache.syncope.common.lib;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractLDAPConf implements Serializable {

    private static final long serialVersionUID = 3705514707899419599L;

    /**
     * The ldap type used to handle specific ops.
     */
    public enum LdapType {

        /**
         * Generic ldap type (OpenLDAP, 389ds, etc).
         */
        GENERIC,
        /**
         * Active directory.
         */
        AD,
        /**
         * FreeIPA directory.
         */
        FreeIPA,
        /**
         * EDirectory.
         */
        EDirectory

    }

    /**
     * The ldap connection pool passivator.
     */
    public enum LdapConnectionPoolPassivator {

        /**
         * No passivator.
         */
        NONE,
        /**
         * Bind passivator.
         */
        BIND

    }

    public enum LdapConnectionStrategy {

        /**
         * First ldap used until it fails.
         */
        ACTIVE_PASSIVE,
        /**
         * Navigate the ldap url list for new connections and circle back.
         */
        ROUND_ROBIN,
        /**
         * Randomly pick a url.
         */
        RANDOM,
        /**
         * ldap urls based on DNS SRV records.
         */
        DNS_SRV

    }

    /**
     * Describe hostname verification strategies.
     */
    public enum LdapHostnameVerifier {
        /**
         * Default option, forcing verification.
         */
        DEFAULT,
        /**
         * Skip hostname verification and allow all.
         */
        ANY

    }

    /**
     * Describe trust manager strategies.
     */
    public enum LdapTrustManager {
        /**
         * Loads the trust managers from the default {@link javax.net.ssl.TrustManagerFactory} and delegates to those.
         */
        DEFAULT,
        /**
         * Trusts any client or server.
         */
        ANY

    }

    /**
     * User filter to use for searching.
     * Syntax is {@code cn={user}} or {@code cn={0}}.
     *
     * You may also provide an external groovy script in the syntax of {@code file:/path/to/GroovyScript.groovy}
     * to fully build the final filter template dynamically.
     */
    private String searchFilter;

    /**
     * Whether subtree searching is allowed.
     */
    private boolean subtreeSearch = true;

    /**
     * Request that the server return results in batches of a
     * specific size. See <a href="http://www.ietf.org/rfc/rfc2696.txt">RFC 2696</a>. This control is often
     * used to work around server result size limits.
     * A negative/zero value disables paged requests.
     */
    private int pageSize;

    /**
     * Base DN to use.
     * There may be scenarios where different parts of a single LDAP tree could be considered as base-dns. Rather than
     * duplicating the LDAP configuration block for each individual base-dn, each entry can be specified
     * and joined together using a special delimiter character. The user DN is retrieved using the combination of all
     * base-dn and DN resolvers in the order defined. DN resolution should fail if multiple DNs are found. Otherwise the
     * first DN found is returned.
     * Usual syntax is: {@code subtreeA,dc=example,dc=net|subtreeC,dc=example,dc=net}.
     */
    private String baseDn;

    private String ldapUrl;

    /**
     * LDAP type.
     */
    private LdapType ldapType = LdapType.GENERIC;

    /**
     * The bind DN to use when connecting to LDAP.
     * LDAP connection configuration injected into the LDAP connection pool
     * can be initialized with the following parameters:
     * <ul>
     * <li>{@code bindDn/bindCredential} provided - Use the provided credentials
     * to bind when initializing connections.</li>
     * <li>{@code bindDn/bindCredential} set to {@code *} - Use a fast-bind
     * strategy to initialize the pool.</li>
     * <li>{@code bindDn/bindCredential} set to blank - Skip connection
     * initializing; perform operations anonymously.</li>
     * <li>SASL mechanism provided - Use the given SASL mechanism
     * to bind when initializing connections. </li>
     * </ul>
     */
    private String bindDn;

    /**
     * The bind credential to use when connecting to LDAP.
     */
    private String bindCredential;

    /**
     * Whether to use a pooled connection factory in components.
     */
    private boolean disablePooling;

    /**
     * Minimum LDAP connection pool size.
     * Size the pool should be initialized to and pruned to
     */
    private int minPoolSize = 3;

    /**
     * Maximum LDAP connection pool size which the pool can use to grow.
     */
    private int maxPoolSize = 10;

    /**
     * You may receive unexpected LDAP failures, when CAS is configured to authenticate
     * using {@code DIRECT} or {@code AUTHENTICATED} types and LDAP is locked down to not allow anonymous
     * binds/searches.
     * Every second attempt with a given LDAP connection from the pool would fail if it was on
     * the same connection as a failed login attempt, and the regular connection validator would
     * similarly fail. When a connection is returned back to a pool,
     * it still may contain the principal and credentials from the previous attempt.
     * Before the next bind attempt using that connection, the validator tries to
     * validate the connection again but fails because it’s no longer trying with the
     * configured bind credentials but with whatever user DN was used in the previous step.
     * Given the validation failure, the connection is closed and CAS would deny
     * access by default. Passivators attempt to reconnect
     * to LDAP with the configured bind credentials, effectively resetting the connection
     * to what it should be after each bind request.
     * Furthermore if you are seeing errors in the logs that resemble
     * a 'Operation exception encountered, reopening connection' type of message, this
     * usually is an indication that the connection pool’s validation timeout
     * established and created by CAS is greater than the timeout configured
     * in the LDAP server, or more likely, in the load balancer in front of
     * the LDAP servers. You can adjust the LDAP server session’s timeout
     * for connections, or you can teach CAS to use a validity period that
     * is equal or less than the LDAP server session’s timeout.
     * Accepted values are:
     * <ul>
     * <li>{@code NONE}: No passivation takes place.</li>
     * <li>{@code BIND}: The default behavior which passivates a connection by performing a
     * bind operation on it. This option requires the availability of bind credentials when establishing connections to
     * LDAP.</li>
     * </ul>
     */
    private LdapConnectionPoolPassivator poolPassivator = LdapConnectionPoolPassivator.BIND;

    /**
     * Hostname verification options.
     */
    private LdapHostnameVerifier hostnameVerifier = LdapHostnameVerifier.DEFAULT;

    /**
     * Trust Manager options.
     * Trust managers are responsible for managing the trust material that is used when making LDAP trust decisions,
     * and for deciding whether credentials presented by a peer should be accepted.
     */
    private LdapTrustManager trustManager;

    /**
     * Whether connections should be validated when loaned out from the pool.
     */
    private boolean validateOnCheckout = true;

    /**
     * Whether connections should be validated periodically when the pool is idle.
     */
    private boolean validatePeriodically = true;

    /**
     * Period at which validation operations may time out.
     */
    private Duration validateTimeout = Duration.parse("PT5S");

    /**
     * Period at which pool should be validated.
     */
    private Duration validatePeriod = Duration.parse("PT5M");

    /**
     * Attempt to populate the connection pool early on startup
     * and fail quickly if something goes wrong.
     */
    private boolean failFast = true;

    /**
     * Removes connections from the pool based on how long they have been idle in the available queue.
     * Prunes connections that have been idle for more than the indicated amount.
     */
    private Duration idleTime = Duration.parse("PT10M");

    /**
     * Removes connections from the pool based on how long they have been idle in the available queue.
     * Run the pruning process at the indicated interval.
     */
    private Duration prunePeriod = Duration.parse("PT2H");

    /**
     * The length of time the pool will block.
     * By default the pool will block indefinitely and there is no guarantee that
     * waiting threads will be serviced in the order in which they made their request.
     * This option should be used with a blocking connection pool when you need to control the exact
     * number of connections that can be created
     */
    private Duration blockWaitTime = Duration.parse("PT3S");

    /**
     * If multiple URLs are provided as the ldapURL this describes how each URL will be processed.
     * <ul>
     * <li>{@code ACTIVE_PASSIVE} First LDAP will be used for every request unless it fails and then the next shall be
     * used.</li>
     * <li>{@code ROUND_ROBIN} For each new connection the next url in the list will be used.</li>
     * <li>{@code RANDOM} For each new connection a random LDAP url will be selected.</li>
     * <li>{@code DNS_SRV} LDAP urls based on DNS SRV records of the configured/given LDAP url will be used. </li>
     * </ul>
     */
    private LdapConnectionStrategy connectionStrategy;

    /**
     * Whether TLS should be used and enabled when establishing the connection.
     */
    private boolean useStartTls;

    /**
     * Sets the maximum amount of time that connects will block.
     */
    private Duration connectTimeout = Duration.parse("PT5S");

    /**
     * Duration of time to wait for responses.
     */
    private Duration responseTimeout = Duration.parse("PT5S");

    /**
     * Whether search/query results are allowed to match on multiple DNs,
     * or whether a single unique DN is expected for the result.
     */
    private boolean allowMultipleDns;

    /**
     * Set if multiple Entries are allowed.
     */
    private boolean allowMultipleEntries;

    /**
     * Set if search referrals should be followed.
     */
    private boolean followReferrals = true;

    /**
     * Indicate the collection of attributes that are to be tagged and processed as binary
     * attributes by the underlying search resolver.
     */
    private List<String> binaryAttributes = Stream.of("objectGUID", "objectSid").collect(Collectors.toList());

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(final String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isSubtreeSearch() {
        return subtreeSearch;
    }

    public void setSubtreeSearch(final boolean subtreeSearch) {
        this.subtreeSearch = subtreeSearch;
    }

    public LdapType getLdapType() {
        return ldapType;
    }

    public void setLdapType(final LdapType ldapType) {
        this.ldapType = ldapType;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(final String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(final String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindCredential() {
        return bindCredential;
    }

    public void setBindCredential(final String bindCredential) {
        this.bindCredential = bindCredential;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(final String baseDn) {
        this.baseDn = baseDn;
    }

    public boolean isDisablePooling() {
        return disablePooling;
    }

    public void setDisablePooling(final boolean disablePooling) {
        this.disablePooling = disablePooling;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public LdapConnectionPoolPassivator getPoolPassivator() {
        return poolPassivator;
    }

    public void setPoolPassivator(final LdapConnectionPoolPassivator poolPassivator) {
        this.poolPassivator = poolPassivator;
    }

    public LdapHostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(final LdapHostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public LdapTrustManager getTrustManager() {
        return trustManager;
    }

    public void setTrustManager(final LdapTrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public boolean isValidateOnCheckout() {
        return validateOnCheckout;
    }

    public void setValidateOnCheckout(final boolean validateOnCheckout) {
        this.validateOnCheckout = validateOnCheckout;
    }

    public boolean isValidatePeriodically() {
        return validatePeriodically;
    }

    public void setValidatePeriodically(final boolean validatePeriodically) {
        this.validatePeriodically = validatePeriodically;
    }

    public Duration getValidateTimeout() {
        return validateTimeout;
    }

    public void setValidateTimeout(final Duration validateTimeout) {
        this.validateTimeout = validateTimeout;
    }

    public Duration getValidatePeriod() {
        return validatePeriod;
    }

    public void setValidatePeriod(final Duration validatePeriod) {
        this.validatePeriod = validatePeriod;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(final boolean failFast) {
        this.failFast = failFast;
    }

    public Duration getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(final Duration idleTime) {
        this.idleTime = idleTime;
    }

    public Duration getPrunePeriod() {
        return prunePeriod;
    }

    public void setPrunePeriod(final Duration prunePeriod) {
        this.prunePeriod = prunePeriod;
    }

    public Duration getBlockWaitTime() {
        return blockWaitTime;
    }

    public void setBlockWaitTime(final Duration blockWaitTime) {
        this.blockWaitTime = blockWaitTime;
    }

    public LdapConnectionStrategy getConnectionStrategy() {
        return connectionStrategy;
    }

    public void setConnectionStrategy(final LdapConnectionStrategy connectionStrategy) {
        this.connectionStrategy = connectionStrategy;
    }

    public boolean isUseStartTls() {
        return useStartTls;
    }

    public void setUseStartTls(final boolean useStartTls) {
        this.useStartTls = useStartTls;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(final Duration responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public boolean isAllowMultipleDns() {
        return allowMultipleDns;
    }

    public void setAllowMultipleDns(final boolean allowMultipleDns) {
        this.allowMultipleDns = allowMultipleDns;
    }

    public boolean isAllowMultipleEntries() {
        return allowMultipleEntries;
    }

    public void setAllowMultipleEntries(final boolean allowMultipleEntries) {
        this.allowMultipleEntries = allowMultipleEntries;
    }

    public boolean isFollowReferrals() {
        return followReferrals;
    }

    public void setFollowReferrals(final boolean followReferrals) {
        this.followReferrals = followReferrals;
    }

    public List<String> getBinaryAttributes() {
        return binaryAttributes;
    }

    public void setBinaryAttributes(final List<String> binaryAttributes) {
        this.binaryAttributes = binaryAttributes;
    }
}
