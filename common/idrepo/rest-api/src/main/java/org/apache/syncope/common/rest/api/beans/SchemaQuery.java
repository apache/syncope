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
package org.apache.syncope.common.rest.api.beans;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.SchemaType;

public class SchemaQuery implements Serializable {

    private static final long serialVersionUID = -1863334226169614417L;

    public static class Builder {

        private final SchemaQuery instance = new SchemaQuery();

        public Builder type(final SchemaType type) {
            instance.setType(type);
            return this;
        }

        public Builder anyTypeClass(final String anyTypeClass) {
            if (instance.getAnyTypeClasses() == null) {
                instance.setAnyTypeClasses(new ArrayList<>());
            }
            instance.getAnyTypeClasses().add(anyTypeClass);

            return this;
        }

        public Builder anyTypeClasses(final Collection<String> anyTypeClasses) {
            anyTypeClasses.forEach(this::anyTypeClass);
            return this;
        }

        public Builder anyTypeClasses(final String... anyTypeClasses) {
            return anyTypeClasses(List.of(anyTypeClasses));
        }

        public Builder keyword(final String keyword) {
            instance.setKeyword(keyword);
            return this;
        }

        public SchemaQuery build() {
            if (instance.type == null) {
                throw new IllegalArgumentException("type is required");
            }
            return instance;
        }
    }

    private SchemaType type;

    private List<String> anyTypeClasses;

    private String keyword;

    public SchemaType getType() {
        return type;
    }

    @NotNull
    @PathParam("type")
    public void setType(final SchemaType type) {
        this.type = type;
    }

    public List<String> getAnyTypeClasses() {
        return anyTypeClasses;
    }

    @QueryParam("anyTypeClass")
    public void setAnyTypeClasses(final List<String> anyTypeClasses) {
        this.anyTypeClasses = anyTypeClasses;
    }

    public String getKeyword() {
        return keyword;
    }

    @QueryParam("keyword")
    public void setKeyword(final String keyword) {
        this.keyword = keyword;
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
        SchemaQuery other = (SchemaQuery) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(anyTypeClasses, other.anyTypeClasses).
                append(keyword, other.keyword).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(anyTypeClasses).
                append(keyword).
                build();
    }
}
