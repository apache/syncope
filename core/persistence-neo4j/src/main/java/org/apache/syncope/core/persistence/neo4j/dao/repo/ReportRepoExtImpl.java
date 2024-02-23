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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jReport;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jReportExec;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class ReportRepoExtImpl implements ReportRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    public ReportRepoExtImpl(final Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jReport.class).ifPresent(this::delete);
    }

    @Override
    public void delete(final Report report) {
        report.getExecs().forEach(exec -> neo4jTemplate.deleteById(exec.getKey(), Neo4jReportExec.class));

        neo4jTemplate.deleteById(report.getKey(), Neo4jReport.class);
    }
}
