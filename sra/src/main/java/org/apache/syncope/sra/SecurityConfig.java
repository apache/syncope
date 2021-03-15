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
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.sra.security.CsrfRouteMatcher;
import org.apache.syncope.sra.security.LogoutRouteMatcher;
import org.apache.syncope.sra.security.oauth2.OAuth2SecurityConfigUtils;
import org.apache.syncope.sra.security.PublicRouteMatcher;
import org.apache.syncope.sra.security.cas.CASSecurityConfigUtils;
import org.apache.syncope.sra.security.pac4j.NoOpLogoutHandler;
import org.apache.syncope.sra.security.saml2.SAML2MetadataEndpoint;
import org.apache.syncope.sra.security.saml2.SAML2SecurityConfigUtils;
import org.apache.syncope.sra.security.saml2.SAML2WebSsoAuthenticationWebFilter;
import org.jasig.cas.client.Protocol;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
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
@Configuration
public class SecurityConfig {

    public static final String AM_TYPE = "am.type";

    public enum AMType {
        OIDC,
        OAUTH2,
        SAML2,
        CAS

    }

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private Environment env;

    @Bean
    @Order(0)
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "SAML2")
    public SecurityWebFilterChain saml2SecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher metadataMatcher =
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, SAML2MetadataEndpoint.METADATA_URL);
        return http.securityMatcher(metadataMatcher).
                authorizeExchange().anyExchange().permitAll().
                and().csrf().requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(metadataMatcher)).
                and().build();
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain actuatorSecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher actuatorMatcher = EndpointRequest.toAnyEndpoint();
        return http.securityMatcher(actuatorMatcher).
                authorizeExchange().anyExchange().authenticated().
                and().httpBasic().
                and().csrf().requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(actuatorMatcher)).
                and().build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder().
                username(Objects.requireNonNull(env.getProperty("anonymousUser"))).
                password("{noop}" + env.getProperty("anonymousKey")).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OIDC")
    public InMemoryReactiveClientRegistrationRepository oidcClientRegistrationRepository() {
        return new InMemoryReactiveClientRegistrationRepository(
                ClientRegistrations.fromOidcIssuerLocation(env.getProperty("am.oidc.configuration")).
                        registrationId("OIDC").
                        clientId(env.getProperty("am.oidc.client.id")).
                        clientSecret(env.getProperty("am.oidc.client.secret")).
                        scope(env.getProperty("am.oidc.scopes", String[].class,
                                new String[] { OidcScopes.OPENID, OidcScopes.ADDRESS, OidcScopes.EMAIL,
                                    OidcScopes.PHONE, OidcScopes.PROFILE })).
                        build());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OIDC")
    public OAuth2TokenValidator<Jwt> oidcJWTValidator() {
        return JwtValidators.createDefaultWithIssuer(env.getProperty("am.oidc.configuration"));
    }

    @Bean
    @ConditionalOnMissingBean
    public Converter<Map<String, Object>, Map<String, Object>> jwtClaimSetConverter() {
        return MappedJwtClaimSetConverter.withDefaults(Map.of());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OIDC")
    public ReactiveJwtDecoder oidcJWTDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(
                oidcClientRegistrationRepository().iterator().next().getProviderDetails().getJwkSetUri()).build();
        jwtDecoder.setJwtValidator(oidcJWTValidator());
        jwtDecoder.setClaimSetConverter(jwtClaimSetConverter());
        return jwtDecoder;
    }

    @Bean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OAUTH2")
    public InMemoryReactiveClientRegistrationRepository oauth2ClientRegistrationRepository() {
        return new InMemoryReactiveClientRegistrationRepository(
                ClientRegistration.withRegistrationId("OAUTH2").
                        redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}").
                        tokenUri(env.getProperty("am.oauth2.tokenUri")).
                        authorizationUri(env.getProperty("am.oauth2.authorizationUri")).
                        userInfoUri(env.getProperty("am.oauth2.userInfoUri")).
                        userNameAttributeName(env.getProperty("am.oauth2.userNameAttributeName")).
                        clientId(env.getProperty("am.oauth2.client.id")).
                        clientSecret(env.getProperty("am.oauth2.client.secret")).
                        scope(env.getProperty("am.oauth2.scopes", String[].class)).
                        authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).
                        jwkSetUri(env.getProperty("am.oauth2.jwkSetUri")).
                        build());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OAUTH2")
    public OAuth2TokenValidator<Jwt> oauth2JWTValidator() {
        String issuer = env.getProperty("am.oauth2.issuer");
        return issuer == null
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(env.getProperty("am.oauth2.issuer"));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "OAUTH2")
    public ReactiveJwtDecoder oauth2JWTDecoder() {
        String jwkSetUri = oauth2ClientRegistrationRepository().iterator().next().getProviderDetails().getJwkSetUri();
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
        jwtDecoder.setJwtValidator(oauth2JWTValidator());
        jwtDecoder.setClaimSetConverter(jwtClaimSetConverter());
        return jwtDecoder;
    }

    @Bean
    @ConditionalOnProperty(name = AM_TYPE, havingValue = "SAML2")
    public SAML2Client saml2Client() {
        SAML2Configuration cfg = new SAML2Configuration(
                resourceResolver.getResource(env.getProperty("am.saml2.keystore")),
                env.getProperty("am.saml2.keystore.storepass"),
                env.getProperty("am.saml2.keystore.keypass"),
                resourceResolver.getResource(env.getProperty("am.saml2.idp")));

        cfg.setKeystoreType(env.getProperty("am.saml2.keystore.type"));
        if (cfg.getKeystoreResource() instanceof FileUrlResource) {
            cfg.setKeystoreGenerator(new BaseSAML2KeystoreGenerator(cfg) {

                @Override
                protected void store(
                        final KeyStore ks,
                        final X509Certificate certificate,
                        final PrivateKey privateKey) throws Exception {

                    // nothing to do
                }

                @Override
                public InputStream retrieve() throws Exception {
                    return cfg.getKeystoreResource().getInputStream();
                }
            });
        }

        cfg.setAuthnRequestBindingType(
                SAML2BindingType.valueOf(env.getProperty("am.saml2.sp.authnrequest.binding")).getUri());
        cfg.setResponseBindingType(SAML2BindingType.POST.getUri());
        cfg.setSpLogoutRequestBindingType(
                SAML2BindingType.valueOf(env.getProperty("am.saml2.sp.logout.request.binding")).getUri());
        cfg.setSpLogoutResponseBindingType(
                SAML2BindingType.valueOf(env.getProperty("am.saml2.sp.logout.response.binding")).getUri());

        cfg.setServiceProviderEntityId(env.getProperty("am.saml2.sp.entityId"));

        cfg.setWantsAssertionsSigned(true);
        cfg.setAuthnRequestSigned(true);
        cfg.setSpLogoutRequestSigned(true);
        cfg.setServiceProviderMetadataResourceFilepath(env.getProperty("am.saml2.sp.metadata"));
        cfg.setAcceptedSkew(env.getProperty("am.saml2.sp.skew", int.class));

        cfg.setLogoutHandler(new NoOpLogoutHandler());

        SAML2Client saml2Client = new SAML2Client(cfg);
        saml2Client.setName(AMType.SAML2.name());
        saml2Client.setCallbackUrl(env.getProperty("am.saml2.sp.entityId")
                + SAML2WebSsoAuthenticationWebFilter.FILTER_PROCESSES_URI);
        saml2Client.setCallbackUrlResolver(new NoParameterCallbackUrlResolver());
        saml2Client.init();

        return saml2Client;
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = AM_TYPE)
    public SecurityWebFilterChain routesSecurityFilterChain(
            final ServerHttpSecurity http,
            final CacheManager cacheManager,
            final LogoutRouteMatcher logoutRouteMatcher,
            final PublicRouteMatcher publicRouteMatcher,
            final CsrfRouteMatcher csrfRouteMatcher,
            final ConfigurableApplicationContext ctx) {

        AMType amType = AMType.valueOf(env.getProperty(AM_TYPE));

        ServerHttpSecurity.AuthorizeExchangeSpec builder = http.authorizeExchange().
                matchers(publicRouteMatcher).permitAll().
                anyExchange().authenticated();

        switch (amType) {
            case OIDC:
            case OAUTH2:
                OAuth2SecurityConfigUtils.forLogin(http, amType, ctx);
                OAuth2SecurityConfigUtils.forLogout(builder, amType, cacheManager, logoutRouteMatcher, ctx);
                http.oauth2ResourceServer().jwt().jwtDecoder(ctx.getBean(ReactiveJwtDecoder.class));
                break;

            case SAML2:
                SAML2Client saml2Client = saml2Client();
                SAML2SecurityConfigUtils.forLogin(http, saml2Client, publicRouteMatcher);
                SAML2SecurityConfigUtils.forLogout(builder, saml2Client, cacheManager, logoutRouteMatcher, ctx);
                break;

            case CAS:
                CASSecurityConfigUtils.forLogin(
                        http,
                        env.getProperty("am.cas.server.name"),
                        Protocol.CAS3,
                        env.getProperty("am.cas.url.prefix"),
                        publicRouteMatcher);
                CASSecurityConfigUtils.forLogout(
                        builder,
                        cacheManager,
                        env.getProperty("am.cas.url.prefix"),
                        logoutRouteMatcher,
                        ctx);
                break;

            default:
        }

        return builder.and().csrf().requireCsrfProtectionMatcher(csrfRouteMatcher).and().build();
    }
}
