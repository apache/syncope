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

import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ExecutionRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

public abstract class StartAtTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    protected String key = null;

    protected final Form<?> form;

    protected final Model<Date> startAtDateModel = new Model<>();

    public StartAtTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super("startAt", pageRef);

        form = new Form<>("startAtForm");
        addInnerObject(form);

        final DateTimeFieldPanel startAtDate = new DateTimeFieldPanel(
                "startAtDate", "startAtDate", startAtDateModel, SyncopeConstants.DATE_PATTERNS[3]);

        startAtDate.setReadOnly(true).hideLabel();
        form.add(startAtDate);

        final AjaxCheckBoxPanel startAtCheck = new AjaxCheckBoxPanel(
                "startAtCheck", "startAtCheck", new Model<>(false), false);

        form.add(startAtCheck);

        startAtCheck.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(startAtDate.setModelObject(null).setReadOnly(!startAtCheck.getModelObject()));
            }
        });

        form.add(new AjaxSubmitLink("startAt", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    getRestClient().startExecution(key, startAtDateModel.getObject());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                    target.add(container);
                } catch (SyncopeClientException e) {
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                            getMessage());
                    LOG.error("While running task {}", key, e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }

    public void setExecutionDetail(final String key, final String header, final AjaxRequestTarget target) {
        this.key = key;
        setHeader(target, header);
    }

    protected abstract ExecutionRestClient getRestClient();
}
