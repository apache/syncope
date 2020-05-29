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

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.fasterxml.jackson.jaxrs.yaml.JacksonYAMLProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * Factory bean for creating instances of {@link SyncopeClient}.
 * Supports Spring-bean configuration and override via subclassing (see protected methods).
 */
public class SyncopeClientFactoryBean {

    public enum ContentType {

        JSON(MediaType.APPLICATION_JSON_TYPE),
        YAML(RESTHeaders.APPLICATION_YAML_TYPE),
        XML(MediaType.APPLICATION_XML_TYPE);

        private final MediaType mediaType;

        ContentType(final MediaType mediaType) {
            this.mediaType = mediaType;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public static ContentType fromString(final String value) {
            return XML.getMediaType().toString().equalsIgnoreCase(value)
                    ? XML
                    : YAML.getMediaType().toString().equalsIgnoreCase(value)
                    ? YAML
                    : JSON;
        }
    }

    private JacksonJsonProvider jsonProvider;

    private JacksonXMLProvider xmlProvider;

    private JacksonYAMLProvider yamlProvider;

    private RestClientExceptionMapper exceptionMapper;

    private String address;

    private ContentType contentType;

    private String domain;

    private boolean useCompression;

    private TLSClientParameters tlsClientParameters;

    private JAXRSClientFactoryBean restClientFactoryBean;

    protected static JacksonJsonProvider defaultJsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return new JacksonJsonProvider(objectMapper);
    }

    protected static JacksonXMLProvider defaultXmlProvider() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JodaModule());
        xmlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        xmlMapper.configOverride(List.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        xmlMapper.configOverride(Set.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        xmlMapper.configOverride(Map.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        return new JacksonXMLProvider(xmlMapper);
    }

    protected static JacksonYAMLProvider defaultYamlProvider() {
        YAMLMapper yamlMapper = new YAMLMapper();
        yamlMapper.registerModule(new JodaModule());
        yamlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return new JacksonYAMLProvider(yamlMapper);
    }

    protected static RestClientExceptionMapper defaultExceptionMapper() {
        return new RestClientExceptionMapper();
    }

    protected JAXRSClientFactoryBean defaultRestClientFactoryBean() {
        JAXRSClientFactoryBean defaultRestClientFactoryBean = new JAXRSClientFactoryBean();
        defaultRestClientFactoryBean.setHeaders(new HashMap<>());

        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("Property 'address' is missing");
        }
        defaultRestClientFactoryBean.setAddress(address);

        if (StringUtils.isNotBlank(domain)) {
            defaultRestClientFactoryBean.getHeaders().put(RESTHeaders.DOMAIN, List.of(domain));
        }

        defaultRestClientFactoryBean.setThreadSafe(true);
        defaultRestClientFactoryBean.setInheritHeaders(true);

        defaultRestClientFactoryBean.setFeatures(List.of(new LoggingFeature()));

        defaultRestClientFactoryBean.setProviders(List.of(
                new DateParamConverterProvider(),
                getJsonProvider(),
                getXmlProvider(),
                getYamlProvider(),
                getExceptionMapper()));

        return defaultRestClientFactoryBean;
    }

    public JacksonJsonProvider getJsonProvider() {
        return Optional.ofNullable(jsonProvider).orElseGet(SyncopeClientFactoryBean::defaultJsonProvider);
    }

    public void setJsonProvider(final JacksonJsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    public JacksonXMLProvider getXmlProvider() {
        return xmlProvider == null
                ? defaultXmlProvider()
                : xmlProvider;
    }

    public void setXmlProvider(final JacksonXMLProvider xmlProvider) {
        this.xmlProvider = xmlProvider;
    }

    public JacksonYAMLProvider getYamlProvider() {
        return yamlProvider == null
                ? defaultYamlProvider()
                : yamlProvider;
    }

    public void setYamlProvider(final JacksonYAMLProvider yamlProvider) {
        this.yamlProvider = yamlProvider;
    }

    public RestClientExceptionMapper getExceptionMapper() {
        return Optional.ofNullable(exceptionMapper).orElseGet(SyncopeClientFactoryBean::defaultExceptionMapper);
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
        return Optional.ofNullable(contentType).orElse(ContentType.JSON);
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
     * Sets the given service instance for transparent gzip {@code Content-Encoding} handling.
     *
     * @param useCompression whether transparent gzip {@code Content-Encoding} handling is to be enabled
     * @return the current instance
     */
    public SyncopeClientFactoryBean setUseCompression(final boolean useCompression) {
        this.useCompression = useCompression;
        return this;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    /**
     * Sets the client TLS configuration.
     *
     * @param tlsClientParameters client TLS configuration
     * @return the current instance
     */
    public SyncopeClientFactoryBean setTlsClientParameters(final TLSClientParameters tlsClientParameters) {
        this.tlsClientParameters = tlsClientParameters;
        return this;
    }

    public TLSClientParameters getTlsClientParameters() {
        return tlsClientParameters;
    }

    public JAXRSClientFactoryBean getRestClientFactoryBean() {
        return Optional.ofNullable(restClientFactoryBean).orElseGet(this::defaultRestClientFactoryBean);
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
     * Such credentials will be used only to obtain a valid JWT in the
     * {@link javax.ws.rs.core.HttpHeaders#AUTHORIZATION} header;
     *
     * @param username username
     * @param password password
     * @return client instance with the given credentials
     */
    public SyncopeClient create(final String username, final String password) {
        return create(new BasicAuthenticationHandler(username, password));
    }

    /**
     * Builds client instance which will be passing the provided value in the
     * {@link javax.ws.rs.core.HttpHeaders#AUTHORIZATION} request header.
     *
     * @param jwt value received after login, in the {@link RESTHeaders#TOKEN} response header
     * @return client instance which will be passing the provided value in the
     * {@link javax.ws.rs.core.HttpHeaders#AUTHORIZATION} request header
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
                useCompression,
                tlsClientParameters);
    }
}
