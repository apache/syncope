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
package org.apache.syncope.common.services;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlEnum;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.types.AttributableType;

@Path("schemas/{kind}/{type}")
public interface SchemaService {

    @XmlEnum
    enum SchemaType {

        /**
         * Derived schema calculated based on other attributes.
         */
        DERIVED("derivedSchema"),

        /**
         * Standard schema for normal attributes to be stored within syncope.
         */
        NORMAL("schema"),

        /**
         * Virtual schema for attributes fetched from remote resources only.
         */
        VIRTUAL("virtualSchema");

        public static SchemaType fromString(String value) {
            return SchemaType.valueOf(value.toUpperCase());
        }

        // TODO remove name once CXF migration is complete
        private final String name;

        private SchemaType(String name) {
            this.name = name;
        }

        public String toSpringURL() {
            return name;
        }
    }

    /**
     * @param kind Kind for schema to be created
     * @param type Type for schema to be created
     * @param schemaTO Schema to be created
     * @return Response containing URI location for created resource.
     */
    @POST
    <T extends AbstractSchemaTO> Response create(@PathParam("kind") AttributableType kind,
            @PathParam("type") SchemaType type, T schemaTO);

    /**
     * @param kind Kind for schema to be deleted
     * @param type Type for schema to be deleted
     * @param schemaName Name of schema to be deleted
     */
    @DELETE
    @Path("{name}")
    void delete(@PathParam("kind") AttributableType kind, @PathParam("type") SchemaType type,
            @PathParam("name") String schemaName);

    /**
     * @param kind Kind for schemas to be listed
     * @param type Type for schemas to be listed
     * @return List of schemas with matching kind and type
     */
    @GET
    List<? extends AbstractSchemaTO> list(@PathParam("kind") AttributableType kind, @PathParam("type") SchemaType type);

    /**
     * @param kind Kind for schemas to be read
     * @param type Type for schemas to be read
     * @param schemaName Name of schema to be read
     * @return Returns schema with matching name, kind and type
     */
    @GET
    @Path("{name}")
    <T extends AbstractSchemaTO> T read(@PathParam("kind") AttributableType kind, @PathParam("type") SchemaType type,
            @PathParam("name") String schemaName);

    /**
     * @param kind Kind for schemas to be updated
     * @param type Type for schemas to be updated
     * @param schemaName Name of schema to be updated
     * @param schemaTO New schema to be stored
     */
    @PUT
    @Path("{name}")
    <T extends AbstractSchemaTO> void update(@PathParam("kind") AttributableType kind,
            @PathParam("type") SchemaType type, @PathParam("name") String schemaName, T schemaTO);

}
