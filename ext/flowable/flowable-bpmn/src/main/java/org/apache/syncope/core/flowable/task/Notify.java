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
package org.apache.syncope.core.flowable.task;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * General-purpose notification task for usage within workflow.
 * It requires a pre-existing Notification with category {@code CUSTOM} and result {@code SUCCESS}.
 * An {@code event} workflow variable needs to be provided as well.
 */
public class Notify extends FlowableServiceTask {

    protected final NotificationManager notificationManager;

    public Notify(final NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Override
    protected void doExecute(final DelegateExecution execution) {
        User user = execution.getVariable(FlowableRuntimeUtils.USER, User.class);
        UserTO userTO = execution.getVariable(FlowableRuntimeUtils.USER_TO, UserTO.class);
        String event = execution.getVariable(FlowableRuntimeUtils.EVENT, String.class);
        String wfExecutor = execution.getVariable(FlowableRuntimeUtils.WF_EXECUTOR, String.class);

        if (StringUtils.isNotBlank(event)) {
            notificationManager.createTasks(
                    wfExecutor,
                    OpEvent.CategoryType.CUSTOM,
                    null,
                    null,
                    event,
                    OpEvent.Outcome.SUCCESS,
                    userTO,
                    null,
                    user.getToken());
        } else {
            LOG.debug("Not sending any notification since no event was found");
        }
    }
}
