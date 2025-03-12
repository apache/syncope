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
package org.apache.syncope.client.ui.commons.rest;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

public class ResponseHolder implements Serializable {

    private static final long serialVersionUID = 2627155013246805827L;

    private transient InputStream inputStream;

    private String contentType;

    private String location;

    private String filename;

    public ResponseHolder(final Response response) {
        Object entity = response.getEntity();
        if (response.getStatusInfo().getStatusCode() == Response.Status.OK.getStatusCode()
                && (entity instanceof final InputStream stream)) {

            this.inputStream = stream;
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

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getContentType() {
        return contentType;
    }

    public String getLocation() {
        return location;
    }

    public String getFilename() {
        return filename;
    }
}
