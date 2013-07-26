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
package org.apache.syncope.core.policy;

import java.util.List;
import org.apache.syncope.common.types.AbstractPolicySpec;
import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PolicyEvaluator {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PolicyEvaluator.class);

    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicySpec> T evaluate(final Policy policy, final AbstractAttributable attributable) {
        if (policy == null) {
            return null;
        }

        T result = null;
        switch (policy.getType()) {
            case PASSWORD:
            case GLOBAL_PASSWORD:
                final PasswordPolicySpec ppSpec = policy.getSpecification();
                final PasswordPolicySpec evaluatedPPSpec = new PasswordPolicySpec();

                BeanUtils.copyProperties(ppSpec, evaluatedPPSpec, new String[]{"schemasNotPermitted"});

                for (String schema : ppSpec.getSchemasNotPermitted()) {
                    AbstractAttr attribute = attributable.getAttribute(schema);
                    if (attribute != null) {
                        List<String> values = attribute.getValuesAsStrings();
                        if (values != null && !values.isEmpty()) {
                            evaluatedPPSpec.getWordsNotPermitted().add(values.get(0));
                        }
                    }
                }

                // Password history verification and update

                if (!(attributable instanceof SyncopeUser)) {
                    LOG.error("Cannot check previous passwords. attributable is not a user object: {}",
                            attributable.getClass().getName());
                    result = (T) evaluatedPPSpec;
                    break;
                }
                SyncopeUser user = (SyncopeUser) attributable;
                if (user.verifyPasswordHistory(user.getClearPassword(), ppSpec.getHistoryLength())) {
                    evaluatedPPSpec.getWordsNotPermitted().add(user.getClearPassword());
                }
                result = (T) evaluatedPPSpec;
                break;
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                final AccountPolicySpec spec = policy.getSpecification();
                final AccountPolicySpec accountPolicy = new AccountPolicySpec();

                BeanUtils.copyProperties(spec, accountPolicy, new String[]{"schemasNotPermitted"});

                for (String schema : spec.getSchemasNotPermitted()) {
                    AbstractAttr attribute = attributable.getAttribute(schema);
                    if (attribute != null) {
                        List<String> values = attribute.getValuesAsStrings();
                        if (values != null && !values.isEmpty()) {
                            accountPolicy.getWordsNotPermitted().add(values.get(0));
                        }
                    }
                }

                result = (T) accountPolicy;
                break;
            case SYNC:
            case GLOBAL_SYNC:
            default:
                result = null;
        }

        return result;
    }
}
