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
package org.apache.syncope.sra.security.saml2;

import org.apache.syncope.sra.security.pac4j.ServerHttpContext;
import java.net.URI;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

abstract class SAML2RequestGenerator {

    protected final SAML2Client saml2Client;

    protected SAML2RequestGenerator(final SAML2Client saml2Client) {
        this.saml2Client = saml2Client;
    }

    protected Mono<Void> handle(
            final RedirectionAction action,
            final ServerHttpContext shc) {

        if (action instanceof WithLocationAction) {
            WithLocationAction withLocationAction = (WithLocationAction) action;
            shc.getNative().getResponse().setStatusCode(HttpStatus.FOUND);
            shc.getNative().getResponse().getHeaders().setLocation(URI.create(withLocationAction.getLocation()));
            return shc.getNative().getResponse().setComplete();
        } else if (action instanceof WithContentAction) {
            WithContentAction withContentAction = (WithContentAction) action;
            String content = withContentAction.getContent();

            if (content == null) {
                throw new IllegalArgumentException("No content set for POST AuthnRequest");
            }

            return Mono.defer(() -> {
                shc.getNative().getResponse().getHeaders().setContentType(MediaType.TEXT_HTML);
                return shc.getNative().getResponse().
                        writeWith(Mono.just(shc.getNative().getResponse().bufferFactory().wrap(content.getBytes())));
            });
        } else {
            throw new IllegalArgumentException("Unsupported Action: " + action.getClass().getName());
        }
    }
}
