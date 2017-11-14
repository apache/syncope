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
package org.apache.syncope.client.console.pages;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.SCIMConfRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMEnterpriseUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.scim.types.EmailCanonicalType;
import org.apache.syncope.common.lib.scim.types.SCIMEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@ExtPage(label = "SCIM 2.0", icon = "fa-cloud", listEntitlement = SCIMEntitlement.SCIM_CONF_GET, priority = 100)
public class SCIMConf extends BaseExtPage {

    private static final long serialVersionUID = 9128779230455599119L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SCIMConfRestClient restClient = new SCIMConfRestClient();

    public SCIMConf(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        final BaseModal<String> modal = new BaseModal<>("modal");
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);
            }
        });
        modal.size(Modal.Size.Large);
        modal.addSubmitButton();
        body.add(modal);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        body.add(content);

        String confString = "";
        try {
            org.apache.syncope.common.lib.scim.SCIMConf conf = restClient.get();
            if (conf.getUserConf() == null) {
                conf.setUserConf(new SCIMUserConf());
            }
            if (conf.getUserConf().getName() == null) {
                conf.getUserConf().setName(new SCIMUserNameConf());
            }
            if (conf.getUserConf().getEmails().isEmpty()) {
                conf.getUserConf().getEmails().add(new SCIMComplexConf<EmailCanonicalType>());
            }

            if (conf.getEnterpriseUserConf() == null) {
                conf.setEnterpriseUserConf(new SCIMEnterpriseUserConf());
            }

            confString = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(conf);
        } catch (Exception e) {
            LOG.error("While reading SCIM configuration", e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName() : e.getMessage());
        }
        final Model<String> confModel = Model.of(confString);

        content.add(new AjaxLink<Void>("edit") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                modal.header(new ResourceModel("editConf"));
                modal.setContent(new JsonEditorPanel(modal, confModel, false, getPageReference()) {

                    private static final long serialVersionUID = -8927036362466990179L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        try {
                            restClient.set(MAPPER.readValue(
                                    confModel.getObject(), org.apache.syncope.common.lib.scim.SCIMConf.class));

                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            modal.show(false);
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While setting SCIM configuration", e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                    ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                });
                modal.show(true);
                target.add(modal);
            }
        });
    }

}
