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
package org.apache.syncope.core.misc.policy;

import java.util.List;
import org.apache.syncope.common.lib.types.AbstractPolicySpec;
import org.apache.syncope.common.lib.types.AccountPolicySpec;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class PolicyEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyEvaluator.class);

    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicySpec> T evaluate(final Policy policy, final Any<?, ?, ?> any) {
        T result;
        switch (policy.getType()) {
            case PASSWORD:
                PasswordPolicySpec ppSpec = ((PasswordPolicy) policy).getSpecification();
                PasswordPolicySpec evaluatedPPSpec = new PasswordPolicySpec();

                BeanUtils.copyProperties(ppSpec, evaluatedPPSpec, new String[] { "schemasNotPermitted" });

                for (String schema : ppSpec.getSchemasNotPermitted()) {
                    PlainAttr<?> attr = any.getPlainAttr(schema);
                    if (attr != null) {
                        List<String> values = attr.getValuesAsStrings();
                        if (values != null && !values.isEmpty()) {
                            evaluatedPPSpec.getWordsNotPermitted().add(values.get(0));
                        }
                    }
                }

                // Password history verification and update
                if (!(any instanceof User)) {
                    LOG.error("Cannot check previous passwords. instance is not user object: {}",
                            any.getClass().getName());
                    result = (T) evaluatedPPSpec;
                    break;
                }
                User user = (User) any;
                if (user.verifyPasswordHistory(user.getClearPassword(), ppSpec.getHistoryLength())) {
                    evaluatedPPSpec.getWordsNotPermitted().add(user.getClearPassword());
                }
                result = (T) evaluatedPPSpec;
                break;

            case ACCOUNT:
                AccountPolicySpec spec = ((AccountPolicy) policy).getSpecification();
                AccountPolicySpec accountPolicy = new AccountPolicySpec();

                BeanUtils.copyProperties(spec, accountPolicy, new String[] { "schemasNotPermitted" });

                for (String schema : spec.getSchemasNotPermitted()) {
                    PlainAttr<?> attr = any.getPlainAttr(schema);
                    if (attr != null) {
                        List<String> values = attr.getValuesAsStrings();
                        if (values != null && !values.isEmpty()) {
                            accountPolicy.getWordsNotPermitted().add(values.get(0));
                        }
                    }
                }

                result = (T) accountPolicy;
                break;

            default:
                result = null;
        }

        return result;
    }
}
