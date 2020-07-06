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
package org.apache.syncope.sra.filters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;

public class LinkRewriteGatewayFilterFactory extends ModifyResponseGatewayFilterFactory {

    @Override
    protected boolean skipCond(final ServerHttpResponseDecorator decorator) {
        return decorator.getHeaders().getContentType() == null
                || !StringUtils.containsIgnoreCase(decorator.getHeaders().getContentType().toString(), "html");
    }

    private Charset getCharset(final ServerHttpResponseDecorator decorator) {
        return decorator.getHeaders().getContentType() != null
                && decorator.getHeaders().getContentType().getCharset() != null
                ? decorator.getHeaders().getContentType().getCharset()
                : StandardCharsets.UTF_8;
    }

    private void replace(final Document doc, final String element, final String attr, final String prefix) {
        doc.select(element).forEach(link -> {
            String attrValue = link.attributes().get(attr);
            if (attrValue.startsWith("/") && !attrValue.startsWith("//")) {
                link.attr(attr, attrValue.replace(attrValue, prefix + attrValue));
            }
        });
    }

    @Override
    protected byte[] modifyResponse(
            final InputStream responseBody,
            final Config config,
            final ServerHttpResponseDecorator decorator,
            final ServerWebExchange exchange)
            throws IOException {

        String[] keyValue = config.getData().split(",");

        String oldBase = StringUtils.appendIfMissing(keyValue[0], "/");
        String newBase = StringUtils.appendIfMissing(keyValue[1], "/");
        String newBaseAsPrefix = StringUtils.removeEnd(keyValue[1], "/");

        boolean rewriterRootAttrs = true;
        if (keyValue.length == 3) {
            rewriterRootAttrs = BooleanUtils.toBoolean(keyValue[2]);
        }

        Document doc = Jsoup.parse(
                responseBody, getCharset(decorator).name(), exchange.getRequest().getURI().toASCIIString());

        if (rewriterRootAttrs) {
            replace(doc, "a", "href", newBaseAsPrefix);
            replace(doc, "link", "href", newBaseAsPrefix);
            replace(doc, "img", "src", newBaseAsPrefix);
            replace(doc, "script", "src", newBaseAsPrefix);
            replace(doc, "object", "data", newBaseAsPrefix);
        }

        return doc.toString().replace(oldBase, newBase).getBytes();
    }
}
