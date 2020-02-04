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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.common.security.GuardedString;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class GuardedStringSerializerTest extends AbstractTest {

    private static final String READONLY = "readOnly";

    private static final String DISPOSED = "disposed";

    private static final String ENCRYPTED_BYTES = "encryptedBytes";

    private static final String BASE64_SHA1_HASH = "base64SHA1Hash";
    
    private final GuardedStringSerializer serializer = new GuardedStringSerializer();
    
    @Test
    public void serialize(
            @Mock JsonGenerator jgen, 
            @Mock SerializerProvider sp) throws IOException {
        serializer.serialize(new GuardedString(), jgen, sp);
        verify(jgen).writeBooleanField(READONLY, false);
        verify(jgen).writeBooleanField(DISPOSED, false);
        verify(jgen).writeStringField(eq(ENCRYPTED_BYTES), anyString());
        verify(jgen).writeStringField(eq(BASE64_SHA1_HASH), anyString());
        verify(jgen).writeEndObject();
    }
}
