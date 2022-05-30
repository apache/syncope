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
package org.apache.syncope.core.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.ext.scimv2.api.data.AuthenticationScheme;
import org.apache.syncope.ext.scimv2.api.data.BulkConfigurationOption;
import org.apache.syncope.ext.scimv2.api.data.ConfigurationOption;
import org.apache.syncope.ext.scimv2.api.data.FilterConfigurationOption;
import org.apache.syncope.ext.scimv2.api.data.Meta;
import org.apache.syncope.ext.scimv2.api.data.ResourceType;
import org.apache.syncope.ext.scimv2.api.data.SchemaExtension;
import org.apache.syncope.ext.scimv2.api.data.ServiceProviderConfig;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.springframework.security.access.prepost.PreAuthorize;

public class SCIMLogic extends AbstractLogic<EntityTO> {

    protected static final String SCHEMAS_JSON = "schemas.json";

    protected static final Object MONITOR = new Object();

    protected static ServiceProviderConfig SERVICE_PROVIDER_CONFIG;

    protected static ResourceType USER;

    protected static ResourceType GROUP;

    protected static String SCHEMAS;

    protected static final Map<String, String> SCHEMA_MAP = new HashMap<>();

    protected final SCIMConfManager confManager;

    public SCIMLogic(final SCIMConfManager confManager) {
        this.confManager = confManager;
    }

    protected void init() {
        try {
            JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
            JsonNode tree = mapper.readTree(SCIMLogic.class.getResourceAsStream('/' + SCHEMAS_JSON));
            if (!tree.isArray()) {
                throw new IOException("JSON node is not a tree");
            }

            ArrayNode schemaArray = (ArrayNode) tree;
            SCHEMAS = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);

            for (JsonNode schema : schemaArray) {
                SCHEMA_MAP.put(schema.get("id").asText(), mapper.writeValueAsString(schema));
            }
        } catch (IOException e) {
            LOG.error("Could not parse the default schema definitions", e);
        }
    }

    @PreAuthorize("isAuthenticated()")
    public ServiceProviderConfig serviceProviderConfig(final UriBuilder uriBuilder) {
        synchronized (MONITOR) {
            if (SCHEMAS == null) {
                init();
            }

            if (SERVICE_PROVIDER_CONFIG == null) {
                SCIMConf conf = confManager.get();

                SERVICE_PROVIDER_CONFIG = new ServiceProviderConfig(
                        new Meta(
                                Resource.ServiceProviderConfig,
                                conf.getGeneralConf().getCreationDate(),
                                conf.getGeneralConf().getLastChangeDate(),
                                conf.getGeneralConf().getETagValue(),
                                uriBuilder.build().toASCIIString()),
                        new ConfigurationOption(false),
                        new BulkConfigurationOption(false,
                                conf.getGeneralConf().getBulkMaxOperations(),
                                conf.getGeneralConf().getBulkMaxPayloadSize()),
                        new FilterConfigurationOption(true, conf.getGeneralConf().getFilterMaxResults()),
                        new ConfigurationOption(true),
                        new ConfigurationOption(true),
                        new ConfigurationOption(true));
                SERVICE_PROVIDER_CONFIG.getAuthenticationSchemes().add(new AuthenticationScheme(
                        "JSON Web Token",
                        "Apache Syncope JWT authentication",
                        URI.create("http://www.rfc-editor.org/info/rfc6750"),
                        URI.create("https://syncope.apache.org/docs/"
                                + "reference-guide.html#rest-authentication-and-authorization"),
                        "oauthbearertoken",
                        true));
                SERVICE_PROVIDER_CONFIG.getAuthenticationSchemes().add(new AuthenticationScheme(
                        "HTTP Basic",
                        "Apache Syncope HTTP Basic authentication",
                        URI.create("http://www.rfc-editor.org/info/rfc2617"),
                        URI.create("https://syncope.apache.org/docs/"
                                + "reference-guide.html#rest-authentication-and-authorization"),
                        "httpbasic",
                        false));
            }
        }
        return SERVICE_PROVIDER_CONFIG;
    }

    @PreAuthorize("isAuthenticated()")
    public static List<ResourceType> resourceTypes(final UriBuilder uriBuilder) {
        synchronized (MONITOR) {
            if (USER == null) {
                USER = new ResourceType("User", "User", "/Users", "User Account", Resource.User.schema(),
                        new Meta(Resource.ResourceType,
                                null, null, null, uriBuilder.path("User").build().toASCIIString()));
                USER.getSchemaExtensions().add(new SchemaExtension(Resource.EnterpriseUser.schema(), true));
            }
            if (GROUP == null) {
                GROUP = new ResourceType("Group", "Group", "/Groups", "Group", Resource.Group.schema(),
                        new Meta(Resource.ResourceType,
                                null, null, null, uriBuilder.path("Group").build().toASCIIString()));
            }
        }

        return List.of(USER, GROUP);
    }

    @PreAuthorize("isAuthenticated()")
    public static ResourceType resourceType(final UriBuilder uriBuilder, final String type) {
        if (Resource.User.name().equals(type)) {
            resourceTypes(uriBuilder);
            return USER;
        } else if (Resource.Group.name().equals(type)) {
            resourceTypes(uriBuilder);
            return GROUP;
        } else {
            throw new IllegalArgumentException("Unsupported resource type: " + type);
        }
    }

    @PreAuthorize("isAuthenticated()")
    public String schemas() {
        synchronized (MONITOR) {
            if (SCHEMAS == null) {
                init();
            }
        }

        return SCHEMAS;
    }

    @PreAuthorize("isAuthenticated()")
    public String schema(final String schema) {
        synchronized (MONITOR) {
            if (SCHEMAS == null) {
                init();
            }
        }

        String found = SCHEMA_MAP.get(schema);
        if (found == null) {
            throw new NotFoundException("Schema " + schema + " not found");
        }

        return found;
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
