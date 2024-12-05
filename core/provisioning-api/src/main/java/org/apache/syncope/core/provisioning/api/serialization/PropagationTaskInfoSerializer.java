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
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;

class PropagationTaskInfoSerializer extends JsonSerializer<PropagationTaskInfo> {

    @Override
    public void serialize(final PropagationTaskInfo propagationTaskInfo, final JsonGenerator jgen,
            final SerializerProvider serializerProvider) throws IOException {

        jgen.writeStartObject();

        jgen.writeStringField("key", propagationTaskInfo.getKey());

        if (propagationTaskInfo.getResource() != null) {
            jgen.writeStringField("resource", propagationTaskInfo.getResource().getKey());
        }
        if (propagationTaskInfo.getOperation() != null) {
            jgen.writeStringField("operation", propagationTaskInfo.getOperation().name());
        }
        if (propagationTaskInfo.getObjectClass() != null) {
            jgen.writeStringField("objectClass", propagationTaskInfo.getObjectClass().getObjectClassValue());
        }
        if (propagationTaskInfo.getAnyTypeKind() != null) {
            jgen.writeStringField("anyTypeKind", propagationTaskInfo.getAnyTypeKind().name());
        }
        jgen.writeStringField("anyType", propagationTaskInfo.getAnyType());
        jgen.writeStringField("entityKey", propagationTaskInfo.getEntityKey());
        jgen.writeStringField("connObjectKey", propagationTaskInfo.getConnObjectKey());
        jgen.writeStringField("oldConnObjectKey", propagationTaskInfo.getOldConnObjectKey());
        jgen.writeObjectField("propagationData", propagationTaskInfo.getPropagationData());
        if (propagationTaskInfo.getConnector() != null) {
            jgen.writeObjectField("connector", propagationTaskInfo.getConnector().getConnInstance().getKey());
        }
        jgen.writeObjectField("beforeObj", propagationTaskInfo.getBeforeObj());

        jgen.writeEndObject();
    }
}
