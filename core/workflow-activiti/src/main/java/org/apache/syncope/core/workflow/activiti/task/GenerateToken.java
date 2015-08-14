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
package org.apache.syncope.core.workflow.activiti.task;

import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.workflow.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenerateToken extends AbstractActivitiServiceTask {

    @Autowired
    private ConfDAO confDAO;

    @Override
    protected void doExecute(final String executionId) {
        User user = engine.getRuntimeService().
                getVariable(executionId, ActivitiUserWorkflowAdapter.USER, User.class);

        user.generateToken(
                confDAO.find("token.length", "256").getValues().get(0).getLongValue().intValue(),
                confDAO.find("token.expireTime", "60").getValues().get(0).getLongValue().intValue());

        engine.getRuntimeService().setVariable(executionId, ActivitiUserWorkflowAdapter.USER, user);
    }
}
