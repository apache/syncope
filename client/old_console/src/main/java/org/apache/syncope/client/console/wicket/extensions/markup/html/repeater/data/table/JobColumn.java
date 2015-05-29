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
package org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table;

import org.apache.syncope.client.console.panels.RuntimePanel;
import org.apache.syncope.client.console.rest.JobRestClient;
import org.apache.syncope.common.lib.to.AbsractTaskTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobColumn<T, S> extends AbstractColumn<T, S> {

    private static final long serialVersionUID = 7955560320949560725L;

    protected static final Logger LOG = LoggerFactory.getLogger(JobColumn.class);

    private final PageReference pageRef;

    private RuntimePanel panel;

    private final JobRestClient jobRestClient;

    public JobColumn(final IModel<String> displayModel, final S sortProperty, final PageReference pageRef,
            final JobRestClient jobRestClient) {
        super(displayModel, sortProperty);
        this.pageRef = pageRef;
        this.jobRestClient = jobRestClient;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> model) {
        Long jobId = null;
        if (model.getObject() instanceof AbstractTaskTO) {
            jobId = ((AbstractTaskTO) model.getObject()).getId();
        } else if (model.getObject() instanceof ReportTO) {
            jobId = ((ReportTO) model.getObject()).getId();
        }
        if (jobId != null) {
            panel = new RuntimePanel(componentId, model, pageRef, jobId, jobRestClient);
            startPolling(10);
            item.add(panel);
        }
    }

    public void startPolling(final int seconds) {
        AbstractAjaxTimerBehavior timer = new AbstractAjaxTimerBehavior(Duration.seconds(seconds)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                panel.refresh();
                target.add(panel);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getExtraParameters().put("pollingTimeout", "true");
            }

        };

        panel.setTimer(timer);

    }

}
