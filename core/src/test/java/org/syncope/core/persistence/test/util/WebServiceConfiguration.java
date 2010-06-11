/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.syncope.core.persistence.test.util;

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
 *
 * @author fabio
 * @version 1.0
 * @since 1.0
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
