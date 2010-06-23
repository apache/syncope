/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.test.persistence.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.common.StringUtil;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the WebService Connector.
 */
public class WebServiceConfiguration extends AbstractConfiguration {

    /*
     * Set up base configuration elements
     */
    private String endpoint = null;

    private String context = null;

    private String service = null;

    /**
     * Accessor for the example property. Uses ConfigurationProperty annotation
     * to provide property metadata to the application.
     */
    @ConfigurationProperty(displayMessageKey = "ENDPOINT_DISPLAY", helpMessageKey = "ENDPOINT_HELP", confidential = false)
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Accessor for the example property. Uses ConfigurationProperty annotation
     * to provide property metadata to the application.
     */
    @ConfigurationProperty(displayMessageKey = "SERVICE_DISPLAY", helpMessageKey = "SERVICE_HELP", confidential = false)
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    /**
     * Accessor for the example property. Uses ConfigurationProperty annotation
     * to provide property metadata to the application.
     */
    @ConfigurationProperty(displayMessageKey = "CONTEXT_DISPLAY", helpMessageKey = "CONTEXT_HELP", confidential = false)
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Constructor
     */
    public WebServiceConfiguration() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        if (StringUtil.isBlank(endpoint)) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty.");
        }

        if (StringUtil.isBlank(service)) {
            throw new IllegalArgumentException("Service cannot be null or empty.");
        }

        if (StringUtil.isBlank(context)) {
            throw new IllegalArgumentException("Context cannot be null or empty.");
        }
    }

    public String serializeToXML() {
        ByteArrayOutputStream tokenContentOS = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(tokenContentOS);
        encoder.writeObject(this);
        encoder.flush();
        encoder.close();

        return tokenContentOS.toString();
    }

    public static Object buildFromXML(String xml) {
        ByteArrayInputStream tokenContentIS = new ByteArrayInputStream(xml.getBytes());
        XMLDecoder decoder = new XMLDecoder(tokenContentIS);
        Object object = decoder.readObject();
        decoder.close();

        return object;
    }
}
