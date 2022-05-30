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

import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.policy.PushPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtils;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;

public class JPAPolicyUtilsFactory implements PolicyUtilsFactory {

    @Override
    public PolicyUtils getInstance(final PolicyType type) {
        return new JPAPolicyUtils(type);
    }

    @Override
    public PolicyUtils getInstance(final Policy policy) {
        PolicyType type;
        if (policy instanceof AccountPolicy) {
            type = PolicyType.ACCOUNT;
        } else if (policy instanceof PasswordPolicy) {
            type = PolicyType.PASSWORD;
        } else if (policy instanceof PropagationPolicy) {
            type = PolicyType.PROPAGATION;
        } else if (policy instanceof PullPolicy) {
            type = PolicyType.PULL;
        } else if (policy instanceof PushPolicy) {
            type = PolicyType.PUSH;
        } else if (policy instanceof AuthPolicy) {
            type = PolicyType.AUTH;
        } else if (policy instanceof AccessPolicy) {
            type = PolicyType.ACCESS;
        } else if (policy instanceof AttrReleasePolicy) {
            type = PolicyType.ATTR_RELEASE;
        } else {
            throw new IllegalArgumentException("Invalid policy: " + policy);
        }

        return getInstance(type);
    }

    @Override
    public PolicyUtils getInstance(final Class<? extends PolicyTO> policyClass) {
        PolicyType type;
        if (policyClass == AccountPolicyTO.class) {
            type = PolicyType.ACCOUNT;
        } else if (policyClass == PasswordPolicyTO.class) {
            type = PolicyType.PASSWORD;
        } else if (policyClass == PropagationPolicyTO.class) {
            type = PolicyType.PROPAGATION;
        } else if (policyClass == PullPolicyTO.class) {
            type = PolicyType.PULL;
        } else if (policyClass == PushPolicyTO.class) {
            type = PolicyType.PUSH;
        } else if (policyClass == AuthPolicyTO.class) {
            type = PolicyType.AUTH;
        } else if (policyClass == AccessPolicyTO.class) {
            type = PolicyType.ACCESS;
        } else if (policyClass == AttrReleasePolicyTO.class) {
            type = PolicyType.ATTR_RELEASE;
        } else {
            throw new IllegalArgumentException("Invalid PolicyTO class: " + policyClass.getName());
        }

        return getInstance(type);
    }

    @Override
    public PolicyUtils getInstance(final PolicyTO policyTO) {
        return getInstance(policyTO.getClass());
    }
}
