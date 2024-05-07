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
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ExecutionRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
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

    protected String key;

    protected final Form<?> form;

    protected final Model<Date> startAtDateModel = new Model<>();

    public StartAtTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super("startAt", pageRef);

        form = new Form<>("startAtForm");
        addInnerObject(form);

        AjaxDateTimeFieldPanel startAtDate = new AjaxDateTimeFieldPanel(
                "startAtDate", "startAtDate", startAtDateModel,
                FastDateFormat.getInstance(SyncopeConstants.DATE_PATTERNS[3]));
        form.add(startAtDate.setReadOnly(true).hideLabel());

        AjaxCheckBoxPanel startAtCheck = new AjaxCheckBoxPanel(
                "startAtCheck", "startAtCheck", new Model<>(false), false);
        startAtCheck.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(startAtDate.setModelObject(null).setReadOnly(!startAtCheck.getModelObject()));
            }
        });
        form.add(startAtCheck);

        form.add(new AjaxSubmitLink("startAt", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    getRestClient().startExecution(key, startAtDateModel.getObject());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While running task {}", key, e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
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
