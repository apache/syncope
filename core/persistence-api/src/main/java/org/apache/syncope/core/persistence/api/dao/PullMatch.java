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
package org.apache.syncope.core.persistence.api.dao;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class PullMatch implements Serializable {

    private static final long serialVersionUID = 6515473131174179932L;

    public enum MatchTarget {
        ANY,
        LINKED_ACCOUNT;

    }

    public static class Builder {

        private final PullMatch instance = new PullMatch();

        public Builder matchingKey(final String matchingKey) {
            instance.matchingKey = matchingKey;
            return this;
        }

        public Builder matchTarget(final MatchTarget matchTarget) {
            instance.matchTarget = matchTarget;
            return this;
        }

        public Builder linkingUserKey(final String linkingUserKey) {
            instance.linkingUserKey = linkingUserKey;
            return this;
        }

        public PullMatch build() {
            return instance;
        }
    }

    private MatchTarget matchTarget = MatchTarget.ANY;

    private String matchingKey;

    private String linkingUserKey;

    private PullMatch() {
        // private constructor
    }

    public MatchTarget getMatchTarget() {
        return matchTarget;
    }

    public String getMatchingKey() {
        return matchingKey;
    }

    public String getLinkingUserKey() {
        return linkingUserKey;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(matchTarget).
                append(matchingKey).
                append(linkingUserKey).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PullMatch other = (PullMatch) obj;
        return new EqualsBuilder().
                append(matchingKey, other.matchingKey).
                append(matchTarget, other.matchTarget).
                append(linkingUserKey, other.linkingUserKey).
                build();
    }

    @Override
    public String toString() {
        return "PullMatch{"
                + "matchTarget=" + matchTarget
                + ", matchingKey=" + matchingKey
                + ", linkingUserKey=" + linkingUserKey + '}';
    }
}
