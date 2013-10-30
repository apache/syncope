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
package org.apache.syncope.client.rest;

import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;

public class RestClientFactoryBean extends JAXRSClientFactoryBean {

    public <T> T createServiceInstance(final Class<T> serviceClass, final MediaType mediaType) {
        return createServiceInstance(serviceClass, mediaType, null, null);
    }

    public <T> T createServiceInstance(
            final Class<T> serviceClass, final MediaType mediaType, final String username, final String password) {

        if (StringUtils.isNotBlank(username)) {
            setUsername(username);
        }
        if (StringUtils.isNotBlank(password)) {
            setPassword(password);
        }
        setServiceClass(serviceClass);
        final T serviceInstance = create(serviceClass);
        WebClient.client(serviceInstance).type(mediaType).accept(mediaType);
        return serviceInstance;
    }
}
