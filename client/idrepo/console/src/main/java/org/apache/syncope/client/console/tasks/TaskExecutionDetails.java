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
package org.apache.syncope.client.console.tasks;

import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Task executions.
 *
 * @param <T> actual type of task
 */
public class TaskExecutionDetails<T extends TaskTO> extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -4110576026663173545L;

    @SpringBean
    protected TaskRestClient taskRestClient;

    public TaskExecutionDetails(final T taskTO, final PageReference pageRef) {
        super();

        MultilevelPanel mlp = new MultilevelPanel("executions");
        add(mlp);

        mlp.setFirstLevel(new ExecutionsDirectoryPanel(mlp, taskTO.getKey(), taskRestClient, pageRef) {

            private static final long serialVersionUID = 5691719817252887541L;

            @Override
            protected void next(
                    final String title,
                    final MultilevelPanel.SecondLevel slevel,
                    final AjaxRequestTarget target) {

                mlp.next(title, slevel, target);
            }
        });
    }
}
