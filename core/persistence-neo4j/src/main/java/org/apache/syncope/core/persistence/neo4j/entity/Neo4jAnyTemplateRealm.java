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

import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jAnyTemplateRealm.NODE)
public class Neo4jAnyTemplateRealm extends AbstractAnyTemplate implements AnyTemplateRealm {

    private static final long serialVersionUID = 1863029633568957907L;

    public static final String NODE = "AnyTemplateRealm";

    public static final String REALM_ANY_TEMPLATE_REL = "REALM_ANY_TEMPLATE";

    @Relationship(type = REALM_ANY_TEMPLATE_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm realm;

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, Neo4jRealm.class);
        this.realm = (Neo4jRealm) realm;
    }
}
