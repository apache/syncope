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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.types.AbstractPolicy;
import org.syncope.types.SyntaxPolicy;

@Component
public class PolicyEvaluator {

    public <T extends AbstractPolicy> T evaluate(
            final Policy policy, final AbstractAttributable attributable) {

        T result = null;

        if (policy != null) {
            switch (policy.getType()) {
                case PASSWORD:
                case SCHEMA:
                    final SyntaxPolicy specification = policy.getSpecification();
                    final SyntaxPolicy syntaxPolicy = new SyntaxPolicy();

                    BeanUtils.copyProperties(
                            specification,
                            syntaxPolicy,
                            new String[]{"schemasNotPermitted"});

                    AbstractAttr attribute;
                    List<String> values;
                    for (String schema : specification.getSchemasNotPermitted()) {
                        attribute = attributable.getAttribute(schema);
                        if (attribute != null) {
                            values = attribute.getValuesAsStrings();
                            if (values != null && !values.isEmpty()) {
                                syntaxPolicy.getWordsNotPermitted().add(
                                        values.get(0));
                            }
                        }
                    }

                    result = (T) syntaxPolicy;
                    break;
                case ACCOUNT:
                    result = null;
                    break;
                default:
                    result = null;
            }
        }

        return result;
    }
}
