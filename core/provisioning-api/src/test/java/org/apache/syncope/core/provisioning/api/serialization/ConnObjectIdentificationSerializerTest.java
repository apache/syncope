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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectIdentification;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

public class ConnObjectIdentificationSerializerTest extends AbstractTest {

    @Test
    public void serialize(
            final @Mock JsonGenerator jgen,
            final @Mock SerializationContext ctx) {

        ConnectorObjectIdentification source = new ConnectorObjectIdentification(
                ObjectClass.ACCOUNT,
                Set.of(AttributeBuilder.build(Uid.NAME, "someuid")));

        new ConnectorObjectIdentificationSerializer().serialize(source, jgen, ctx);
        verify(jgen, times(2)).writeStartObject();
        verify(jgen).writeStringProperty(eq("objectClass"), eq(ObjectClass.ACCOUNT.getObjectClassValue()));
        verify(jgen).writeName("attributes");
        verify(jgen).writeName("value");
        verify(jgen).writeStringProperty(eq("name"), eq(Uid.NAME));
        verify(jgen, times(2)).writeStartArray();
        verify(jgen).writeString("someuid");
        verify(jgen, times(2)).writeEndArray();
        verify(jgen, times(2)).writeEndObject();
    }
}
