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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public final class Saml2ReactiveUtils {

    private static final Base64 BASE64 = new Base64(0, new byte[] { '\n' });

    private static final char PATH_DELIMITER = '/';

    public static String samlEncode(final byte[] b) {
        return BASE64.encodeAsString(b);
    }

    public static byte[] samlDecode(final String s) {
        return BASE64.decode(s);
    }

    public static byte[] samlDeflate(final String s) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(b, new Deflater(Deflater.DEFLATED, true));
            deflater.write(s.getBytes(StandardCharsets.UTF_8));
            deflater.finish();
            return b.toByteArray();
        } catch (IOException e) {
            throw new Saml2Exception("Unable to deflate string", e);
        }
    }

    public static String samlInflate(final byte[] b) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream iout = new InflaterOutputStream(out, new Inflater(true));
            iout.write(b);
            iout.finish();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new Saml2Exception("Unable to inflate string", e);
        }
    }

    public static String resolveUrlTemplate(
            final String template, final String baseUrl, final RelyingPartyRegistration relyingParty) {

        if (!StringUtils.hasText(template)) {
            return baseUrl;
        }

        String entityId = relyingParty.getProviderDetails().getEntityId();
        String registrationId = relyingParty.getRegistrationId();
        Map<String, String> uriVariables = new HashMap<>();
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(baseUrl).
                replaceQuery(null).
                fragment(null).
                build();
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", scheme == null ? "" : scheme);
        String host = uriComponents.getHost();
        uriVariables.put("baseHost", host == null ? "" : host);
        // following logic is based on HierarchicalUriComponents#toUriString()
        int port = uriComponents.getPort();
        uriVariables.put("basePort", port == -1 ? "" : ":" + port);
        String path = uriComponents.getPath();
        if (StringUtils.hasLength(path)) {
            if (path.charAt(0) != PATH_DELIMITER) {
                path = PATH_DELIMITER + path;
            }
        }
        uriVariables.put("basePath", path == null ? "" : path);
        uriVariables.put("baseUrl", uriComponents.toUriString());
        uriVariables.put("entityId", StringUtils.hasText(entityId) ? entityId : "");
        uriVariables.put("registrationId", StringUtils.hasText(registrationId) ? registrationId : "");

        return UriComponentsBuilder.fromUriString(template)
                .buildAndExpand(uriVariables)
                .toUriString();
    }

    public static String getApplicationUri(final ServerHttpRequest request) {
        UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request).
                replacePath(request.getPath().contextPath().value()).
                replaceQuery(null).
                fragment(null).
                build();
        return uriComponents.toUriString();
    }

    private Saml2ReactiveUtils() {
        // private constructor for static utility class
    }
}
