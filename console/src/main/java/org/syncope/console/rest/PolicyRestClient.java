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
package org.syncope.console.rest;

import org.springframework.stereotype.Component;
import org.syncope.client.mod.PasswordPolicyMod;
import org.syncope.client.to.PasswordPolicyTO;

/**
 * Console client for invoking Rest Policy services.
 */
@Component
public class PolicyRestClient extends AbstractBaseRestClient {

    public PasswordPolicyTO getPasswordPolicy() {
        try {
            return restTemplate.getForObject(
                    baseURL + "policy/password/read", PasswordPolicyTO.class);
        } catch (Exception e) {
            LOG.debug("No password policy found", e);
            return new PasswordPolicyTO();
        }
    }

    public PasswordPolicyTO createPasswordPolicy(
            final PasswordPolicyTO policy) {
        return restTemplate.postForObject(baseURL + "policy/password/create",
                policy, PasswordPolicyTO.class);
    }

    public PasswordPolicyTO updatePasswordPolicy(
            final PasswordPolicyMod policy) {

        return restTemplate.postForObject(baseURL + "policy/password/update",
                policy, PasswordPolicyTO.class);
    }
}
