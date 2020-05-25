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
package org.apache.syncope.core.rest.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.swagger.openapi.SwaggerToOpenApiConversionUtils;

public class OpenApiFilter implements Filter {

    @Override
    public void init(final FilterConfig config) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(
            final ServletRequest request,
            final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        Response swagger = WebClient.create(
                StringUtils.substringBeforeLast(httpReq.getRequestURL().toString(), "/") + "/swagger.json").
                accept(MediaType.APPLICATION_JSON_TYPE).get();

        String swaggerJson = IOUtils.toString((InputStream) swagger.getEntity(), StandardCharsets.UTF_8);
        String openApiJson = SwaggerToOpenApiConversionUtils.getOpenApiFromSwaggerJson(swaggerJson);

        JsonMapObjectReaderWriter readerWriter = new JsonMapObjectReaderWriter();
        JsonMapObject openapi = readerWriter.fromJsonToJsonObject(openApiJson);

        String basePath = StringUtils.substringBeforeLast(httpReq.getRequestURI(), "/");
        openapi.setProperty("servers", Arrays.asList(Collections.singletonMap("url", basePath)));

        response.setContentType(MediaType.APPLICATION_JSON);
        response.getOutputStream().write(readerWriter.toJson(openapi).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}
