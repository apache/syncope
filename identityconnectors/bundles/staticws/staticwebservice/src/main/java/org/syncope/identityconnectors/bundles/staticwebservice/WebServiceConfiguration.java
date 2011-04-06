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
package org.syncope.identityconnectors.bundles.staticwebservice;

import java.net.MalformedURLException;
import java.net.URL;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.common.StringUtil;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the WebService Connector.
 */
public class WebServiceConfiguration extends AbstractConfiguration {

    /*
     * Web Service Endpoint.
     */
    private String endpoint = null;

    /*
     * Public Web Service interface class
     */
    private String servicename = null;

    /**
     * Accessor for the example property. Uses ConfigurationProperty annotation
     * to provide property metadata to the application.
     */
    @ConfigurationProperty(displayMessageKey = "ENDPOINT_DISPLAY",
    helpMessageKey = "ENDPOINT_HELP",
    confidential = false)
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Setter for the example property.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Accessor for the example property. Uses ConfigurationProperty annotation
     * to provide property metadata to the application.
     */
    @ConfigurationProperty(displayMessageKey = "CLASSNAME_DISPLAY",
    helpMessageKey = "CLASSNAME_HELP",
    confidential = false)
    public String getServicename() {
        return servicename;
    }

    public void setServicename(String classname) {
        this.servicename = classname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        // Check if endpoint has been specified.
        if (StringUtil.isBlank(endpoint)) {
            throw new IllegalArgumentException(
                    "Endpoint cannot be null or empty.");
        }

        // Check if servicename has been specified.
        if (StringUtil.isBlank(servicename)) {
            throw new IllegalArgumentException(
                    "Service name cannot be null or empty.");
        }

        try {
            // Check if the specified enpoint is a well-formed URL.
            new URL(endpoint);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(
                    "The specified endpoint is not a valid URL.");
        }
    }
}
