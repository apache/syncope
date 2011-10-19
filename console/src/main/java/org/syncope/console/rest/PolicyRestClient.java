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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.PasswordPolicyMod;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;

/**
 * Console client for invoking Rest Policy services.
 */
@Component
public class PolicyRestClient extends AbstractBaseRestClient {

    public PasswordPolicyTO getGlobalPasswordPolicy() {
        try {
            return restTemplate.getForObject(
                    baseURL + "policy/password/global/read", PasswordPolicyTO.class);
        } catch (Exception e) {
            LOG.debug("No password policy found", e);
            return new PasswordPolicyTO();
        }
    }

    public List<PasswordPolicyTO> getPasswordPolicies() {
        final List<PasswordPolicyTO> policies =
                new ArrayList<PasswordPolicyTO>();

        PasswordPolicyTO[] passwordPolicies = null;

        try {

            passwordPolicies = restTemplate.getForObject(
                    baseURL + "policy/password/list",
                    PasswordPolicyTO[].class);
        } catch (Exception ignore) {
            LOG.debug("No password policy found", ignore);
        }

        if (passwordPolicies != null) {
            policies.addAll(Arrays.asList(passwordPolicies));
        }

        PasswordPolicyTO globalPasswordPolicy = null;

        try {
            globalPasswordPolicy =
                    restTemplate.getForObject(
                    baseURL + "policy/password/global/read",
                    PasswordPolicyTO.class);
        } catch (Exception ignore) {
            LOG.debug("No global password policy found", ignore);
        }

        if (globalPasswordPolicy != null) {
            policies.add(0, globalPasswordPolicy);
        }

        return policies;
    }

    public List<AccountPolicyTO> getAccountPolicies() {
        final List<AccountPolicyTO> policies =
                new ArrayList<AccountPolicyTO>();

        AccountPolicyTO[] accountPolicies = null;

        try {

            accountPolicies = restTemplate.getForObject(
                    baseURL + "policy/account/list",
                    AccountPolicyTO[].class);
        } catch (Exception ignore) {
            LOG.debug("No password policy found", ignore);
        }

        if (accountPolicies != null) {
            policies.addAll(Arrays.asList(accountPolicies));
        }

        AccountPolicyTO globalAccountPolicy = null;

        try {
            globalAccountPolicy =
                    restTemplate.getForObject(
                    baseURL + "policy/account/global/read",
                    AccountPolicyTO.class);
        } catch (Exception ignore) {
            LOG.debug("No global password policy found", ignore);
        }

        if (globalAccountPolicy != null) {
            policies.add(0, globalAccountPolicy);
        }

        return policies;
    }

    public AccountPolicyTO getGlobalAccountPolicy() {
        try {
            return restTemplate.getForObject(
                    baseURL + "policy/acount/global/read", AccountPolicyTO.class);
        } catch (Exception e) {
            LOG.debug("No account policy found", e);
            return new AccountPolicyTO();
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

    public void delete(final Long id) {
        restTemplate.delete(baseURL + "policy/delete/" + id);
    }
}
