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
package org.apache.syncope.core.rest.cxf;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultReaderConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

/**
 * Automatically loads available javadocs from class loader (when {@link java.net.URLClassLoader}).
 */
public class Swagger2Feature extends org.apache.cxf.jaxrs.swagger.Swagger2Feature {

    @Override
    public void initialize(final Server server, final Bus bus) {
        URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
        if (javaDocURLs != null) {
            super.setJavaDocURLs(javaDocURLs);
        }

        super.initialize(server, bus);
    }

    // ------- Remove the code below this point when CXF 3.1.8 is available -----
    private Swagger2Serializers swagger2Serializers;

    public void setSwagger2Serializers(final Swagger2Serializers swagger2Serializers) {
        this.swagger2Serializers = swagger2Serializers;
    }

    @Override
    protected void addSwaggerResource(final Server server, final Bus bus) {
        List<Object> swaggerResources = new LinkedList<>();
        ApiListingResource apiListingResource = new ApiListingResource();
        swaggerResources.add(apiListingResource);
        if (SWAGGER_UI_RESOURCE_ROOT != null) {
            swaggerResources.add(new SwaggerUIService());
            bus.setProperty("swagger.service.ui.available", "true");
        }
        JAXRSServiceFactoryBean sfb =
                (JAXRSServiceFactoryBean) server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
        sfb.setResourceClassesFromBeans(swaggerResources);

        List<ClassResourceInfo> cris = sfb.getClassResourceInfo();

        List<Object> providers = new ArrayList<>();
        for (ClassResourceInfo cri : cris) {
            if (ApiListingResource.class == cri.getResourceClass()) {
                InjectionUtils.injectContextProxies(cri, apiListingResource);
            }
        }
        if (SWAGGER_UI_RESOURCE_ROOT != null) {
            providers.add(new SwaggerUIFilter());
        }

        if (swagger2Serializers != null) {
            swagger2Serializers.setJavadocProvider(javadocProvider);
            swagger2Serializers.setClassResourceInfos(cris);
            providers.add(swagger2Serializers);
        }

        providers.add(new ReaderConfigFilter());
        ((ServerProviderFactory) server.getEndpoint().get(
                ServerProviderFactory.class.getName())).setUserProviders(providers);

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(getResourcePackage());
        beanConfig.setVersion(getVersion());
        String basePath = getBasePath();
        beanConfig.setBasePath(basePath);
        beanConfig.setHost(getHost());
        beanConfig.setSchemes(getSchemes());
        beanConfig.setTitle(getTitle());
        beanConfig.setDescription(getDescription());
        beanConfig.setContact(getContact());
        beanConfig.setLicense(getLicense());
        beanConfig.setLicenseUrl(getLicenseUrl());
        beanConfig.setTermsOfServiceUrl(getTermsOfServiceUrl());
        beanConfig.setScan(isScan());
        beanConfig.setPrettyPrint(isPrettyPrint());
        beanConfig.setFilterClass(getFilterClass());
    }

    protected class ReaderConfigFilter implements ContainerRequestFilter {

        @Context
        protected MessageContext mc;

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            ServletContext servletContext = mc.getServletContext();
            if (servletContext != null && servletContext.getAttribute(ReaderConfig.class.getName()) == null) {
                if (mc.getServletConfig() != null
                        && Boolean.valueOf(mc.getServletConfig().getInitParameter("scan.all.resources"))) {
                    addReaderConfig(mc.getServletConfig().getInitParameter("ignore.routes"));
                } else if (isScanAllResources()) {
                    addReaderConfig(getIgnoreRoutes());
                }
            }
        }

        protected void addReaderConfig(final String ignoreRoutesParam) {
            DefaultReaderConfig rc = new DefaultReaderConfig();
            rc.setScanAllResources(true);
            if (ignoreRoutesParam != null) {
                Set<String> routes = new LinkedHashSet<>();
                for (String route : StringUtils.split(ignoreRoutesParam, ",")) {
                    routes.add(route.trim());
                }
                rc.setIgnoredRoutes(routes);
            }
            mc.getServletContext().setAttribute(ReaderConfig.class.getName(), rc);
        }
    }

    @PreMatching
    protected static class SwaggerUIFilter implements ContainerRequestFilter {

        private static final Pattern PATTERN =
                Pattern.compile(".*[.]js|/css/.*|/images/.*|/lib/.*|.*ico|/fonts/.*");

        @Override
        public void filter(final ContainerRequestContext rc) throws IOException {
            if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
                UriInfo ui = rc.getUriInfo();
                String path = "/" + ui.getPath();
                if (PATTERN.matcher(path).matches()) {
                    rc.setRequestUri(URI.create("api-docs" + path));
                }
            }
        }
    }

}
