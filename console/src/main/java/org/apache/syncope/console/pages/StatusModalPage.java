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
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.pages.panels.StatusPanel;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class StatusModalPage extends BaseModalPage {

    private static final long serialVersionUID = 4114026480146090961L;

    @SpringBean
    private UserRestClient userRestClient;

    public StatusModalPage(final PageReference pageRef, final ModalWindow window, final UserTO userTO) {

        super();

        final Form form = new Form("form");
        add(form);

        final List<StatusBean> statuses = new ArrayList<StatusBean>();

        final StatusPanel statusPanel = new StatusPanel("statuspanel", userTO, statuses);
        form.add(statusPanel);

        final AjaxButton disable = new ClearIndicatingAjaxButton("disable",
                new ResourceModel("disable", "Disable"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form form) {
                try {
                    userRestClient.suspend(userTO.getId(), statuses);

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
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

        final AjaxButton enable = new ClearIndicatingAjaxButton("enable",
                new ResourceModel("enable", "Enable"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    userRestClient.reactivate(userTO.getId(), statuses);

                    ((BasePage) pageRef.getPage()).setModalResult(true);

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

        form.add(disable);
        form.add(enable);
    }
}
