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

public class GoogleMfaAuthModuleConf implements MFAAuthModuleConf {

    private static final long serialVersionUID = -7883257599139312426L;

    public static class LDAP implements Serializable {

        private static final long serialVersionUID = -7274446267090678730L;

        /**
         * Name of LDAP attribute that holds GAuth account/credential as JSON.
         */
        private String accountAttributeName = "casGAuthRecord";

        /**
         * Base DN to use. There may be scenarios where different parts of a single LDAP tree
         * could be considered as base-dns. Each entry can be specified
         * and joined together using a special delimiter character.
         */
        private String baseDn;

        /**
         * The bind credential to use when connecting to LDAP.
         */
        private String bindCredential;

        /**
         * The bind DN to use when connecting to LDAP.
         */
        private String bindDn;

        /**
         * The LDAP url to the server. More than one may be specified, separated by space and/or comma.
         */
        private String url;

        /**
         * User filter to use for searching. Syntax is i.e. cn={user} or cn={0}.
         */
        private String searchFilter;

        /**
         * Whether subtree searching is allowed.
         */
        private boolean subtreeSearch = true;

        public String getAccountAttributeName() {
            return accountAttributeName;
        }

        public void setAccountAttributeName(final String accountAttributeName) {
            this.accountAttributeName = accountAttributeName;
        }

        public String getBaseDn() {
            return baseDn;
        }

        public void setBaseDn(final String baseDn) {
            this.baseDn = baseDn;
        }

        public String getBindCredential() {
            return bindCredential;
        }

        public void setBindCredential(final String bindCredential) {
            this.bindCredential = bindCredential;
        }

        public String getBindDn() {
            return bindDn;
        }

        public void setBindDn(final String bindDn) {
            this.bindDn = bindDn;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getSearchFilter() {
            return searchFilter;
        }

        public void setSearchFilter(final String searchFilter) {
            this.searchFilter = searchFilter;
        }

        public boolean isSubtreeSearch() {
            return subtreeSearch;
        }

        public void setSubtreeSearch(final boolean subtreeSearch) {
            this.subtreeSearch = subtreeSearch;
        }
    }

    /**
     * Issuer used in the barcode when dealing with device registration events.
     * Used in the registration URL to identify CAS.
     */
    private String issuer = "Syncope";

    /**
     * Label used in the barcode when dealing with device registration events.
     * Used in the registration URL to identify CAS.
     */
    private String label = "Syncope";

    /**
     * Length of the generated code.
     */
    private int codeDigits = 6;

    /**
     * The expiration time of the generated code in seconds.
     */
    private long timeStepSize = 30;

    /**
     * Since TOTP passwords are time-based, it is essential that
     * the clock of both the server and
     * the client are synchronised within
     * the tolerance defined here as the window size.
     */
    private int windowSize = 3;

    private LDAP ldap;

    @Override
    public String getFriendlyName() {
        return "Google Authenticator";
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public int getCodeDigits() {
        return codeDigits;
    }

    public void setCodeDigits(final int codeDigits) {
        this.codeDigits = codeDigits;
    }

    public long getTimeStepSize() {
        return timeStepSize;
    }

    public void setTimeStepSize(final long timeStepSize) {
        this.timeStepSize = timeStepSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    public LDAP getLdap() {
        return ldap;
    }

    public void setLdap(final LDAP ldap) {
        this.ldap = ldap;
    }
}
