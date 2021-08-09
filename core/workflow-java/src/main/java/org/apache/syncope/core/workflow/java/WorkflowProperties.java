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
package org.apache.syncope.core.workflow.java;

import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("workflow")
public class WorkflowProperties {

    private Class<? extends UserWorkflowAdapter> uwfAdapter = DefaultUserWorkflowAdapter.class;

    private Class<? extends GroupWorkflowAdapter> gwfAdapter = DefaultGroupWorkflowAdapter.class;

    private Class<? extends AnyObjectWorkflowAdapter> awfAdapter = DefaultAnyObjectWorkflowAdapter.class;

    public Class<? extends UserWorkflowAdapter> getUwfAdapter() {
        return uwfAdapter;
    }

    public void setUwfAdapter(final Class<? extends UserWorkflowAdapter> uwfAdapter) {
        this.uwfAdapter = uwfAdapter;
    }

    public Class<? extends GroupWorkflowAdapter> getGwfAdapter() {
        return gwfAdapter;
    }

    public void setGwfAdapter(final Class<? extends GroupWorkflowAdapter> gwfAdapter) {
        this.gwfAdapter = gwfAdapter;
    }

    public Class<? extends AnyObjectWorkflowAdapter> getAwfAdapter() {
        return awfAdapter;
    }

    public void setAwfAdapter(final Class<? extends AnyObjectWorkflowAdapter> awfAdapter) {
        this.awfAdapter = awfAdapter;
    }
}
