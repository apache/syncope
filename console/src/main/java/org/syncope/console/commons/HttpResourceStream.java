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
package org.syncope.console.commons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IFixedLocationResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.syncope.client.http.PreemptiveAuthHttpRequestFactory;

public class HttpResourceStream extends AbstractResourceStream
        implements IFixedLocationResourceStream {

    private static final long serialVersionUID = 5811207817876330189L;

    private static final Logger LOG =
            LoggerFactory.getLogger(HttpResourceStream.class);

    private final URI uri;

    private final RestTemplate restTemplate;

    private transient HttpEntity responseEntity;

    private transient String contentType;

    private transient String filename;

    public HttpResourceStream(final String uri, final RestTemplate restTemplate)
            throws URISyntaxException {

        this.uri = new URI(Args.notNull(uri, "uri"));
        this.restTemplate = Args.notNull(restTemplate, "restTemplate");
    }

    private HttpResponse buildFakeResponse(final String errorMessage) {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(bais);
        entity.setContentLength(0);
        entity.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        BasicHttpResponse response = new BasicHttpResponse(
                new ProtocolVersion("HTTP", 1, 1), 400,
                "Exception: " + errorMessage);
        response.setEntity(entity);

        response.addHeader("Content-Disposition", "attachment; filename=error");

        return response;
    }

    private void execute() {
        if (responseEntity != null) {
            return;
        }

        HttpGet getMethod = new HttpGet(this.uri);
        HttpResponse response;
        try {
            response = ((PreemptiveAuthHttpRequestFactory) restTemplate.
                    getRequestFactory()).getHttpClient().execute(getMethod);
        } catch (Exception e) {
            LOG.error("Unexpected exception while executing HTTP method to {}",
                    this.uri, e);
            response = buildFakeResponse(e.getMessage());
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("Unsuccessful HTTP method to {}", this.uri);
            response = buildFakeResponse("HTTP status "
                    + response.getStatusLine().getStatusCode());
        }

        responseEntity = response.getEntity();

        Header[] headers = response.getHeaders("Content-Disposition");
        if (headers != null && headers.length > 0) {
            String value = headers[0].getValue();
            String[] splitted = value.split("=");
            if (splitted != null && splitted.length > 1) {
                filename = splitted[1].trim();
            }
        } else {
            LOG.warn("Could not find Content-Disposition HTTP header");
        }

        contentType = responseEntity.getContentType().getValue();
    }

    @Override
    public InputStream getInputStream()
            throws ResourceStreamNotFoundException {

        try {
            execute();
            return responseEntity.getContent();
        } catch (Exception e) {
            throw new ResourceStreamNotFoundException(e);
        }
    }

    @Override
    public void close()
            throws IOException {
        // Nothing needed here, because we are using HttpComponents HttpClient
    }

    @Override
    public String locationAsString() {
        return uri.toString();
    }

    @Override
    public String getContentType() {
        execute();

        return contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
    }

    public String getFilename() {
        execute();
        return filename;
    }
}
