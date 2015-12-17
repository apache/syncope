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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.SchemaType;

public class SchemaQuery extends AbstractBaseBean {

    private static final long serialVersionUID = -1863334226169614417L;

    public static class Builder {

        private final SchemaQuery instance = new SchemaQuery();

        public Builder type(final SchemaType type) {
            instance.setType(type);
            return this;
        }

        public Builder anyTypeClass(final String anyTypeClass) {
            if (instance.getAnyTypeClasses() == null) {
                instance.setAnyTypeClasses(new ArrayList<String>());
            }
            instance.getAnyTypeClasses().add(anyTypeClass);

            return this;
        }

        public Builder anyTypeClasses(final Collection<String> anyTypeClasses) {
            for (String anyTypeClass : anyTypeClasses) {
                anyTypeClass(anyTypeClass);
            }
            return this;
        }

        public Builder anyTypeClasses(final String... anyTypeClasses) {
            return anyTypeClasses(Arrays.asList(anyTypeClasses));
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

}
