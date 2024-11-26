/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.lib.batch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class BatchOfflineHTTPConduit extends HTTPConduit {

    private ByteArrayOutputStream baos;

    public BatchOfflineHTTPConduit(final Bus bus, final EndpointInfo ei) throws IOException {
        this(bus, ei, null);
    }

    public BatchOfflineHTTPConduit(
            final Bus bus,
            final EndpointInfo ei,
            final EndpointReferenceType t) throws IOException {

        super(bus, ei, t);
        this.proxyAuthSupplier = new DefaultBasicAuthSupplier();
        this.proxyAuthorizationPolicy = new ProxyAuthorizationPolicy();
    }

    @Override
    protected void setupConnection(
            final Message message, final Address address,
            final HTTPClientPolicy csPolicy) {
    }

    @Override
    public HTTPClientPolicy getClient(final Message message) {
        return new HTTPClientPolicy();
    }

    @Override
    protected OutputStream createOutputStream(
            final Message message,
            final boolean needToCacheRequest,
            final boolean isChunking,
            final int chunkThreshold) {

        baos = new ByteArrayOutputStream();
        return baos;
    }

    public ByteArrayOutputStream getOutputStream() {
        return baos;
    }
}
