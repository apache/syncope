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
package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public final class BatchPayloadGenerator {

    private static final String HTTP_1_1 = "HTTP/1.1";

    public static <T extends BatchItem> String generate(final List<T> items, final String boundary) {
        StringBuilder payload = new StringBuilder();

        items.forEach(item -> {
            payload.append(boundary).append(JAXRSService.CRLF);
            payload.append(HttpHeaders.CONTENT_TYPE).append(": ").append("application/http").append('\n');
            payload.append("Content-Transfer-Encoding: binary").append('\n');
            payload.append(JAXRSService.CRLF);

            if (item instanceof BatchRequestItem) {
                BatchRequestItem bri = BatchRequestItem.class.cast(item);
                payload.append(bri.getMethod()).append(' ').append(bri.getRequestURI());
                if (bri.getQueryString() != null) {
                    payload.append('?').append(bri.getQueryString());
                }
                payload.append(' ').append(HTTP_1_1).append('\n');
            }

            if (item instanceof BatchResponseItem) {
                BatchResponseItem bri = BatchResponseItem.class.cast(item);
                payload.append(HTTP_1_1).append(' ').
                        append(bri.getStatus()).append(' ').
                        append(Response.Status.fromStatusCode(bri.getStatus()).getReasonPhrase()).
                        append('\n');
            }

            if (item.getHeaders() != null && !item.getHeaders().isEmpty()) {
                item.getHeaders().forEach((key, values) -> values.forEach(
                        value -> payload.append(key).append(": ").append(value).append('\n')));
                payload.append(JAXRSService.CRLF);
            }

            if (item.getContent() != null) {
                payload.append(item.getContent()).append('\n');
            }
        });

        payload.append(boundary).append(JAXRSService.DOUBLE_DASH).append('\n');

        return payload.toString();
    }

    private BatchPayloadGenerator() {
        // private constructor for static utility class
    }
}
