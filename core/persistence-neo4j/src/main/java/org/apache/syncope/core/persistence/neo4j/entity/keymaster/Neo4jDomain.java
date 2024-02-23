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
package org.apache.syncope.core.persistence.neo4j.entity.keymaster;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.entity.keymaster.DomainEntity;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractProvidedKeyNode;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jDomain.NODE)
public class Neo4jDomain extends AbstractProvidedKeyNode implements DomainEntity {

    private static final long serialVersionUID = -9028021617728866693L;

    public static final String NODE = "SyncopeDomain";

    private String spec;

    @Override
    public Domain get() {
        return StringUtils.isBlank(spec)
                ? null
                : POJOHelper.deserialize(spec, Domain.class);
    }

    @Override
    public void set(final Domain domain) {
        spec = POJOHelper.serialize(domain);
    }
}
