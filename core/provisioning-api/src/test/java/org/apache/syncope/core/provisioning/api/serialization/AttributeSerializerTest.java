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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.List;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class AttributeSerializerTest extends AbstractTest {

    @Test
    public void serialize(
            final @Mock Attribute source,
            final @Mock JsonGenerator jgen,
            final @Mock SerializerProvider sp)
            throws IOException {

        AttributeSerializer serializer = new AttributeSerializer();
        when(source.getValue()).thenReturn(null);
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeStartObject();
        verify(jgen).writeFieldName("value");
        verify(jgen).writeNull();

        when(source.getValue()).thenAnswer(ic -> List.of(new GuardedString()));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeObject(any(GuardedString.class));

        when(source.getValue()).thenAnswer(ic -> List.of(9000));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeNumber(anyInt());

        when(source.getValue()).thenAnswer(ic -> List.of(9000L));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeNumber(anyLong());

        when(source.getValue()).thenAnswer(ic -> List.of(9000.1));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeNumber(anyDouble());

        when(source.getValue()).thenAnswer(ic -> List.of(Boolean.TRUE));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeBoolean(anyBoolean());

        when(source.getValue()).thenAnswer(ic -> List.of(new byte[] { 9, 0, 0, 0 }));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeString(anyString());

        when(source.getValue()).thenAnswer(ic -> List.of("test"));
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeString(eq("test"));
    }
}
