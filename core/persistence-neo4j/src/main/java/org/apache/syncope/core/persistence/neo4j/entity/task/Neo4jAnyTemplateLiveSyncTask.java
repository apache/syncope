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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplateLiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractAnyTemplate;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jAnyTemplateLiveSyncTask.NODE)
public class Neo4jAnyTemplateLiveSyncTask extends AbstractAnyTemplate implements AnyTemplateLiveSyncTask {

    private static final long serialVersionUID = 3517381731849788407L;

    public static final String NODE = "AnyTemplateLiveSyncTask";

    @NotNull
    @Relationship(type = Neo4jLiveSyncTask.LIVE_SYNC_TASK_TEMPLATE_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jLiveSyncTask liveSyncTask;

    @Override
    public LiveSyncTask getLiveSyncTask() {
        return liveSyncTask;
    }

    @Override
    public void setLiveSyncTask(final LiveSyncTask pullTask) {
        checkType(pullTask, Neo4jLiveSyncTask.class);
        this.liveSyncTask = (Neo4jLiveSyncTask) pullTask;
    }
}
