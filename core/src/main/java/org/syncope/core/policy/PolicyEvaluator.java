/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.policy;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.types.AbstractPolicy;
import org.syncope.types.PasswordPolicy;

@Component
public class PolicyEvaluator {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            PolicyEvaluator.class);

    public <T extends AbstractPolicy> T evaluate(
            final Policy policy, final AbstractAttributable attributable) {

        T result = null;

        if (policy != null) {
            switch (policy.getType()) {
                case GLOBAL_PASSWORD:
                    final PasswordPolicy spec = policy.getSpecification();
                    final PasswordPolicy passwordPolicy = new PasswordPolicy();

                    BeanUtils.copyProperties(
                            spec,
                            passwordPolicy,
                            new String[]{"schemasNotPermitted"});

                    AbstractAttr attribute;
                    List<String> values;
                    for (String schema : spec.getSchemasNotPermitted()) {
                        attribute = attributable.getAttribute(schema);
                        if (attribute != null) {
                            values = attribute.getValuesAsStrings();
                            if (values != null && !values.isEmpty()) {
                                passwordPolicy.getWordsNotPermitted().add(
                                        values.get(0));
                            }
                        }
                    }

                    // Password history verification and update
                    final String password =
                            ((SyncopeUser) attributable).getPassword();

                    final List<String> passwordHistory =
                            ((SyncopeUser) attributable).getPasswordHistory();

                    if (((SyncopeUser) attributable).verifyPasswordHistory(
                            ((SyncopeUser) attributable).getClearPassword(),
                            spec.getHistoryLength())) {
                        passwordPolicy.getWordsNotPermitted().add(
                                ((SyncopeUser) attributable).getClearPassword());
                    } else {

                        if (spec.getHistoryLength() > 0 && password != null) {
                            passwordHistory.add(password);
                        }

                        if (spec.getHistoryLength() < passwordHistory.size()) {
                            for (int i = 0; i < passwordHistory.size()
                                    - spec.getHistoryLength(); i++) {
                                passwordHistory.remove(i);
                            }
                        }
                    }

                    result = (T) passwordPolicy;
                    break;
                case PASSWORD:
                    break;
                case GLOBAL_ACCOUNT:
                    result = null;
                    break;
                case ACCOUNT:
                    break;
                case SYNC:
                    break;
                default:
                    result = null;
            }
        }

        return result;
    }
}
