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
package org.apache.syncope.common.lib.to;

import jakarta.ws.rs.PathParam;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.ImplementationEngine;

public class ImplementationTO implements EntityTO {

    private static final long serialVersionUID = 2703397698393060586L;

    private String key;

    private ImplementationEngine engine;

    private String type;

    private String body;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public ImplementationEngine getEngine() {
        return engine;
    }

    public void setEngine(final ImplementationEngine engine) {
        this.engine = engine;
    }

    public String getType() {
        return type;
    }

    @PathParam("type")
    public void setType(final String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(engine).
                append(type).
                append(body).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ImplementationTO other = (ImplementationTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(engine, other.engine).
                append(type, other.type).
                append(body, other.body).
                build();
    }
}
