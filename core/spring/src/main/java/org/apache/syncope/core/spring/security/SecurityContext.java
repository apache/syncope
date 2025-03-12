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
package org.apache.syncope.core.spring.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeyLengthException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.spring.policy.DefaultRuleProvider;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

@EnableConfigurationProperties(SecurityProperties.class)
@Configuration(proxyBeanMethods = false)
public class SecurityContext {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityContext.class);

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    public static GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(""); // Remove the ROLE_ prefix
    }

    protected static String jwsKey(final JWSAlgorithm jwsAlgorithm, final SecurityProperties props) {
        String jwsKey = props.getJwsKey();
        if (jwsKey == null) {
            throw new IllegalArgumentException("No JWS key provided");
        }

        if (JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)) {
            int minLength = jwsAlgorithm.equals(JWSAlgorithm.HS256)
                    ? 256 / 8
                    : jwsAlgorithm.equals(JWSAlgorithm.HS384)
                    ? 384 / 8
                    : 512 / 8;
            if (jwsKey.length() < minLength) {
                jwsKey = SecureRandomUtils.generateRandomPassword(minLength);
                props.setJwsKey(jwsKey);
                LOG.warn("The configured key for {} must be at least {} bits, generating random: {}",
                        jwsAlgorithm, minLength * 8, jwsKey);
            }
        }

        return jwsKey;
    }

    @Bean
    public CipherAlgorithm adminPasswordAlgorithm(final SecurityProperties props) {
        return props.getAdminPasswordAlgorithm();
    }

    @Bean
    public JWSAlgorithm jwsAlgorithm(final SecurityProperties props) {
        return JWSAlgorithm.parse(props.getJwsAlgorithm().toUpperCase());
    }

    @ConditionalOnMissingBean
    @Bean
    public DefaultCredentialChecker credentialChecker(
            final SecurityProperties props,
            final JWSAlgorithm jwsAlgorithm) {

        return new DefaultCredentialChecker(
                jwsKey(jwsAlgorithm, props),
                props.getAdminPassword(),
                props.getAnonymousKey());
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenJWSVerifier accessTokenJWSVerifier(
            final JWSAlgorithm jwsAlgorithm,
            final SecurityProperties props)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        return new AccessTokenJWSVerifier(jwsAlgorithm, jwsKey(jwsAlgorithm, props));
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenJWSSigner accessTokenJWSSigner(
            final JWSAlgorithm jwsAlgorithm,
            final SecurityProperties props)
            throws KeyLengthException, NoSuchAlgorithmException, InvalidKeySpecException {

        return new AccessTokenJWSSigner(jwsAlgorithm, jwsKey(jwsAlgorithm, props));
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeJWTSSOProvider syncopeJWTSSOProvider(
            final SecurityProperties props,
            final EncryptorManager encryptorManager,
            final AccessTokenJWSVerifier accessTokenJWSVerifier,
            final UserDAO userDAO,
            final AccessTokenDAO accessTokenDAO) {

        return new SyncopeJWTSSOProvider(props, encryptorManager, accessTokenJWSVerifier, userDAO, accessTokenDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public PasswordGenerator passwordGenerator() {
        return new DefaultPasswordGenerator();
    }

    @ConditionalOnMissingBean
    @Bean
    public RuleProvider ruleProvider(final RealmSearchDAO realmSearchDAO) {
        return new DefaultRuleProvider(realmSearchDAO);
    }

    @Bean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }

    @Bean
    public EncryptorManager encryptorManager() {
        return new DefaultEncryptorManager();
    }
}
