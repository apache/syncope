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

import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

class PropagationTaskInfoSerializer extends ValueSerializer<PropagationTaskInfo> {

    @Override
    public void serialize(
            final PropagationTaskInfo propagationTaskInfo,
            final JsonGenerator jgen,
            final SerializationContext ctx) throws JacksonException {

        jgen.writeStartObject();

        jgen.writeStringProperty("key", propagationTaskInfo.getKey());

        if (propagationTaskInfo.getResource() != null) {
            jgen.writeStringProperty("resource", propagationTaskInfo.getResource().getKey());
        }
        if (propagationTaskInfo.getOperation() != null) {
            jgen.writeStringProperty("operation", propagationTaskInfo.getOperation().name());
        }
        if (propagationTaskInfo.getObjectClass() != null) {
            jgen.writeStringProperty("objectClass", propagationTaskInfo.getObjectClass().getObjectClassValue());
        }
        if (propagationTaskInfo.getAnyTypeKind() != null) {
            jgen.writeStringProperty("anyTypeKind", propagationTaskInfo.getAnyTypeKind().name());
        }
        jgen.writeStringProperty("anyType", propagationTaskInfo.getAnyType());
        jgen.writeStringProperty("entityKey", propagationTaskInfo.getEntityKey());
        jgen.writeStringProperty("connObjectKey", propagationTaskInfo.getConnObjectKey());
        jgen.writeStringProperty("oldConnObjectKey", propagationTaskInfo.getOldConnObjectKey());
        jgen.writeName("propagationData");
        jgen.writePOJO(propagationTaskInfo.getPropagationData());
        if (propagationTaskInfo.getConnector() != null) {
            jgen.writeName("connector");
            jgen.writePOJO(propagationTaskInfo.getConnector().getConnInstance().getKey());
        }
        jgen.writeName("beforeObj");
        jgen.writePOJO(propagationTaskInfo.getBeforeObj());

        jgen.writeEndObject();
    }
}
