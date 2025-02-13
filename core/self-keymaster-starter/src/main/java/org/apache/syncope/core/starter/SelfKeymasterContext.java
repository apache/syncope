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
package org.apache.syncope.core.starter;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.rest.api.service.ConfParamService;
import org.apache.syncope.common.keymaster.rest.api.service.DomainService;
import org.apache.syncope.common.keymaster.rest.api.service.NetworkServiceService;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.keymaster.internal.InternalConfParamHelper;
import org.apache.syncope.core.keymaster.internal.SelfKeymasterInternalConfParamOps;
import org.apache.syncope.core.keymaster.internal.SelfKeymasterInternalDomainOps;
import org.apache.syncope.core.keymaster.internal.SelfKeymasterInternalServiceOps;
import org.apache.syncope.core.keymaster.rest.cxf.service.ConfParamServiceImpl;
import org.apache.syncope.core.keymaster.rest.cxf.service.DomainServiceImpl;
import org.apache.syncope.core.keymaster.rest.cxf.service.NetworkServiceServiceImpl;
import org.apache.syncope.core.keymaster.rest.security.SelfKeymasterUsernamePasswordAuthenticationProvider;
import org.apache.syncope.core.logic.ConfParamLogic;
import org.apache.syncope.core.logic.DomainLogic;
import org.apache.syncope.core.logic.NetworkServiceLogic;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.keymaster.ConfParamDAO;
import org.apache.syncope.core.persistence.api.dao.keymaster.DomainDAO;
import org.apache.syncope.core.persistence.api.dao.keymaster.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.rest.cxf.JavaDocUtils;
import org.apache.syncope.core.rest.cxf.RestServiceExceptionMapper;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.DefaultCredentialChecker;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.UsernamePasswordAuthenticationProvider;
import org.apache.syncope.core.spring.security.WebSecurityContext;
import org.apache.syncope.core.starter.SelfKeymasterContext.SelfKeymasterCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

@EnableConfigurationProperties(KeymasterProperties.class)
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(WebSecurityContext.class)
@Conditional(SelfKeymasterCondition.class)
public class SelfKeymasterContext {

    private static final Logger LOG = LoggerFactory.getLogger(SelfKeymasterContext.class);

    private static final Pattern HTTP = Pattern.compile("^http.+");

    static class SelfKeymasterCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            String keymasterAddress = context.getEnvironment().getProperty("keymaster.address");
            return new ConditionOutcome(
                    keymasterAddress != null && HTTP.matcher(keymasterAddress).matches(),
                    "Keymaster address not set for Self: " + keymasterAddress);
        }
    }

    @Bean
    public Server selfKeymasterContainer(
            final DomainHolder<?> domainHolder,
            final ConfParamService confParamService,
            final NetworkServiceService networkServiceService,
            final DomainService domainService,
            final JacksonJsonProvider jsonProvider,
            final GZIPInInterceptor gzipInInterceptor,
            final GZIPOutInterceptor gzipOutInterceptor,
            final JAXRSBeanValidationInInterceptor validationInInterceptor,
            final RestServiceExceptionMapper restServiceExceptionMapper,
            final Bus bus,
            final Environment env) {

        SpringJAXRSServerFactoryBean selfKeymasterContainer = new SpringJAXRSServerFactoryBean();
        selfKeymasterContainer.setBus(bus);
        selfKeymasterContainer.setAddress("/keymaster");
        selfKeymasterContainer.setStaticSubresourceResolution(true);

        selfKeymasterContainer.setProperties(Map.of("convert.wadl.resources.to.dom", "false"));

        selfKeymasterContainer.setServiceBeans(List.of(confParamService, networkServiceService, domainService));

        selfKeymasterContainer.setInInterceptors(List.of(gzipInInterceptor, validationInInterceptor));

        selfKeymasterContainer.setOutInterceptors(List.of(gzipOutInterceptor));

        selfKeymasterContainer.setProviders(List.of(restServiceExceptionMapper, jsonProvider));

        // OpenAPI
        JavaDocProvider javaDocProvider = JavaDocUtils.getJavaDocURLs().
                map(JavaDocProvider::new).
                orElseGet(() -> JavaDocUtils.getJavaDocPaths(env).
                map(javaDocPaths -> {
                    try {
                        return new JavaDocProvider(javaDocPaths);
                    } catch (Exception e) {
                        LOG.error("Could not set javadoc paths from {}", List.of(javaDocPaths), e);
                        return null;
                    }
                }).
                orElse(null));
        OpenApiCustomizer openApiCustomizer = new OpenApiCustomizer() {

            @Override
            public OpenAPIConfiguration customize(final OpenAPIConfiguration configuration) {
                super.customize(configuration);

                MessageContext ctx = JAXRSUtils.createContextValue(
                        JAXRSUtils.getCurrentMessage(), null, MessageContext.class);

                String url = StringUtils.substringBeforeLast(ctx.getUriInfo().getRequestUri().getRawPath(), "/");
                configuration.getOpenAPI().setServers(List.of(new io.swagger.v3.oas.models.servers.Server().url(url)));

                return configuration;
            }

            @Override
            protected void addParameters(final List<Parameter> parameters) {
                Optional<Parameter> domainHeaderParameter = parameters.stream().
                        filter(p -> p instanceof HeaderParameter && RESTHeaders.DOMAIN.equals(p.getName())).findFirst();
                if (domainHeaderParameter.isEmpty()) {
                    HeaderParameter parameter = new HeaderParameter();
                    parameter.setName(RESTHeaders.DOMAIN);
                    parameter.setRequired(true);

                    ExternalDocumentation extDoc = new ExternalDocumentation();
                    extDoc.setDescription("Apache Syncope Reference Guide");
                    extDoc.setUrl("https://syncope.apache.org/docs/3.0/reference-guide.html#domains");

                    Schema<String> schema = new Schema<>();
                    schema.setDescription("Domains are built to facilitate multitenancy.");
                    schema.setExternalDocs(extDoc);
                    schema.setEnum(domainHolder.getDomains().keySet().stream().sorted().toList());
                    schema.setDefault(SyncopeConstants.MASTER_DOMAIN);
                    parameter.setSchema(schema);

                    parameters.add(parameter);
                }
            }
        };
        openApiCustomizer.setDynamicBasePath(false);
        openApiCustomizer.setReplaceTags(false);
        openApiCustomizer.setJavadocProvider(javaDocProvider);

        String version = env.getProperty("version");
        OpenApiFeature openapiFeature = new OpenApiFeature();
        openapiFeature.setUseContextBasedConfig(true);
        openapiFeature.setTitle("Apache Syncope Self Keymaster");
        openapiFeature.setVersion(version);
        openapiFeature.setDescription("Apache Syncope Self Keymaster" + version);
        openapiFeature.setContactName("The Apache Syncope community");
        openapiFeature.setContactEmail("dev@syncope.apache.org");
        openapiFeature.setContactUrl("https://syncope.apache.org");
        openapiFeature.setScan(false);
        openapiFeature.setResourcePackages(Set.of("org.apache.syncope.common.keymaster.rest.api.service"));
        openapiFeature.setSecurityDefinitions(
                Map.of("BasicAuthentication", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
        openapiFeature.setCustomizer(openApiCustomizer);
        selfKeymasterContainer.setFeatures(List.of(openapiFeature));

        return selfKeymasterContainer.create();
    }

    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider(
            final DomainOps domainOps,
            final AuthDataAccessor dataAccessor,
            final UserProvisioningManager provisioningManager,
            final DefaultCredentialChecker credentialChecker,
            final SecurityProperties securityProperties,
            final KeymasterProperties keymasterProperties,
            final EncryptorManager encryptorManager) {

        return new SelfKeymasterUsernamePasswordAuthenticationProvider(
                domainOps,
                dataAccessor,
                provisioningManager,
                credentialChecker,
                securityProperties,
                keymasterProperties,
                encryptorManager);
    }

    @Bean
    public InternalConfParamHelper internalConfParamHelper(
            final ConfParamDAO confParamDAO,
            final EntityFactory entityFactory) {

        return new InternalConfParamHelper(confParamDAO, entityFactory);
    }

    @Bean
    public ConfParamOps internalConfParamOps(final InternalConfParamHelper helper) {
        return new SelfKeymasterInternalConfParamOps(helper);
    }

    @Bean
    public ServiceOps internalServiceOps(
            final NetworkServiceLogic networkServiceLogic,
            final KeymasterProperties props) {

        return new SelfKeymasterInternalServiceOps(networkServiceLogic, props);
    }

    @Bean
    public DomainOps domainOps(final DomainLogic domainLogic, final KeymasterProperties props) {
        return new SelfKeymasterInternalDomainOps(domainLogic, props);
    }

    @Bean
    public ConfParamLogic confParamLogic(final InternalConfParamHelper helper) {
        return new ConfParamLogic(helper);
    }

    @Bean
    public DomainLogic domainLogic(
            final DomainDAO domainDAO,
            final EntityFactory selfKeymasterEntityFactory,
            final DomainWatcher domainWatcher) {

        return new DomainLogic(domainDAO, selfKeymasterEntityFactory, domainWatcher);
    }

    @Bean
    public NetworkServiceLogic networkServiceLogic(
            final NetworkServiceDAO serviceDAO,
            final EntityFactory entityFactory) {

        return new NetworkServiceLogic(serviceDAO, entityFactory);
    }

    @Bean
    public ConfParamService confParamService(final ConfParamLogic confParamLogic) {
        return new ConfParamServiceImpl(confParamLogic);
    }

    @Bean
    public DomainService domainService(final DomainLogic domainLogic) {
        return new DomainServiceImpl(domainLogic);
    }

    @Bean
    public NetworkServiceService networkServiceService(final NetworkServiceLogic networkServiceLogic) {
        return new NetworkServiceServiceImpl(networkServiceLogic);
    }
}
