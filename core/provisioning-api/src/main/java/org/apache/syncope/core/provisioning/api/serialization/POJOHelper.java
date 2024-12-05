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
package org.apache.syncope.core.provisioning.api.serialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for serialization and deserialization of configuration objects (POJOs) in JSON.
 */
public final class POJOHelper {

    private static final Logger LOG = LoggerFactory.getLogger(POJOHelper.class);

    private static final JsonMapper MAPPER;

    static {
        SimpleModule pojoModule = new SimpleModule("POJOModule", new Version(1, 0, 0, null, null, null));
        pojoModule.addSerializer(GuardedString.class, new GuardedStringSerializer());
        pojoModule.addSerializer(Attribute.class, new AttributeSerializer());
        pojoModule.addSerializer(AttributeDelta.class, new AttributeDeltaSerializer());
        pojoModule.addSerializer(SyncToken.class, new SyncTokenSerializer());
        pojoModule.addSerializer(PropagationTaskInfo.class, new PropagationTaskInfoSerializer());
        pojoModule.addDeserializer(GuardedString.class, new GuardedStringDeserializer());
        pojoModule.addDeserializer(Attribute.class, new AttributeDeserializer());
        pojoModule.addDeserializer(AttributeDelta.class, new AttributeDeltaDeserializer());
        pojoModule.addDeserializer(SyncToken.class, new SyncTokenDeserializer());

        MAPPER = JsonMapper.builder().
                addModule(pojoModule).
                addModule(new JavaTimeModule()).
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).
                build();
    }

    public static String serialize(final Object object) {
        String result = null;

        try {
            result = MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            LOG.error("During serialization", e);
        }

        return result;
    }

    public static <T extends Object> String serialize(final T object, final TypeReference<T> reference) {
        String result = null;

        try {
            result = MAPPER.writerFor(reference).writeValueAsString(object);
        } catch (Exception e) {
            LOG.error("During serialization", e);
        }

        return result;
    }

    public static <T extends Object> T deserialize(final String serialized, final Class<T> reference) {
        T result = null;

        try {
            result = MAPPER.readValue(serialized, reference);
        } catch (Exception e) {
            LOG.error("During deserialization", e);
        }

        return result;
    }

    public static <T extends Object> T deserialize(final String serialized, final TypeReference<T> reference) {
        T result = null;

        try {
            result = MAPPER.readValue(serialized, reference);
        } catch (Exception e) {
            LOG.error("During deserialization", e);
        }

        return result;
    }

    public static <T extends Object> T convertValue(final Object value, final Class<T> reference) {
        T result = null;

        try {
            result = MAPPER.convertValue(value, reference);
        } catch (Exception e) {
            LOG.error("During conversion", e);
        }

        return result;
    }

    private POJOHelper() {
    }
}
