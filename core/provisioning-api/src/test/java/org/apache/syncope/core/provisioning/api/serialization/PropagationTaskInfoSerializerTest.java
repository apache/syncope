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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.UUID;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class PropagationTaskInfoSerializerTest extends AbstractTest {

    @Test
    public void propagationTaskInfoSerializer(final @Mock JsonGenerator jgen, final @Mock SerializerProvider sp)
            throws IOException {

        PropagationTaskInfoSerializer serializer = new PropagationTaskInfoSerializer();
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn("resource-ldap");
        PropagationData propagationData = mock(PropagationData.class);
        PropagationTaskInfo source =
                new PropagationTaskInfo(resource,
                        ResourceOperation.UPDATE,
                        ObjectClass.ACCOUNT,
                        AnyTypeKind.USER,
                        AnyTypeKind.USER.name(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        propagationData);
        source.setKey(UUID.randomUUID().toString());
        Connector connector = mock(Connector.class);
        ConnInstance connInstance = mock(ConnInstance.class);
        when(connInstance.getKey()).thenReturn(UUID.randomUUID().toString());
        when(connector.getConnInstance()).thenReturn(connInstance);
        source.setConnector(connector);
        serializer.serialize(source, jgen, sp);
        verify(jgen).writeStartObject();
        verify(jgen).writeStringField("key", source.getKey());
        verify(jgen).writeStringField("anyType", source.getAnyType());
        verify(jgen).writeStringField("entityKey", source.getEntityKey());
        verify(jgen).writeStringField("connObjectKey", source.getConnObjectKey());
        verify(jgen).writeStringField("oldConnObjectKey", source.getOldConnObjectKey());
        verify(jgen).writeObjectField("propagationData", source.getPropagationData());
        verify(jgen).writeObjectField("connector", source.getConnector().getConnInstance().getKey());
        verify(jgen).writeEndObject();
    }
}
