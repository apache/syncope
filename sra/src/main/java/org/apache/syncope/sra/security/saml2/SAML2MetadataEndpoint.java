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

import com.google.common.net.HttpHeaders;
import org.apache.syncope.sra.SecurityConfig;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(SAML2MetadataEndpoint.METADATA_URL)
@ConditionalOnProperty(name = SecurityConfig.AM_TYPE, havingValue = "SAML2")
public class SAML2MetadataEndpoint {

    public static final String METADATA_URL = "/saml2/metadata";

    private final SAML2Client saml2Client;

    public SAML2MetadataEndpoint(final SAML2Client saml2Client) {
        this.saml2Client = saml2Client;
    }

    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE })
    @ResponseBody
    public Mono<ResponseEntity<String>> metadata(final ServerHttpRequest request) {
        return Mono.just(ResponseEntity.ok().
                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE).
                body(saml2Client.getServiceProviderMetadataResolver().getMetadata()));
    }
}
