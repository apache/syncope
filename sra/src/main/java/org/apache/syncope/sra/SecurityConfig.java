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
package org.apache.syncope.sra;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.sra.security.CsrfRouteMatcher;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.apache.syncope.sra.security.cas.CASSecurityConfigUtils;
import org.apache.syncope.sra.security.oauth2.OAuth2SecurityConfigUtils;
import org.apache.syncope.sra.security.saml2.SAML2MetadataEndpoint;
import org.apache.syncope.sra.security.saml2.SAML2SecurityConfigUtils;
import org.apache.syncope.sra.security.saml2.SAML2WebSsoAuthenticationWebFilter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

@EnableWebFluxSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    @Order(0)
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "SAML2")
    public SecurityWebFilterChain saml2SecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher metadataMatcher =
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, SAML2MetadataEndpoint.METADATA_URL);
        http.securityMatcher(metadataMatcher);

        http.authorizeExchange(customizer -> customizer.anyExchange().permitAll());

        http.csrf(customizer -> customizer.
                requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(metadataMatcher)));

        return http.build();
    }

    @ConditionalOnMissingBean
    @Bean
    @Order(1)
    public SecurityWebFilterChain actuatorSecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher actuatorMatcher = EndpointRequest.toAnyEndpoint();
        http.securityMatcher(actuatorMatcher);

        http.authorizeExchange(customizer -> customizer.anyExchange().authenticated());

        http.httpBasic(Customizer.withDefaults());

        http.csrf(customizer -> customizer.
                requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(actuatorMatcher)));

        return http.build();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReactiveUserDetailsService actuatorUserDetailsService(final SRAProperties props) {
        UserDetails user = User.builder().
                username(props.getAnonymousUser()).
                password("{noop}" + props.getAnonymousKey()).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OIDC")
    public ClientRegistration oidcClientRegistration(final SRAProperties props) {
        return ClientRegistrations.fromOidcIssuerLocation(props.getOidc().getConfiguration()).
                registrationId(SRAProperties.AMType.OIDC.name()).
                clientId(props.getOidc().getClientId()).
                clientSecret(props.getOidc().getClientSecret()).
                scope(props.getOidc().getScopes().toArray(String[]::new)).
                build();
    }

    @Bean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OIDC")
    public ReactiveClientRegistrationRepository oidcClientRegistrationRepository(
            @Qualifier("oidcClientRegistration") final ClientRegistration oidcClientRegistration) {
        return new InMemoryReactiveClientRegistrationRepository(oidcClientRegistration);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OIDC")
    public OAuth2TokenValidator<Jwt> oidcJWTValidator(final SRAProperties props) {
        return JwtValidators.createDefaultWithIssuer(props.getOidc().getConfiguration());
    }

    @Bean
    @ConditionalOnMissingBean
    public Converter<Map<String, Object>, Map<String, Object>> jwtClaimSetConverter() {
        return MappedJwtClaimSetConverter.withDefaults(Map.of());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OIDC")
    public ReactiveJwtDecoder oidcJWTDecoder(
            @Qualifier("oidcClientRegistration")
            final ClientRegistration oidcClientRegistration,
            @Qualifier("oidcJWTValidator")
            final OAuth2TokenValidator<Jwt> oidcJWTValidator,
            @Qualifier("jwtClaimSetConverter")
            final Converter<Map<String, Object>, Map<String, Object>> jwtClaimSetConverter) {
        String jwkSetUri = oidcClientRegistration.getProviderDetails().getJwkSetUri();
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .jwsAlgorithm(SignatureAlgorithm.RS512)
                .build();
        jwtDecoder.setJwtValidator(oidcJWTValidator);
        jwtDecoder.setClaimSetConverter(jwtClaimSetConverter);
        return jwtDecoder;
    }

    @Bean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OAUTH2")
    public ClientRegistration oauth2ClientRegistration(final SRAProperties props) {
        return ClientRegistration.withRegistrationId(SRAProperties.AMType.OAUTH2.name()).
                redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}").
                tokenUri(props.getOauth2().getTokenUri()).
                authorizationUri(props.getOauth2().getAuthorizationUri()).
                userInfoUri(props.getOauth2().getUserInfoUri()).
                userNameAttributeName(props.getOauth2().getUserNameAttributeName()).
                clientId(props.getOauth2().getClientId()).
                clientSecret(props.getOauth2().getClientSecret()).
                scope(props.getOauth2().getScopes().toArray(String[]::new)).
                authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).
                jwkSetUri(props.getOauth2().getJwkSetUri()).
                build();
    }

    @Bean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OAUTH2")
    public ReactiveClientRegistrationRepository oauth2ClientRegistrationRepository(
            @Qualifier("oauth2ClientRegistration") final ClientRegistration oauth2ClientRegistration) {
        return new InMemoryReactiveClientRegistrationRepository(oauth2ClientRegistration);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OAUTH2")
    public OAuth2TokenValidator<Jwt> oauth2JWTValidator(final SRAProperties props) {
        return props.getOauth2().getIssuer() == null
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(props.getOauth2().getIssuer());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "OAUTH2")
    public ReactiveJwtDecoder oauth2JWTDecoder(
            @Qualifier("oauth2ClientRegistration")
            final ClientRegistration oauth2ClientRegistration,
            @Qualifier("oauth2JWTValidator")
            final OAuth2TokenValidator<Jwt> oauth2JWTValidator,
            @Qualifier("jwtClaimSetConverter")
            final Converter<Map<String, Object>, Map<String, Object>> jwtClaimSetConverter) {

        String jwkSetUri = oauth2ClientRegistration.getProviderDetails().getJwkSetUri();
        NimbusReactiveJwtDecoder jwtDecoder;
        if (StringUtils.isBlank(jwkSetUri)) {
            jwtDecoder = new NimbusReactiveJwtDecoder(jwt -> {
                try {
                    return Mono.just(jwt.getJWTClaimsSet());
                } catch (ParseException e) {
                    return Mono.error(e);
                }
            });
        } else {
            jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        jwtDecoder.setJwtValidator(oauth2JWTValidator);
        jwtDecoder.setClaimSetConverter(jwtClaimSetConverter);
        return jwtDecoder;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE, havingValue = "SAML2")
    public SAML2Client saml2Client(final ResourcePatternResolver resourceResolver, final SRAProperties props) {
        SAML2Configuration cfg = new SAML2Configuration(
                resourceResolver.getResource(props.getSaml2().getKeystore()),
                null,
                props.getSaml2().getKeystoreType(),
                props.getSaml2().getKeystoreStorePass(),
                props.getSaml2().getKeystoreKeypass(),
                resourceResolver.getResource(props.getSaml2().getIdpMetadata()));

        if (cfg.getKeystoreResource() instanceof FileUrlResource) {
            cfg.setKeystoreGenerator(new BaseSAML2KeystoreGenerator(cfg) {

                @Override
                protected void store(
                        final KeyStore ks,
                        final X509Certificate certificate,
                        final PrivateKey privateKey) {

                    // nothing to do
                }

                @Override
                public InputStream retrieve() throws Exception {
                    return cfg.getKeystoreResource().getInputStream();
                }
            });
        }

        cfg.setAuthnRequestBindingType(props.getSaml2().getAuthnRequestBinding().getUri());
        cfg.setResponseBindingType(SAML2BindingType.POST.getUri());
        cfg.setSpLogoutRequestBindingType(props.getSaml2().getLogoutRequestBinding().getUri());
        cfg.setSpLogoutResponseBindingType(props.getSaml2().getLogoutResponseBinding().getUri());

        cfg.setServiceProviderEntityId(props.getSaml2().getEntityId());

        cfg.setWantsAssertionsSigned(true);
        cfg.setAuthnRequestSigned(true);
        cfg.setSpLogoutRequestSigned(true);
        cfg.setServiceProviderMetadataResourceFilepath(props.getSaml2().getSpMetadataFilePath());
        cfg.setMaximumAuthenticationLifetime(props.getSaml2().getMaximumAuthenticationLifetime());
        cfg.setAcceptedSkew(props.getSaml2().getAcceptedSkew());

        SAML2Client saml2Client = new SAML2Client(cfg);
        saml2Client.setName(SRAProperties.AMType.SAML2.name());
        saml2Client.setCallbackUrl(props.getSaml2().getEntityId()
                + SAML2WebSsoAuthenticationWebFilter.FILTER_PROCESSES_URI);
        saml2Client.setCallbackUrlResolver(new NoParameterCallbackUrlResolver());
        saml2Client.init();

        return saml2Client;
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = SRAProperties.PREFIX, name = SRAProperties.AM_TYPE)
    public SecurityWebFilterChain routesSecurityFilterChain(
            @Qualifier("saml2Client") final ObjectProvider<SAML2Client> saml2Client,
            final SRAProperties props,
            final ServerHttpSecurity http,
            final CacheManager cacheManager,
            final LogoutRouteMatcher logoutRouteMatcher,
            final PublicRouteMatcher publicRouteMatcher,
            final CsrfRouteMatcher csrfRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        http.authorizeExchange(customizer -> customizer.
                matchers(publicRouteMatcher).permitAll().
                anyExchange().authenticated());

        switch (props.getAmType()) {
            case OIDC, OAUTH2 -> {
                OAuth2SecurityConfigUtils.forLogin(http, props.getAmType(), ctx);
                OAuth2SecurityConfigUtils.forLogout(http, props.getAmType(), cacheManager, logoutRouteMatcher, ctx);
                http.oauth2ResourceServer(customizer -> customizer.jwt(
                        c -> c.jwtDecoder(ctx.getBean(ReactiveJwtDecoder.class))));
            }

            case SAML2 ->
                saml2Client.ifAvailable(client -> {
                    SAML2SecurityConfigUtils.forLogin(http, client, publicRouteMatcher);
                    SAML2SecurityConfigUtils.forLogout(http, client, cacheManager, logoutRouteMatcher, ctx);
                });

            case CAS -> {
                CASSecurityConfigUtils.forLogin(
                        http,
                        props.getCas().getProtocol(),
                        props.getCas().getServerPrefix(),
                        publicRouteMatcher);
                CASSecurityConfigUtils.forLogout(
                        http,
                        cacheManager,
                        props.getCas().getServerPrefix(),
                        logoutRouteMatcher,
                        ctx);
            }

            default -> {
            }
        }

        http.csrf(customizer -> customizer.requireCsrfProtectionMatcher(csrfRouteMatcher));
        return http.build();
    }
}
