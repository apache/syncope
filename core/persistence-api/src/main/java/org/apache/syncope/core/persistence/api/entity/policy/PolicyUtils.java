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
package org.apache.syncope.core.persistence.api.entity.policy;

import org.apache.syncope.common.lib.types.PolicyType;

public class PolicyUtils {

    protected final PolicyType type;

    public PolicyUtils(final PolicyType type) {
        this.type = type;
    }

    public PolicyType getType() {
        return type;
    }

    public Class<? extends Policy> policyClass() {
        switch (type) {
            case ACCOUNT:
                return AccountPolicy.class;

            case PASSWORD:
                return PasswordPolicy.class;

            case AUTH:
                return AuthPolicy.class;

            case ATTR_RELEASE:
                return AttrReleasePolicy.class;

            case ACCESS:
                return AccessPolicy.class;

            case TICKET_EXPIRATION:
                return TicketExpirationPolicy.class;

            case PROPAGATION:
                return PropagationPolicy.class;

            case INBOUND:
                return InboundPolicy.class;

            case PUSH:
            default:
                return PushPolicy.class;
        }
    }
}
