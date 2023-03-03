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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultAttrReleasePolicyConf implements AttrReleasePolicyConf {

    private static final long serialVersionUID = -1969836661359025380L;

    public enum PrincipalAttrRepoMergingStrategy {
        /**
         * Replace attributes. Overwrites existing attribute values, if any.
         */
        REPLACE,
        /**
         * Add attributes.
         * Retains existing attribute values if any, and ignores values from subsequent sources in the resolution chain.
         */
        ADD,
        /**
         * No merging.
         * Doesn't merge attributes, ignores attributes from non-authentication attribute repositories.
         */
        NONE,
        /**
         * Multivalued attributes.
         * Combines all values into a single attribute, essentially creating a multi-valued attribute.
         */
        MULTIVALUED;

    }

    public static class PrincipalAttrRepoConf implements Serializable {

        private static final long serialVersionUID = 6369987956789092057L;

        private PrincipalAttrRepoMergingStrategy mergingStrategy = PrincipalAttrRepoMergingStrategy.MULTIVALUED;

        private boolean ignoreResolvedAttributes;

        private long expiration;

        private TimeUnit timeUnit = TimeUnit.HOURS;

        private final List<String> attrRepos = new ArrayList<>();

        public PrincipalAttrRepoMergingStrategy getMergingStrategy() {
            return mergingStrategy;
        }

        public void setMergingStrategy(final PrincipalAttrRepoMergingStrategy mergingStrategy) {
            this.mergingStrategy = mergingStrategy;
        }

        public boolean isIgnoreResolvedAttributes() {
            return ignoreResolvedAttributes;
        }

        public void setIgnoreResolvedAttributes(final boolean ignoreResolvedAttributes) {
            this.ignoreResolvedAttributes = ignoreResolvedAttributes;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(final long expiration) {
            this.expiration = expiration;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setTimeUnit(final TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }

        @JacksonXmlElementWrapper(localName = "attrRepos")
        @JacksonXmlProperty(localName = "attrRepo")
        public List<String> getAttrRepos() {
            return attrRepos;
        }
    }

    private final Map<String, Object> releaseAttrs = new HashMap<>();

    /**
     * Specify the list of allowed attribute to release.
     * Use the special {@code *} to release everything.
     */
    private final List<String> allowedAttrs = new ArrayList<>();

    private final List<String> excludedAttrs = new ArrayList<>();

    private final List<String> includeOnlyAttrs = new ArrayList<>();

    private String principalIdAttr;

    private final PrincipalAttrRepoConf principalAttrRepoConf = new PrincipalAttrRepoConf();

    public Map<String, Object> getReleaseAttrs() {
        return releaseAttrs;
    }

    @JacksonXmlElementWrapper(localName = "allowedAttrs")
    @JacksonXmlProperty(localName = "allowedAttr")
    public List<String> getAllowedAttrs() {
        return allowedAttrs;
    }

    @JacksonXmlElementWrapper(localName = "excludedAttrs")
    @JacksonXmlProperty(localName = "excludedAttr")
    public List<String> getExcludedAttrs() {
        return excludedAttrs;
    }

    @JacksonXmlElementWrapper(localName = "includeOnlyAttrs")
    @JacksonXmlProperty(localName = "includeOnlyAttr")
    public List<String> getIncludeOnlyAttrs() {
        return includeOnlyAttrs;
    }

    public String getPrincipalIdAttr() {
        return principalIdAttr;
    }

    public void setPrincipalIdAttr(final String principalIdAttr) {
        this.principalIdAttr = principalIdAttr;
    }

    public PrincipalAttrRepoConf getPrincipalAttrRepoConf() {
        return principalAttrRepoConf;
    }
}
