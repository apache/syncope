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

import org.apache.syncope.client.console.rest.JobRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.markup.html.panel.Panel;

public class RuntimePanel extends Panel {

    private static final long serialVersionUID = -9002724127542172464L;

    private boolean latestStatus;

    private Fragment fragmentStop, fragmentSpinner;

    public AbstractAjaxTimerBehavior timer;

    private final PageReference pageRef;

    private final long jobId;

    private final JobRestClient jobRestClient;

    public RuntimePanel(final String componentId, final IModel<?> model, final PageReference pageRef, final long jobId,
            final JobRestClient jobRestClient) {
        super(componentId, model);
        this.pageRef = pageRef;
        this.jobId = jobId;
        this.jobRestClient = jobRestClient;
        latestStatus = false;
        this.refresh();

    }

    public final void refresh() {
        boolean currentStatus = jobRestClient.isJobRunning(jobId);
        if (currentStatus && !latestStatus) {
            setRunning();
        } else if (!currentStatus) {
            setNotRunning();
        }
        latestStatus = currentStatus;
    }

    public void setRunning() {
        fragmentStop = new Fragment("panelStop", "fragmentStop", this);
        fragmentStop.addOrReplace(new ClearIndicatingAjaxLink<Void>("stopLink", pageRef) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                jobRestClient.stopJob(jobId);
                this.setEnabled(false);
                target.add(this);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return "";
            }
        });
        addOrReplace(fragmentStop);
        fragmentSpinner = new Fragment("panelSpinner", "fragmentSpinner", this);
        addOrReplace(fragmentSpinner);
    }

    public void setNotRunning() {
        fragmentStop = new Fragment("panelStop", "emptyFragment", this);
        addOrReplace(fragmentStop);
        fragmentSpinner = new Fragment("panelSpinner", "emptyFragment", this);
        addOrReplace(fragmentSpinner);
    }

    public void setTimer(AbstractAjaxTimerBehavior timer) {
        if (this.timer != null) {
            remove(this.timer);
        }
        this.timer = timer;
        this.add(this.timer);
    }

}
