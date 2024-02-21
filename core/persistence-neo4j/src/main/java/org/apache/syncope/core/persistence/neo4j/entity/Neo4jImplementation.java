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

import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.common.validation.ImplementationCheck;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jImplementation.NODE)
@ImplementationCheck
public class Neo4jImplementation extends AbstractProvidedKeyNode implements Implementation {

    private static final long serialVersionUID = 7668545382291708999L;

    public static final String NODE = "Implementation";

    private ImplementationEngine engine;

    private String type;

    private String body;

    @Override
    public ImplementationEngine getEngine() {
        return engine;
    }

    @Override
    public void setEngine(final ImplementationEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public void setBody(final String body) {
        this.body = body;
    }
}
