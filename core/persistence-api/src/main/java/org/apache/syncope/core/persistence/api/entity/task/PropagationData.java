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
package org.apache.syncope.core.persistence.api.entity.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Set;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;

public class PropagationData implements Serializable {

    private static final long serialVersionUID = -6193849782964810456L;

    public static class Builder {

        private final PropagationData instance;

        public Builder() {
            instance = new PropagationData();
        }

        public Builder attributes(final Set<Attribute> attributes) {
            instance.setAttributes(attributes);
            return this;
        }

        public Builder attributeDeltas(final Set<AttributeDelta> attributeDeltas) {
            instance.setAttributeDeltas(attributeDeltas);
            return this;
        }

        public PropagationData build() {
            return instance;
        }
    }

    private Set<Attribute> attributes;

    private Set<AttributeDelta> attributeDeltas;

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public Set<AttributeDelta> getAttributeDeltas() {
        return attributeDeltas;
    }

    public void setAttributeDeltas(final Set<AttributeDelta> attributeDeltas) {
        this.attributeDeltas = attributeDeltas;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return attributes == null && attributeDeltas == null;
    }
}
