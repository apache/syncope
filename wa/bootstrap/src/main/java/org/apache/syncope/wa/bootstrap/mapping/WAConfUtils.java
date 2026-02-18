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
package org.apache.syncope.wa.bootstrap.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.dataformat.yaml.YAMLMapper;

public final class WAConfUtils {

    private static class ResourceSerializer extends StdSerializer<Resource> {

        ResourceSerializer() {
            this(null);
        }

        ResourceSerializer(final Class<Resource> t) {
            super(t);
        }

        @Override
        public void serialize(
                final Resource value,
                final JsonGenerator jgen,
                final SerializationContext ctx) throws JacksonException {

            if (value instanceof ClassPathResource) {
                jgen.writeString(ResourceUtils.CLASSPATH_URL_PREFIX + value.getFilename());
            } else {
                try {
                    jgen.writeString(value.getURI().toString());
                } catch (IOException e) {
                    throw new StreamWriteException(jgen, "During Resource serialization", e);
                }
            }
        }
    }

    private static final YAMLMapper YAML_MAPPER;

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Resource.class, new ResourceSerializer());

        YAML_MAPPER = YAMLMapper.builder().
                addModule(module).
                filterProvider(new SimpleFilterProvider().setFailOnUnknownId(false)).
                propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE).
                changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)).
                changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)).
                build();
    }

    public static Map<String, Object> asMap(final Serializable properties) {
        return FunctionUtils.doUnchecked(() -> {
            try (StringWriter writer = new StringWriter()) {
                YAML_MAPPER.writeValue(writer, properties);
                ByteArrayResource resource = new ByteArrayResource(writer.toString().getBytes(StandardCharsets.UTF_8));
                return CasCoreConfigurationUtils.loadYamlProperties(resource);
            }
        });
    }

    private WAConfUtils() {
        // private constructor for static utility class
    }
}
