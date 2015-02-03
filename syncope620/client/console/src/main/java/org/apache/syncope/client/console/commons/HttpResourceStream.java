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
package org.apache.syncope.client.console.commons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IFixedLocationResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

public class HttpResourceStream extends AbstractResourceStream implements IFixedLocationResourceStream {

    private static final long serialVersionUID = 5811207817876330189L;

    private transient InputStream inputStream;

    private String contentType;

    private String location;

    private String filename;

    public HttpResourceStream(final Response response) {
        super();

        Object entity = response.getEntity();
        if (response.getStatusInfo().getStatusCode() == Response.Status.OK.getStatusCode()
                && (entity instanceof InputStream)) {

            this.inputStream = (InputStream) entity;
            this.contentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
            this.location = response.getLocation() == null ? null : response.getLocation().toASCIIString();
            String contentDisposition = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
            if (StringUtils.isNotBlank(contentDisposition)) {
                String[] splitted = contentDisposition.split("=");
                if (splitted != null && splitted.length > 1) {
                    this.filename = splitted[1].trim();
                }
            }
        }
    }

    @Override
    public InputStream getInputStream()
            throws ResourceStreamNotFoundException {

        return inputStream == null
                ? new ByteArrayInputStream(new byte[0])
                : inputStream;
    }

    @Override
    public Bytes length() {
        return inputStream == null
                ? Bytes.bytes(0)
                : null;
    }

    @Override
    public void close() throws IOException {
        // No need for explict closing
    }

    @Override
    public String locationAsString() {
        return location;
    }

    @Override
    public String getContentType() {
        return contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : contentType;
    }

    public String getFilename() {
        return filename;
    }
}
