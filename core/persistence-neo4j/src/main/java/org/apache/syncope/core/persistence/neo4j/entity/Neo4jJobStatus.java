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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jJobStatus.NODE)
public class Neo4jJobStatus extends AbstractProvidedKeyNode implements JobStatus {

    private static final long serialVersionUID = 9061740216669505871L;

    public static final String NODE = "JobStatus";

    private static final int STATUS_MAX_LENGTH = 255;

    private String jobStatus;

    @Override
    public String getStatus() {
        return jobStatus;
    }

    @Override
    public void setStatus(final String status) {
        jobStatus = StringUtils.abbreviate(status, STATUS_MAX_LENGTH);
    }
}
