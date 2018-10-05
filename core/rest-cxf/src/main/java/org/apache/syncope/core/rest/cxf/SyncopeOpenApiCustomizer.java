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

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.spring.ApplicationContextProvider;

public class SyncopeOpenApiCustomizer extends OpenApiCustomizer {

    // remove the line below with CXF 3.2.7
    private JavaDocProvider javadocProvider;

    // remove the line below with CXF 3.2.7
    private boolean replaceTags;

    // remove the line below with CXF 3.2.7
    private List<ClassResourceInfo> cris;

    private List<String> domains;

    public SyncopeOpenApiCustomizer() {
        super();

        URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
        if (javaDocURLs != null) {
            // remove the line below with CXF 3.2.7
            this.javadocProvider = new JavaDocProvider(javaDocURLs);
            super.setJavaDocURLs(javaDocURLs);
        }
    }

    @Override
    public void setReplaceTags(final boolean replaceTags) {
        // remove this method with CXF 3.2.7
        this.replaceTags = replaceTags;
        super.setReplaceTags(replaceTags);
    }

    @Override
    public void setClassResourceInfos(final List<ClassResourceInfo> classResourceInfos) {
        // remove this method with CXF 3.2.7
        this.cris = classResourceInfos;
        super.setClassResourceInfos(classResourceInfos);
    }

    @Override
    public void customize(final OpenAPI oas) {
        // remove this method with CXF 3.2.7
        if (replaceTags || javadocProvider != null) {
            Map<String, ClassResourceInfo> operations = new HashMap<>();
            Map<Pair<String, String>, OperationResourceInfo> methods = new HashMap<>();
            cris.forEach(cri -> {
                cri.getMethodDispatcher().getOperationResourceInfos().forEach(ori -> {
                    String normalizedPath = getNormalizedPath(
                            cri.getURITemplate().getValue(), ori.getURITemplate().getValue());

                    operations.put(normalizedPath, cri);
                    methods.put(Pair.of(ori.getHttpMethod(), normalizedPath), ori);
                });
            });

            List<Tag> tags = new ArrayList<>();
            oas.getPaths().forEach((pathKey, pathItem) -> {
                Tag tag = null;
                if (replaceTags && operations.containsKey(pathKey)) {
                    ClassResourceInfo cri = operations.get(pathKey);

                    tag = new Tag();
                    tag.setName(cri.getURITemplate().getValue().replaceAll("/", "_"));
                    if (javadocProvider != null) {
                        tag.setDescription(javadocProvider.getClassDoc(cri));
                    }

                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }

                for (Map.Entry<PathItem.HttpMethod, Operation> subentry : pathItem.readOperationsMap().entrySet()) {
                    if (replaceTags && tag != null) {
                        subentry.getValue().setTags(Collections.singletonList(tag.getName()));
                    }

                    Pair<String, String> key = Pair.of(subentry.getKey().name(), pathKey);
                    if (methods.containsKey(key) && javadocProvider != null) {
                        OperationResourceInfo ori = methods.get(key);

                        if (StringUtils.isBlank(subentry.getValue().getSummary())) {
                            subentry.getValue().setSummary(javadocProvider.getMethodDoc(ori));
                        }
                        if (subentry.getValue().getParameters() == null) {
                            List<Parameter> parameters = new ArrayList<>();
                            addParameters(parameters);
                            subentry.getValue().setParameters(parameters);
                        } else {
                            for (int i = 0; i < subentry.getValue().getParameters().size(); i++) {
                                if (StringUtils.isBlank(subentry.getValue().getParameters().get(i).getDescription())) {
                                    subentry.getValue().getParameters().get(i).
                                            setDescription(javadocProvider.getMethodParameterDoc(ori, i));
                                }
                            }
                            addParameters(subentry.getValue().getParameters());
                        }

                        customizeResponses(subentry.getValue(), ori);
                    }
                }
            });
            if (replaceTags && oas.getTags() != null) {
                oas.setTags(tags);
            }
        }
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

    protected void customizeResponses(final Operation operation, final OperationResourceInfo ori) {
        // this will be replaced by super.customizeResponses(operation, ori);
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            ApiResponse response = operation.getResponses().entrySet().iterator().next().getValue();
            if (StringUtils.isBlank(response.getDescription())
                    || (StringUtils.isNotBlank(javadocProvider.getMethodResponseDoc(ori))
                    && Reader.DEFAULT_DESCRIPTION.equals(response.getDescription()))) {

                response.setDescription(javadocProvider.getMethodResponseDoc(ori));
            }
        }
        //

        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        ApiResponse defaultResponse = responses.getDefault();
        if (defaultResponse != null) {
            responses.remove(ApiResponses.DEFAULT);
            responses.addApiResponse("200", defaultResponse);
        }

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
                RESTHeaders.APPLICATION_YAML, new MediaType().schema(new Schema<ErrorTO>()));
        content.addMediaType(
                javax.ws.rs.core.MediaType.APPLICATION_XML, new MediaType().schema(new Schema<ErrorTO>()));

        responses.addApiResponse("400", new ApiResponse().
                description("An error occurred; HTTP status code can vary depending on the actual error: "
                        + "400, 403, 404, 409, 412").
                headers(headers).
                content(content));
    }
}
