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
package org.apache.syncope.core.spring.policy;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.HaveIBeenPwnedPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRuleConfClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@PasswordRuleConfClass(HaveIBeenPwnedPasswordRuleConf.class)
public class HaveIBeenPwnedPasswordRule implements PasswordRule {

    private static final Logger LOG = LoggerFactory.getLogger(HaveIBeenPwnedPasswordRule.class);

    @Autowired
    private EncryptorManager encryptorManager;

    private HaveIBeenPwnedPasswordRuleConf conf;

    @Override
    public HaveIBeenPwnedPasswordRuleConf getConf() {
        return conf;
    }

    @Override
    public void setConf(final PasswordRuleConf conf) {
        if (conf instanceof HaveIBeenPwnedPasswordRuleConf haveIBeenPwnedPasswordRuleConf) {
            this.conf = haveIBeenPwnedPasswordRuleConf;
        } else {
            throw new IllegalArgumentException(
                    HaveIBeenPwnedPasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    protected void enforce(final String clearPassword) {
        try {
            String sha1 = encryptorManager.getInstance().encode(clearPassword, CipherAlgorithm.SHA1);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Apache Syncope");
            ResponseEntity<String> response = new RestTemplate().exchange(
                    URI.create("https://api.pwnedpasswords.com/range/" + sha1.substring(0, 5)),
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    String.class);
            if (StringUtils.isNotBlank(response.getBody())) {
                if (Stream.of(response.getBody().split("\\n")).
                        anyMatch(line -> sha1.equals(sha1.substring(0, 5) + StringUtils.substringBefore(line, ":")))) {

                    throw new PasswordPolicyException("Password pwned");
                }
            }
        } catch (GeneralSecurityException e) {
            LOG.error("Could not encode the password value as SHA1", e);
        } catch (HttpStatusCodeException e) {
            LOG.error("Error while contacting the PwnedPasswords service", e);
        }
    }

    @Override
    public void enforce(final String username, final String clearPassword) {
        Optional.ofNullable(clearPassword).ifPresent(this::enforce);
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final User user, final String clearPassword) {
        Optional.ofNullable(clearPassword).ifPresent(this::enforce);
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final LinkedAccount account) {
        if (account.getPassword() != null) {
            String clearPassword = null;
            if (account.canDecodeSecrets()) {
                try {
                    clearPassword = encryptorManager.getInstance().
                            decode(account.getPassword(), account.getCipherAlgorithm());
                } catch (Exception e) {
                    LOG.error("Could not decode password for {}", account, e);
                }
            }

            if (clearPassword != null) {
                enforce(clearPassword);
            }
        }
    }
}
