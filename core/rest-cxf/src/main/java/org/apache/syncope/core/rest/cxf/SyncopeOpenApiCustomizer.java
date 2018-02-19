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

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.spring.ApplicationContextProvider;

public class SyncopeOpenApiCustomizer extends OpenApiCustomizer {

    private List<String> domains;

    public SyncopeOpenApiCustomizer() {
        super();

        URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
        if (javaDocURLs != null) {
            super.setJavaDocURLs(javaDocURLs);
        }
    }

    @Override
    public OpenAPIConfiguration customize(final OpenAPIConfiguration configuration) {
        Map<String, Header> headers = new LinkedHashMap<>();
        headers.put(
                RESTHeaders.ERROR_CODE,
                new Header().schema(new Schema<>().type("string")).description("Error code"));
        headers.put(
                RESTHeaders.ERROR_INFO,
                new Header().schema(new Schema<>().type("string")).description("Error message"));

        Content content = new Content();
        content.addMediaType(
                javax.ws.rs.core.MediaType.APPLICATION_JSON, new MediaType().schema(new Schema<ErrorTO>()));
        content.addMediaType(
                javax.ws.rs.core.MediaType.APPLICATION_XML, new MediaType().schema(new Schema<ErrorTO>()));

        configuration.getOpenAPI().getComponents().addResponses("400", new ApiResponse().
                description("An error occurred; HTTP status code can vary depending on the actual error: "
                        + "400, 403, 404, 409, 412").
                headers(headers).
                content(content));

        return super.customize(configuration);
    }

    @Override
    protected void addParameters(final List<Parameter> parameters) {
        if (domains == null) {
            domains = new ArrayList<>(
                    ApplicationContextProvider.getApplicationContext().
                            getBean(DomainsHolder.class).getDomains().keySet());
        }

        Optional<Parameter> domainHeaderParameter = parameters.stream().filter(parameter
                -> parameter instanceof HeaderParameter && RESTHeaders.DOMAIN.equals(parameter.getName())).
                findFirst();
        if (!domainHeaderParameter.isPresent()) {
            HeaderParameter parameter = new HeaderParameter();
            parameter.setName(RESTHeaders.DOMAIN);
            parameter.setRequired(true);

            Schema<String> schema = new Schema<>();
            schema.setType("string");
            schema.setEnum(domains);
            schema.setDefault(SyncopeConstants.MASTER_DOMAIN);
            parameter.setSchema(schema);

            parameters.add(parameter);
        }
    }
}
