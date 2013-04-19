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

import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.console.pages.panels.ResourceConnConfPanel;
import org.apache.syncope.console.pages.panels.ResourceDetailsPanel;
import org.apache.syncope.console.pages.panels.ResourceMappingPanel;
import org.apache.syncope.console.pages.panels.ResourceSecurityPanel;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with Resource form.
 */
public class ResourceModalPage extends BaseModalPage {

    private static final long serialVersionUID = 1734415311027284221L;

    @SpringBean
    private ResourceRestClient restClient;

    public ResourceModalPage(final PageReference pageRef, final ModalWindow window, final ResourceTO resourceTO,
            final boolean createFlag) {

        super();

        this.add(new Label("new", StringUtils.isBlank(resourceTO.getName())
                ? new ResourceModel("new")
                : new Model("")));

        this.add(new Label("name", StringUtils.isBlank(resourceTO.getName())
                ? ""
                : resourceTO.getName()));

        final Form form = new Form("form");
        form.setModel(new CompoundPropertyModel(resourceTO));

        //--------------------------------
        // Resource details panel
        //--------------------------------
        form.add(new ResourceDetailsPanel("details", resourceTO,
                restClient.getPropagationActionsClasses(), createFlag));
        //--------------------------------

        //--------------------------------
        // Resource mapping panels
        //--------------------------------
        form.add(new ResourceMappingPanel("umapping", resourceTO, AttributableType.USER));
        form.add(new ResourceMappingPanel("rmapping", resourceTO, AttributableType.ROLE));
        //--------------------------------

        //--------------------------------
        // Resource mapping panel
        //--------------------------------
        form.add(new ResourceConnConfPanel("connconf", resourceTO, createFlag));
        //--------------------------------

        //--------------------------------
        // Resource security panel
        //--------------------------------
        form.add(new ResourceSecurityPanel("security", resourceTO));
        //--------------------------------

        final AjaxButton submit = new IndicatingAjaxButton("apply", new ResourceModel("submit", "submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ResourceTO resourceTO = (ResourceTO) form.getDefaultModelObject();

                boolean accountIdError = false;

                if (resourceTO.getUmapping() == null || resourceTO.getUmapping().getItems().isEmpty()) {
                    resourceTO.setUmapping(null);
                } else {
                    int uAccountIdCount = 0;
                    for (MappingItemTO item : resourceTO.getUmapping().getItems()) {
                        if (item.isAccountid()) {
                            uAccountIdCount++;
                        }
                    }
                    accountIdError = uAccountIdCount != 1;
                }

                if (resourceTO.getRmapping() == null || resourceTO.getRmapping().getItems().isEmpty()) {
                    resourceTO.setRmapping(null);
                } else {
                    int rAccountIdCount = 0;
                    for (MappingItemTO item : resourceTO.getRmapping().getItems()) {
                        if (item.isAccountid()) {
                            rAccountIdCount++;
                        }
                    }
                    accountIdError |= rAccountIdCount != 1;
                }

                if (accountIdError) {
                    error(getString("accountIdValidation"));
                    target.add(feedbackPanel);
                } else {
                    try {
                        if (createFlag) {
                            restClient.create(resourceTO);
                        } else {
                            restClient.update(resourceTO);
                        }

                        ((Resources) pageRef.getPage()).setModalResult(true);
                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Failure managing resource {}", resourceTO, e);
                        error(getString("error") + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, xmlRolesReader.getAllAllowedRoles("Resources",
                createFlag
                ? "create"
                : "update"));
    }

    /**
     * Generic resource event.
     */
    public static class ResourceEvent {

        /**
         * Request target.
         */
        private AjaxRequestTarget target;

        /**
         * Constructor.
         *
         * @param target request target.
         */
        public ResourceEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        /**
         * Target getter.
         *
         * @return request target.
         */
        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
