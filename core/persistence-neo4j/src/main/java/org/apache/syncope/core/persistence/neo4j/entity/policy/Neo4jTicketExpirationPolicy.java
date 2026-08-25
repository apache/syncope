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
package org.apache.syncope.core.persistence.neo4j.entity.policy;

import org.apache.syncope.common.lib.policy.TicketExpirationPolicyConf;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.converters.TicketExpirationPolicyConfConverter;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jTicketExpirationPolicy.NODE)
public class Neo4jTicketExpirationPolicy extends Neo4jPolicy implements TicketExpirationPolicy {

    private static final long serialVersionUID = -4190607009908888884L;

    public static final String NODE = "TicketExpirationPolicy";

    @ConvertWith(converter = TicketExpirationPolicyConfConverter.class)
    private TicketExpirationPolicyConf jsonConf;

    @Override
    public TicketExpirationPolicyConf getConf() {
        return jsonConf;
    }

    @Override
    public void setConf(final TicketExpirationPolicyConf conf) {
        jsonConf = conf;
    }
}
