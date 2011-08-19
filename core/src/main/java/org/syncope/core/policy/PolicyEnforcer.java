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

import java.io.InvalidObjectException;
import org.springframework.stereotype.Component;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.types.AbstractPolicy;
import org.syncope.types.PolicyType;
import org.syncope.types.SyntaxPolicy;

@Component
public class PolicyEnforcer<E> {

    public <T extends AbstractPolicy> void enforce(
            final T policy, final PolicyType type, final E object)
            throws InvalidObjectException, Exception {

        switch (type) {
            case PASSWORD:
            case SCHEMA:
                if (!(object instanceof String)) {
                    throw new InvalidObjectException("Invalid object type");
                }
                syntaxVerification((SyntaxPolicy) policy, (String) object);
                break;
            case ACCOUNT:
                if (!(object instanceof SyncopeUser)) {
                    throw new InvalidObjectException("Invalid object type");
                }
                break;
            default:
        }
    }

    private void syntaxVerification(
            final SyntaxPolicy policy, final String object) throws Exception {

        // check length
        if (policy.getMinLength() > 0
                && policy.getMinLength() > object.length()) {
            throw new Exception("Password too short");
        }

        if (policy.getMaxLength() > 0
                && policy.getMaxLength() < object.length()) {
            throw new Exception("Password too long");
        }
        // check words not permitted

        // check non alphanumeric characters occurence

        // check digits occurrence

        // check lowercase alphabetic characters occurrence

        // check uppercase alphabetic characters occurrence

        // check prefix

        // check suffix

        // check non alphanumeric character first occurrence

        // check digit first occurrence

        // check non alphanumeric character last occurrence

        // check digit last occurrence

        // check schemas
    }
}
