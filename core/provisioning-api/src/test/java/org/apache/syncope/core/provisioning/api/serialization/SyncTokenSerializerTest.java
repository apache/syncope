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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.UUID;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class SyncTokenSerializerTest extends AbstractTest {

    @Test
    public void SyncTokenSerializer(
            @Mock JsonGenerator jgen,
            @Mock SerializerProvider sp) throws IOException {
        SyncTokenSerializer serializer = new SyncTokenSerializer();
        SyncToken source = new SyncToken(UUID.randomUUID().toString());
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeStartObject();
        verify(jgen).writeFieldName("value");
        verify(jgen).writeEndObject();

        boolean bool = false;
        source = new SyncToken(bool);
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeBoolean(bool);

        double doubleNum = 9000.1;
        source = new SyncToken(doubleNum);
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeNumber(doubleNum);

        long longNum = 9001;
        source = new SyncToken(longNum);
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeNumber(longNum);

        int intNum = 9000;
        source = new SyncToken(intNum);
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeNumber(intNum);

        byte[] bytes = new byte[] { 9, 0, 0, 1 };
        source = new SyncToken(bytes);
        serializer.serialize(source, jgen, sp);
        verify(jgen, times(2)).writeString(anyString());
    }
}
