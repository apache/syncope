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
package org.apache.syncope.core.persistence.jpa.entity.policy;

import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtils;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;

public class JPAPolicyUtils implements PolicyUtils {

    private final PolicyType type;

    protected JPAPolicyUtils(final PolicyType type) {
        this.type = type;
    }

    @Override
    public PolicyType getType() {
        return type;
    }

    @Override
    public Class<? extends Policy> policyClass() {
        switch (type) {
            case ACCOUNT:
                return AccountPolicy.class;

            case PASSWORD:
                return PasswordPolicy.class;

            case PULL:
                return PullPolicy.class;

            case AUTH:
                return AuthPolicy.class;

            case ATTR_RELEASE:
                return AttrReleasePolicy.class;

            case ACCESS:
                return AccessPolicy.class;

            case PUSH:
            default:
                return PushPolicy.class;
        }
    }
}
