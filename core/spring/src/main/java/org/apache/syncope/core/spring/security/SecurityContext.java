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

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.jws.AccessTokenJwsSignatureProvider;
import org.apache.syncope.core.spring.security.jws.AccessTokenJwsSignatureVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

@PropertySource("classpath:security.properties")
@PropertySource(value = "file:${conf.directory}/security.properties", ignoreResourceNotFound = true)
@Configuration
public class SecurityContext implements EnvironmentAware {

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    @Bean
    public String adminUser() {
        return env.getProperty("adminUser");
    }

    @Bean
    public String adminPassword() {
        return env.getProperty("adminPassword");
    }

    @Bean
    public String adminPasswordAlgorithm() {
        return env.getProperty("adminPasswordAlgorithm");
    }

    @Bean
    public String anonymousUser() {
        return env.getProperty("anonymousUser");
    }

    @Bean
    public String anonymousKey() {
        return env.getProperty("anonymousKey");
    }

    @Bean
    public String jwtIssuer() {
        return env.getProperty("jwtIssuer");
    }

    @Bean
    public String jwsKey() {
        return env.getProperty("jwsKey");
    }

    @ConditionalOnMissingBean
    @Bean
    public DefaultCredentialChecker credentialChecker() {
        return new DefaultCredentialChecker(jwsKey(), adminPassword(), anonymousKey());
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenJwsSignatureVerifier accessTokenJwsSignatureVerifier() {
        AccessTokenJwsSignatureVerifier verifier = new AccessTokenJwsSignatureVerifier();
        verifier.setJwsAlgorithm(env.getProperty("jwsAlgorithm", SignatureAlgorithm.class));
        verifier.setJwsKey(jwsKey());
        return verifier;
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenJwsSignatureProvider accessTokenJwsSignatureProvider() {
        AccessTokenJwsSignatureProvider provider = new AccessTokenJwsSignatureProvider();
        provider.setJwsAlgorithm(env.getProperty("jwsAlgorithm", SignatureAlgorithm.class));
        provider.setJwsKey(jwsKey());
        return provider;
    }

    @ConditionalOnMissingBean
    @Bean
    public PasswordGenerator passwordGenerator() {
        return new DefaultPasswordGenerator();
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
