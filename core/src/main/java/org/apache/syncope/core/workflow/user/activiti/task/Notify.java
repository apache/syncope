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
package org.apache.syncope.core.workflow.user.activiti.task;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.workflow.user.activiti.ActivitiUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * General-purpose notification task for usage within workflow.
 * It requires a pre-existing <tt>Notification</tt> with category <tt>CUSTOM</tt> and result <tt>SUCCESS</tt>.
 * An <tt>event</tt> workflow variable needs to be provided as well.
 */
@Component
public class Notify extends AbstractActivitiServiceTask {

    @Autowired
    private NotificationManager notificationManager;

    @Override
    protected void doExecute(final String executionId) {
        SyncopeUser user =
                runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.SYNCOPE_USER, SyncopeUser.class);
        UserTO userTO = runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.USER_TO, UserTO.class);
        String event = runtimeService.getVariable(executionId, ActivitiUserWorkflowAdapter.EVENT, String.class);

        if (StringUtils.isNotBlank(event)) {
            notificationManager.createTasks(
                    AuditElements.EventCategoryType.CUSTOM,
                    null,
                    null,
                    event,
                    AuditElements.Result.SUCCESS,
                    userTO,
                    null,
                    user.getToken());
        } else {
            LOG.debug("Not sending any notification since no event was found");
        }
    }

}
