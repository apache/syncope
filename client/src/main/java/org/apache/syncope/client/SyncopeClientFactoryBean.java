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
package org.apache.syncope.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.Marshaller;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.syncope.client.rest.RestClientExceptionMapper;
import org.apache.syncope.client.rest.RestClientFactoryBean;
import org.apache.syncope.common.to.AbstractPolicyTO;

/**
 * Factory bean for creating instances of <tt>SyncopeClient</tt>.
 * Supports Spring-bean configuration and override via subclassing (see protected methods).
 */
public class SyncopeClientFactoryBean {

    public enum ContentType {

        JSON(MediaType.APPLICATION_JSON_TYPE),
        XML(MediaType.APPLICATION_XML_TYPE);

        private final MediaType mediaType;

        private ContentType(final MediaType mediaType) {
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

    @SuppressWarnings("rawtypes")
    private JAXBElementProvider jaxbProvider;

    private RestClientExceptionMapper exceptionMapper;

    private String address;

    private ContentType contentType;

    private RestClientFactoryBean restClientFactoryBean;

    protected JacksonJaxbJsonProvider defaultJsonProvider() {
        return new JacksonJaxbJsonProvider();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected JAXBElementProvider defaultJAXBProvider() {
        JAXBElementProvider defaultJAXBProvider = new JAXBElementProvider();

        DocumentDepthProperties depthProperties = new DocumentDepthProperties();
        depthProperties.setInnerElementCountThreshold(500);
        defaultJAXBProvider.setDepthProperties(depthProperties);

        Map marshallerProperties = new HashMap();
        marshallerProperties.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        defaultJAXBProvider.setMarshallerProperties(marshallerProperties);

        Map<String, String> collectionWrapperMap = new HashMap<String, String>();
        collectionWrapperMap.put(AbstractPolicyTO.class.getName(), "policies");
        defaultJAXBProvider.setCollectionWrapperMap(collectionWrapperMap);

        return defaultJAXBProvider;
    }

    protected RestClientExceptionMapper defaultExceptionMapper() {
        return new RestClientExceptionMapper();
    }

    protected RestClientFactoryBean defaultRestClientFactoryBean() {
        RestClientFactoryBean defaultRestClientFactoryBean = new RestClientFactoryBean();

        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("Property 'address' is missing");
        }
        defaultRestClientFactoryBean.setAddress(address);

        defaultRestClientFactoryBean.setThreadSafe(true);
        defaultRestClientFactoryBean.setInheritHeaders(true);

        List<Feature> features = new ArrayList<Feature>();
        features.add(new LoggingFeature());
        defaultRestClientFactoryBean.setFeatures(features);

        List<Object> providers = new ArrayList<Object>(3);
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

    public JAXBElementProvider getJaxbProvider() {
        return jaxbProvider == null
                ? defaultJAXBProvider()
                : jaxbProvider;
    }

    public SyncopeClientFactoryBean setJaxbProvider(final JAXBElementProvider jaxbProvider) {
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

    public RestClientFactoryBean getRestClientFactoryBean() {
        return restClientFactoryBean == null
                ? defaultRestClientFactoryBean()
                : restClientFactoryBean;
    }

    public SyncopeClientFactoryBean setRestClientFactoryBean(final RestClientFactoryBean restClientFactoryBean) {
        this.restClientFactoryBean = restClientFactoryBean;
        return this;
    }

    public SyncopeClient create(final String username, final String password) {
        return new SyncopeClient(getContentType().getMediaType(), getRestClientFactoryBean(), username, password);
    }
}
