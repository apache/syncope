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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.client.console.widgets.JobWidget;
import org.apache.syncope.client.console.widgets.ReconciliationWidget;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.panel.Panel;

public class DashboardControlPanel extends Panel {

    private static final long serialVersionUID = -776362411304859269L;

    public DashboardControlPanel(final String id, final PageReference pageRef) {
        super(id);

        JobWidget job = new JobWidget("job", pageRef);
        MetaDataRoleAuthorizationStrategy.authorize(job, Component.RENDER,
                String.format("%s,%s,%s",
                        StandardEntitlement.NOTIFICATION_LIST,
                        StandardEntitlement.TASK_LIST,
                        StandardEntitlement.REPORT_LIST));
        add(job);

        ReconciliationWidget reconciliation = new ReconciliationWidget("reconciliation", pageRef);
        MetaDataRoleAuthorizationStrategy.authorize(job, Component.RENDER,
                String.format("%s,%s,%s",
                        StandardEntitlement.REPORT_EXECUTE,
                        StandardEntitlement.REPORT_READ,
                        StandardEntitlement.REPORT_LIST));
        add(reconciliation);
    }
}
