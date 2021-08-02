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
import com.nimbusds.jose.KeyLengthException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSSigner;
import org.apache.syncope.core.spring.security.jws.AccessTokenJWSVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

@EnableConfigurationProperties(SecurityProperties.class)
@Configuration
public class SecurityContext {

    @Autowired
    private SecurityProperties props;

    @Bean
    public String adminUser() {
        return props.getAdminUser();
    }

    @Bean
    public String adminPassword() {
        return props.getAdminPassword();
    }

    @Bean
    public CipherAlgorithm adminPasswordAlgorithm() {
        return props.getAdminPasswordAlgorithm();
    }

    @Bean
    public String anonymousUser() {
        return props.getAnonymousUser();
    }

    @Bean
    public String anonymousKey() {
        return props.getAnonymousKey();
    }

    @Bean
    public String jwtIssuer() {
        return props.getJwtIssuer();
    }

    @Bean
    public String jwsKey() {
        return props.getJwsKey();
    }

    @ConditionalOnMissingBean
    @Bean
    public DefaultCredentialChecker credentialChecker() {
        return new DefaultCredentialChecker(jwsKey(), adminPassword(), anonymousKey());
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenJWSVerifier accessTokenJWSVerifier()
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        return new AccessTokenJWSVerifier(props.getJwsAlgorithm(), jwsKey());
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenJWSSigner accessTokenJWSSigner()
            throws KeyLengthException, NoSuchAlgorithmException, InvalidKeySpecException {

        return new AccessTokenJWSSigner(props.getJwsAlgorithm(), jwsKey());
    }

    @Bean
    public PasswordGenerator passwordGenerator() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return props.getPasswordGenerator().getDeclaredConstructor().newInstance();
    }

    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(""); // Remove the ROLE_ prefix
    }

    @Bean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }
}
