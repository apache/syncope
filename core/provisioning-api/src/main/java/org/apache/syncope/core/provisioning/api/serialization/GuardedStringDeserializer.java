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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.Field;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.EncryptorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GuardedStringDeserializer extends JsonDeserializer<GuardedString> {

    private static final Logger LOG = LoggerFactory.getLogger(GuardedStringDeserializer.class);

    @Override
    public GuardedString deserialize(final JsonParser jp, final DeserializationContext ctx)
            throws IOException {

        ObjectNode tree = jp.readValueAsTree();

        boolean readOnly = false;
        if (tree.has("readOnly")) {
            readOnly = tree.get("readOnly").asBoolean();
        }
        boolean disposed = false;
        if (tree.has("disposed")) {
            disposed = tree.get("disposed").asBoolean();
        }
        byte[] encryptedBytes = null;
        if (tree.has("encryptedBytes")) {
            encryptedBytes = Base64.decode(tree.get("encryptedBytes").asText());
        }
        String base64SHA1Hash = null;
        if (tree.has("base64SHA1Hash")) {
            base64SHA1Hash = tree.get("base64SHA1Hash").asText();
        }

        final byte[] clearBytes = EncryptorFactory.getInstance().getDefaultEncryptor().decrypt(encryptedBytes);

        GuardedString dest = new GuardedString(new String(clearBytes).toCharArray());

        try {
            Field field = GuardedString.class.getDeclaredField("readOnly");
            field.setAccessible(true);
            field.setBoolean(dest, readOnly);
        } catch (Exception e) {
            LOG.error("Could not set field value to {}", readOnly, e);
        }

        try {
            Field field = GuardedString.class.getDeclaredField("disposed");
            field.setAccessible(true);
            field.setBoolean(dest, disposed);
        } catch (Exception e) {
            LOG.error("Could not set field value to {}", disposed, e);
        }

        if (base64SHA1Hash != null) {
            try {
                Field field = GuardedString.class.getDeclaredField("base64SHA1Hash");
                field.setAccessible(true);
                field.set(dest, base64SHA1Hash);
            } catch (Exception e) {
                LOG.error("Could not set field value to {}", base64SHA1Hash, e);
            }
        }

        return dest;
    }

}
