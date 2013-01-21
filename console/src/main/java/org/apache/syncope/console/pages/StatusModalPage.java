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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.List;

import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.pages.panels.StatusPanel;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class StatusModalPage extends BaseModalPage {

    private static final long serialVersionUID = 4114026480146090961L;

    @SpringBean
    private UserRestClient userRestClient;

    public StatusModalPage(final PageReference callerPageRef, final ModalWindow window,
            final AbstractAttributableTO attributable) {

        super();

        final Form form = new Form("form");
        add(form);

        final List<StatusBean> statuses = new ArrayList<StatusBean>();

        final StatusPanel statusPanel = new StatusPanel("statuspanel", attributable, statuses);
        form.add(statusPanel);

        final AjaxButton disable;
        if (attributable instanceof UserTO) {
            disable = new IndicatingAjaxButton("disable", new ResourceModel("disable", "Disable")) {

                private static final long serialVersionUID = -958724007591692537L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form form) {

                    try {
                        userRestClient.suspend(attributable.getId(), statuses);

                        if (callerPageRef.getPage() instanceof BasePage) {
                            ((BasePage) callerPageRef.getPage()).setModalResult(true);
                        }

                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error disabling resources", e);
                        error(getString("error") + ":" + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }

                @Override
                protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                    target.add(feedbackPanel);
                }
            };
        } else {
            disable = new AjaxButton("disable") {

                private static final long serialVersionUID = 5538299138211283825L;

            };
            disable.setVisible(false);
        }
        form.add(disable);

        final AjaxButton enable;
        if (attributable instanceof UserTO) {
            enable = new IndicatingAjaxButton("enable", new ResourceModel("enable", "Enable")) {

                private static final long serialVersionUID = -958724007591692537L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                    try {
                        userRestClient.reactivate(attributable.getId(), statuses);

                        ((BasePage) callerPageRef.getPage()).setModalResult(true);

                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error enabling resources", e);
                        error(getString("error") + ":" + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }

                @Override
                protected void onError(final AjaxRequestTarget target, final Form<?> form) {

                    target.add(feedbackPanel);
                }
            };
        } else {
            enable = new AjaxButton("enable") {

                private static final long serialVersionUID = 5538299138211283825L;

            };
            enable.setVisible(false);
        }
        form.add(enable);

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form form) {
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }
}
