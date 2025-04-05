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
package org.apache.syncope.core.provisioning.api.rules;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;

public final class InboundMatch implements Serializable {

    private static final long serialVersionUID = 6515473131174179932L;

    private final MatchType matchTarget;

    private Any any;

    private LinkedAccount linkedAccount;

    public InboundMatch(final MatchType matchTarget, final Entity entity) {
        this.matchTarget = matchTarget;

        if (entity instanceof final Any any) {
            this.any = any;
        } else if (entity instanceof LinkedAccount linkedAccount1) {
            linkedAccount = linkedAccount1;
        }
    }

    public MatchType getMatchTarget() {
        return matchTarget;
    }

    public Any getAny() {
        return any;
    }

    public LinkedAccount getLinkedAccount() {
        return linkedAccount;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(matchTarget).
                append(any).
                append(linkedAccount).
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
        final InboundMatch other = (InboundMatch) obj;
        return new EqualsBuilder().
                append(matchTarget, other.matchTarget).
                append(any, other.any).
                append(linkedAccount, other.linkedAccount).
                build();
    }

    @Override
    public String toString() {
        return "InboundMatch{"
                + "matchTarget=" + matchTarget
                + ", any=" + any
                + ", linkedAccount=" + linkedAccount
                + '}';
    }
}
