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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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

public final class WAConfUtils {

    private static class ResourceSerializer extends StdSerializer<Resource> {

        private static final long serialVersionUID = 7971411664567411958L;

        ResourceSerializer() {
            this(null);
        }

        ResourceSerializer(final Class<Resource> t) {
            super(t);
        }

        @Override
        public void serialize(final Resource value,
                final JsonGenerator jgen,
                final SerializerProvider provider) throws IOException {

            if (value instanceof ClassPathResource) {
                jgen.writeString(ResourceUtils.CLASSPATH_URL_PREFIX + value.getFilename());
            } else {
                jgen.writeString(value.getURI().toString());
            }
        }
    }

    private static final YAMLMapper YAML_MAPPER;

    static {
        YAML_MAPPER = new YAMLMapper();
        YAML_MAPPER.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        YAML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        YAML_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Resource.class, new ResourceSerializer());
        YAML_MAPPER.registerModule(module);
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
