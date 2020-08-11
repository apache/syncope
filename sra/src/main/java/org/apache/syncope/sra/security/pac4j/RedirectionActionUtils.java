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
package org.apache.syncope.sra.security.pac4j;

import java.net.URI;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

public final class RedirectionActionUtils {

    public static Mono<Void> handle(
            final RedirectionAction action,
            final ServerWebExchangeContext swec) {

        if (action instanceof WithLocationAction) {
            WithLocationAction withLocationAction = (WithLocationAction) action;
            swec.getNative().getResponse().setStatusCode(HttpStatus.FOUND);
            swec.getNative().getResponse().getHeaders().setLocation(URI.create(withLocationAction.getLocation()));
            return swec.getNative().getResponse().setComplete();
        } else if (action instanceof WithContentAction) {
            WithContentAction withContentAction = (WithContentAction) action;
            String content = withContentAction.getContent();

            if (content == null) {
                throw new IllegalArgumentException("No content set for POST AuthnRequest");
            }

            return Mono.defer(() -> {
                swec.getNative().getResponse().getHeaders().setContentType(MediaType.TEXT_HTML);
                return swec.getNative().getResponse().
                        writeWith(Mono.just(swec.getNative().getResponse().bufferFactory().wrap(content.getBytes())));
            });
        } else {
            throw new IllegalArgumentException("Unsupported Action: " + action.getClass().getName());
        }
    }

    private RedirectionActionUtils() {
        // private constructor for static utility class
    }
}
