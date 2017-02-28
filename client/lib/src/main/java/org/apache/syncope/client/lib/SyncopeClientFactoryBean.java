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
package org.apache.syncope.client.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.Marshaller;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * Factory bean for creating instances of {@link SyncopeClient}.
 * Supports Spring-bean configuration and override via subclassing (see protected methods).
 */
public class SyncopeClientFactoryBean {

    public enum ContentType {

        JSON(MediaType.APPLICATION_JSON_TYPE),
        XML(MediaType.APPLICATION_XML_TYPE);

        private final MediaType mediaType;

        ContentType(final MediaType mediaType) {
            this.mediaType = mediaType;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public static ContentType fromString(final String value) {
            return StringUtils.isNotBlank(value) && value.equalsIgnoreCase(XML.getMediaType().toString())
                    ? XML
                    : JSON;
        }
    }

    private JacksonJaxbJsonProvider jsonProvider;

    private JAXBElementProvider<?> jaxbProvider;

    private RestClientExceptionMapper exceptionMapper;

    private String address;

    private ContentType contentType;

    private String domain;

    private boolean useCompression;

    private JAXRSClientFactoryBean restClientFactoryBean;

    protected JacksonJaxbJsonProvider defaultJsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return new JacksonJaxbJsonProvider(objectMapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
    }

    @SuppressWarnings({ "rawtypes" })
    protected JAXBElementProvider<?> defaultJAXBProvider() {
        JAXBElementProvider<?> defaultJAXBProvider = new JAXBElementProvider();

        DocumentDepthProperties depthProperties = new DocumentDepthProperties();
        depthProperties.setInnerElementCountThreshold(500);
        defaultJAXBProvider.setDepthProperties(depthProperties);

        Map<String, Object> marshallerProperties = new HashMap<>();
        marshallerProperties.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        defaultJAXBProvider.setMarshallerProperties(marshallerProperties);

        Map<String, String> collectionWrapperMap = new HashMap<>();
        collectionWrapperMap.put(AbstractPolicyTO.class.getName(), "policies");
        defaultJAXBProvider.setCollectionWrapperMap(collectionWrapperMap);

        return defaultJAXBProvider;
    }

    protected RestClientExceptionMapper defaultExceptionMapper() {
        return new RestClientExceptionMapper();
    }

    protected JAXRSClientFactoryBean defaultRestClientFactoryBean() {
        JAXRSClientFactoryBean defaultRestClientFactoryBean = new JAXRSClientFactoryBean();
        defaultRestClientFactoryBean.setHeaders(new HashMap<String, String>());

        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("Property 'address' is missing");
        }
        defaultRestClientFactoryBean.setAddress(address);

        if (StringUtils.isNotBlank(domain)) {
            defaultRestClientFactoryBean.getHeaders().put(RESTHeaders.DOMAIN, Collections.singletonList(domain));
        }

        defaultRestClientFactoryBean.setThreadSafe(true);
        defaultRestClientFactoryBean.setInheritHeaders(true);

        List<Feature> features = new ArrayList<>();
        features.add(new LoggingFeature());
        defaultRestClientFactoryBean.setFeatures(features);

        List<Object> providers = new ArrayList<>(4);
        providers.add(new DateParamConverterProvider());
        providers.add(getJaxbProvider());
        providers.add(getJsonProvider());
        providers.add(getExceptionMapper());
        defaultRestClientFactoryBean.setProviders(providers);

        return defaultRestClientFactoryBean;
    }

    public JacksonJaxbJsonProvider getJsonProvider() {
        return jsonProvider == null
                ? defaultJsonProvider()
                : jsonProvider;
    }

    public void setJsonProvider(final JacksonJaxbJsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    public JAXBElementProvider<?> getJaxbProvider() {
        return jaxbProvider == null
                ? defaultJAXBProvider()
                : jaxbProvider;
    }

    public SyncopeClientFactoryBean setJaxbProvider(final JAXBElementProvider<?> jaxbProvider) {
        this.jaxbProvider = jaxbProvider;
        return this;
    }

    public RestClientExceptionMapper getExceptionMapper() {
        return exceptionMapper == null
                ? defaultExceptionMapper()
                : exceptionMapper;
    }

    public SyncopeClientFactoryBean setExceptionMapper(final RestClientExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public SyncopeClientFactoryBean setAddress(final String address) {
        this.address = address;
        return this;
    }

    public ContentType getContentType() {
        return contentType == null
                ? ContentType.JSON
                : contentType;
    }

    public SyncopeClientFactoryBean setContentType(final ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public SyncopeClientFactoryBean setContentType(final String contentType) {
        this.contentType = ContentType.fromString(contentType);
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public SyncopeClientFactoryBean setDomain(final String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Sets the given service instance for transparent gzip <tt>Content-Encoding</tt> handling.
     *
     * @param useCompression whether transparent gzip <tt>Content-Encoding</tt> handling is to be enabled
     * @return the current instance
     */
    public SyncopeClientFactoryBean setUseCompression(final boolean useCompression) {
        this.useCompression = useCompression;
        return this;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public JAXRSClientFactoryBean getRestClientFactoryBean() {
        return restClientFactoryBean == null
                ? defaultRestClientFactoryBean()
                : restClientFactoryBean;
    }

    public SyncopeClientFactoryBean setRestClientFactoryBean(final JAXRSClientFactoryBean restClientFactoryBean) {
        this.restClientFactoryBean = restClientFactoryBean;
        return this;
    }

    /**
     * Builds client instance with no authentication, for user self-registration and password reset.
     *
     * @return client instance with no authentication
     */
    public SyncopeClient create() {
        return create(new NoAuthenticationHandler());
    }

    /**
     * Builds client instance with the given credentials.
     * Such credentials will be used only to obtain a valid JWT in the {@link RESTHeaders#TOKEN} header;
     *
     * @param username username
     * @param password password
     * @return client instance with the given credentials
     */
    public SyncopeClient create(final String username, final String password) {
        return create(new BasicAuthenticationHandler(username, password));
    }

    /**
     * Builds client instance which will be passing the provided value in the {@link RESTHeaders#TOKEN}
     * request header.
     *
     * @param jwt value received after login, in the {@link RESTHeaders#TOKEN} response header
     * @return client instance which will be passing the provided value in the {{@link RESTHeaders#TOKEN}
     * request header
     */
    public SyncopeClient create(final String jwt) {
        return create(new JWTAuthenticationHandler(jwt));
    }

    /**
     * Builds client instance with the given authentication handler.
     *
     * @param handler authentication handler
     * @return client instance with the given authentication handler
     */
    public SyncopeClient create(final AuthenticationHandler handler) {
        return new SyncopeClient(
                getContentType().getMediaType(),
                getRestClientFactoryBean(),
                getExceptionMapper(),
                handler,
                useCompression);
    }
}
