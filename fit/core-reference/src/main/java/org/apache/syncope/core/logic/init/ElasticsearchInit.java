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
package org.apache.syncope.core.logic.init;

import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.logic.TaskLogic;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ElasticsearchInit {

    private static final String ES_REINDEX = "org.apache.syncope.core.provisioning.java.job.ElasticsearchReindex";

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private TaskLogic taskLogic;

    @Transactional
    public void init() {
        Implementation reindex = implementationDAO.findByType(IdRepoImplementationType.TASKJOB_DELEGATE).
                stream().
                filter(impl -> impl.getEngine() == ImplementationEngine.JAVA && ES_REINDEX.equals(impl.getBody())).
                findAny().
                orElseGet(() -> {
                    Implementation impl = entityFactory.newEntity(Implementation.class);
                    impl.setKey(ES_REINDEX);
                    impl.setEngine(ImplementationEngine.JAVA);
                    impl.setType(IdRepoImplementationType.TASKJOB_DELEGATE);
                    impl.setBody(ES_REINDEX);
                    return implementationDAO.save(impl);
                });

        SchedTaskTO task = new SchedTaskTO();
        task.setJobDelegate(reindex.getKey());
        task.setName("Elasticsearch Reindex");
        task = taskLogic.createSchedTask(TaskType.SCHEDULED, task);

        taskLogic.execute(task.getKey(), null, false);
    }
}
