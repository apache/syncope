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
package org.apache.syncope.core.persistence.neo4j.entity;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jAnyTypeClass.NODE)
public class Neo4jAnyTypeClass extends AbstractProvidedKeyNode implements AnyTypeClass {

    private static final long serialVersionUID = 5243617517976085041L;

    public static final String NODE = "AnyTypeClass";

    public static final String ANY_TYPE_CLASS_PLAIN_REL = "ANY_TYPE_CLASS_PLAIN";

    public static final String ANY_TYPE_CLASS_DER_REL = "ANY_TYPE_CLASS_DER";

    public static final String ANY_TYPE_CLASS_VIR_REL = "ANY_TYPE_CLASS_VIR";

    @Relationship(type = ANY_TYPE_CLASS_PLAIN_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jPlainSchema> plainSchemas = new ArrayList<>();

    @Relationship(type = ANY_TYPE_CLASS_DER_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jDerSchema> derSchemas = new ArrayList<>();

    @Relationship(type = ANY_TYPE_CLASS_VIR_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jVirSchema> virSchemas = new ArrayList<>();

    @Override
    public boolean add(final PlainSchema schema) {
        checkType(schema, Neo4jPlainSchema.class);
        return this.plainSchemas.add((Neo4jPlainSchema) schema);
    }

    @Override
    public List<? extends PlainSchema> getPlainSchemas() {
        return plainSchemas;
    }

    @Override
    public boolean add(final DerSchema schema) {
        checkType(schema, Neo4jDerSchema.class);
        return this.derSchemas.add((Neo4jDerSchema) schema);
    }

    @Override
    public List<? extends DerSchema> getDerSchemas() {
        return derSchemas;
    }

    @Override
    public boolean add(final VirSchema schema) {
        checkType(schema, Neo4jVirSchema.class);
        return this.virSchemas.add((Neo4jVirSchema) schema);
    }

    @Override
    public List<? extends VirSchema> getVirSchemas() {
        return virSchemas;
    }
}
