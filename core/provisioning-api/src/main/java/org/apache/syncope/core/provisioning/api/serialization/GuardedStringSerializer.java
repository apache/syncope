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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.lang.reflect.Field;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.EncryptorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GuardedStringSerializer extends JsonSerializer<GuardedString> {

    private static final Logger LOG = LoggerFactory.getLogger(GuardedStringSerializer.class);

    @Override
    public void serialize(final GuardedString source, final JsonGenerator jgen, final SerializerProvider sp)
            throws IOException {

        jgen.writeStartObject();

        boolean readOnly = false;
        try {
            Field field = GuardedString.class.getDeclaredField("readOnly");
            field.setAccessible(true);
            readOnly = field.getBoolean(source);
        } catch (Exception e) {
            LOG.error("Could not get field value", e);
        }
        jgen.writeBooleanField("readOnly", readOnly);

        boolean disposed = false;
        try {
            Field field = GuardedString.class.getDeclaredField("disposed");
            field.setAccessible(true);
            disposed = field.getBoolean(source);
        } catch (Exception e) {
            LOG.error("Could not get field value", e);
        }
        jgen.writeBooleanField("disposed", disposed);

        byte[] encryptedBytes =
                EncryptorFactory.getInstance().getDefaultEncryptor().encrypt(SecurityUtil.decrypt(source).getBytes());
        jgen.writeStringField("encryptedBytes", Base64.encode(encryptedBytes));

        String base64SHA1Hash = null;
        try {
            Field field = GuardedString.class.getDeclaredField("base64SHA1Hash");
            field.setAccessible(true);
            base64SHA1Hash = field.get(source).toString();
        } catch (Exception e) {
            LOG.error("Could not get field value", e);
        }
        if (base64SHA1Hash != null) {
            jgen.writeStringField("base64SHA1Hash", base64SHA1Hash);
        }

        jgen.writeEndObject();
    }

}
