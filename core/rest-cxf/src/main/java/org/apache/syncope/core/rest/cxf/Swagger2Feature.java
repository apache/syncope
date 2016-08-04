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
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

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

    // ------- Remove the code below this point when CXF-6990 is part of next CXF release (3.1.8?) -----
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
        providers.add(new Swagger2Serializers(dynamicBasePath, replaceTags, javadocProvider, cris));
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

    protected static class Swagger2Serializers extends SwaggerSerializers {

        protected final boolean dynamicBasePath;

        protected final boolean replaceTags;

        protected final DocumentationProvider javadocProvider;

        protected final List<ClassResourceInfo> cris;

        public Swagger2Serializers(
                final boolean dynamicBasePath,
                final boolean replaceTags,
                final DocumentationProvider javadocProvider,
                final List<ClassResourceInfo> cris) {

            super();

            this.dynamicBasePath = dynamicBasePath;
            this.replaceTags = replaceTags;
            this.javadocProvider = javadocProvider;
            this.cris = cris;
        }

        @Override
        public void writeTo(
                final Swagger data,
                final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType,
                final MultivaluedMap<String, Object> headers,
                final OutputStream out) throws IOException {

            if (dynamicBasePath) {
                MessageContext ctx = JAXRSUtils.createContextValue(
                        JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
                data.setBasePath(StringUtils.substringBeforeLast(ctx.getHttpServletRequest().
                        getRequestURI(), "/"));
            }

            if (replaceTags || javadocProvider != null) {
                Map<String, ClassResourceInfo> operations = new HashMap<>();
                Map<Pair<String, String>, OperationResourceInfo> methods = new HashMap<>();
                for (ClassResourceInfo cri : cris) {
                    for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
                        String normalizedPath = getNormalizedPath(
                                cri.getURITemplate().getValue(), ori.getURITemplate().getValue());

                        operations.put(normalizedPath, cri);
                        methods.put(ImmutablePair.of(ori.getHttpMethod(), normalizedPath), ori);
                    }
                }

                if (replaceTags && data.getTags() != null) {
                    data.getTags().clear();
                }
                for (final Map.Entry<String, Path> entry : data.getPaths().entrySet()) {
                    Tag tag = null;
                    if (replaceTags && operations.containsKey(entry.getKey())) {
                        ClassResourceInfo cri = operations.get(entry.getKey());

                        tag = new Tag();
                        tag.setName(cri.getURITemplate().getValue());
                        if (javadocProvider != null) {
                            tag.setDescription(javadocProvider.getClassDoc(cri));
                        }

                        data.addTag(tag);
                    }

                    for (Map.Entry<io.swagger.models.HttpMethod, Operation> subentry : entry.getValue().
                            getOperationMap().entrySet()) {
                        if (replaceTags && tag != null) {
                            subentry.getValue().setTags(Collections.singletonList(tag.getName()));
                        }

                        Pair<String, String> key = ImmutablePair.of(subentry.getKey().name(), entry.getKey());
                        if (methods.containsKey(key) && javadocProvider != null) {
                            OperationResourceInfo ori = methods.get(key);

                            subentry.getValue().setSummary(javadocProvider.getMethodDoc(ori));
                            for (int i = 0; i < subentry.getValue().getParameters().size(); i++) {
                                subentry.getValue().getParameters().get(i).
                                        setDescription(javadocProvider.getMethodParameterDoc(ori, i));
                            }

                            if (subentry.getValue().getResponses() != null
                                    && !subentry.getValue().getResponses().isEmpty()) {

                                subentry.getValue().getResponses().entrySet().iterator().next().getValue().
                                        setDescription(javadocProvider.getMethodResponseDoc(ori));
                            }
                        }
                    }
                }
            }
            if (replaceTags && data.getTags() != null) {
                Collections.sort(data.getTags(), new Comparator<Tag>() {

                    @Override
                    public int compare(final Tag tag1, final Tag tag2) {
                        return ComparatorUtils.<String>naturalComparator().compare(tag1.getName(), tag2.getName());
                    }
                });
            }

            super.writeTo(data, type, genericType, annotations, mediaType, headers, out);
        }

        protected String getNormalizedPath(final String classResourcePath, final String operationResourcePath) {
            StringBuilder normalizedPath = new StringBuilder();

            String[] segments = org.apache.commons.lang3.StringUtils.split(classResourcePath + operationResourcePath,
                    "/");
            for (String segment : segments) {
                if (!org.apache.commons.lang3.StringUtils.isEmpty(segment)) {
                    normalizedPath.append("/").append(segment);
                }
            }
            // Adapt to Swagger's path expression
            if (normalizedPath.toString().endsWith(":.*}")) {
                normalizedPath.setLength(normalizedPath.length() - 4);
                normalizedPath.append('}');
            }
            return StringUtils.EMPTY.equals(normalizedPath.toString()) ? "/" : normalizedPath.toString();
        }
    }
}
