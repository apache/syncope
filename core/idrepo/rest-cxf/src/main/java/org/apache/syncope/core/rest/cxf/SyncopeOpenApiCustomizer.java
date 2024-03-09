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
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.DomainHolder;

public class SyncopeOpenApiCustomizer extends OpenApiCustomizer {

    private final DomainHolder<?> domainHolder;

    public SyncopeOpenApiCustomizer(final DomainHolder<?> domainHolder) {
        this.domainHolder = domainHolder;
    }

    @Override
    public OpenAPIConfiguration customize(final OpenAPIConfiguration configuration) {
        super.customize(configuration);

        MessageContext ctx = JAXRSUtils.createContextValue(JAXRSUtils.getCurrentMessage(), null, MessageContext.class);

        String url = StringUtils.substringBeforeLast(ctx.getUriInfo().getRequestUri().getRawPath(), "/");
        configuration.getOpenAPI().setServers(List.of(new Server().url(url)));

        return configuration;
    }

    @Override
    public void customize(final OpenAPI oas) {
        if (oas.getPaths() == null) {
            oas.setPaths(new Paths());
        }

        super.customize(oas);
    }

    @Override
    protected void addParameters(final List<Parameter> parameters) {
        Optional<Parameter> domainHeaderParameter = parameters.stream().
                filter(p -> p instanceof HeaderParameter && RESTHeaders.DOMAIN.equals(p.getName())).findFirst();
        if (domainHeaderParameter.isEmpty()) {
            HeaderParameter parameter = new HeaderParameter();
            parameter.setName(RESTHeaders.DOMAIN);
            parameter.setRequired(true);

            ExternalDocumentation extDoc = new ExternalDocumentation();
            extDoc.setDescription("Apache Syncope Reference Guide");
            extDoc.setUrl("https://syncope.apache.org/docs/3.0/reference-guide.html#domains");

            Schema<String> schema = new Schema<>();
            schema.setDescription("Domains are built to facilitate multitenancy.");
            schema.setExternalDocs(extDoc);
            schema.setEnum(domainHolder.getDomains().keySet().stream().sorted().toList());
            schema.setDefault(SyncopeConstants.MASTER_DOMAIN);
            parameter.setSchema(schema);

            parameters.add(parameter);
        }

        Optional<Parameter> delegatedByHeaderParameter = parameters.stream().
                filter(p -> p instanceof HeaderParameter && RESTHeaders.DELEGATED_BY.equals(p.getName())).findFirst();
        if (delegatedByHeaderParameter.isEmpty()) {
            HeaderParameter parameter = new HeaderParameter();
            parameter.setName(RESTHeaders.DELEGATED_BY);
            parameter.setRequired(false);

            ExternalDocumentation extDoc = new ExternalDocumentation();
            extDoc.setDescription("Apache Syncope Reference Guide");
            extDoc.setUrl("https://syncope.apache.org/docs/3.0/reference-guide.html#delegation");

            Schema<String> schema = new Schema<>();
            schema.setDescription("Acton behalf of someone else");
            schema.setExternalDocs(extDoc);
            parameter.setSchema(schema);

            parameters.add(parameter);
        }
    }

    @Override
    protected void customizeResponses(final Operation operation, final OperationResourceInfo ori) {
        super.customizeResponses(operation, ori);

        ApiResponses responses = Optional.ofNullable(operation.getResponses()).orElseGet(() -> {
            ApiResponses r = new ApiResponses();
            operation.setResponses(r);
            return r;
        });

        Optional.ofNullable(responses.get(ApiResponses.DEFAULT)).ifPresent(defaultResponse -> {
            responses.remove(ApiResponses.DEFAULT);
            responses.addApiResponse("200", defaultResponse);
        });

        Map<String, Header> headers = new LinkedHashMap<>();
        headers.put(
                RESTHeaders.ERROR_CODE,
                new Header().schema(new Schema<>().type("string")).description("Error code"));
        headers.put(
                RESTHeaders.ERROR_INFO,
                new Header().schema(new Schema<>().type("string")).description("Error message(s)"));

        ErrorTO sampleError = new ErrorTO();
        sampleError.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
        sampleError.setType(ClientExceptionType.InvalidEntity);
        sampleError.getElements().add("error message");

        Schema<ErrorTO> errorSchema = new Schema<>();
        errorSchema.example(sampleError).
                addProperty("status", new IntegerSchema().description("HTTP status code")).
                addProperty("type", new StringSchema().
                        _enum(Stream.of(ClientExceptionType.values()).map(Enum::name).toList()).
                        description("Error code")).
                addProperty("elements", new ArraySchema().type("string").description("Error message(s)"));

        Content content = new Content();
        content.addMediaType(
                jakarta.ws.rs.core.MediaType.APPLICATION_JSON,
                new MediaType().schema(errorSchema));
        content.addMediaType(
                RESTHeaders.APPLICATION_YAML,
                new MediaType().schema(errorSchema));
        content.addMediaType(
                jakarta.ws.rs.core.MediaType.APPLICATION_XML,
                new MediaType().schema(errorSchema));

        responses.addApiResponse("400", new ApiResponse().
                description("An error occurred; HTTP status code can vary depending on the actual error: "
                        + "400, 403, 404, 409, 412").
                headers(headers).
                content(content));
    }
}
