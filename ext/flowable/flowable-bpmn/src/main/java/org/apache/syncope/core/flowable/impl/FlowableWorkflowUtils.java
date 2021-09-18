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
package org.apache.syncope.core.flowable.impl;

import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.entity.user.User;

public class FlowableWorkflowUtils {

    public static boolean isUserIngroup(final UserTO user, final String groupName) {
        return user.getMemberships().stream().
                anyMatch(membership -> groupName != null && groupName.equals(membership.getGroupName()));
    }

    protected final DomainProcessEngine engine;

    public FlowableWorkflowUtils(final DomainProcessEngine engine) {
        this.engine = engine;
    }

    public void setUserWorkflowVariable(final String variableName, final Object value, final User user) {
        engine.getRuntimeService().
                setVariable(FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey()), variableName, value);
    }

    public Object getUserWorkflowVariable(final String variableName, final User user) {
        return engine.getRuntimeService().
                getVariable(FlowableRuntimeUtils.getWFProcInstID(engine, user.getKey()), variableName);
    }
}
