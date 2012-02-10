/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.pages.panels.ResourceConnConfPanel;
import org.syncope.console.pages.panels.ResourceDetailsPanel;
import org.syncope.console.pages.panels.ResourceMappingPanel;
import org.syncope.console.pages.panels.ResourceSecurityPanel;
import org.syncope.console.rest.ResourceRestClient;

/**
 * Modal window with Resource form.
 */
public class ResourceModalPage extends BaseModalPage {

    private static final long serialVersionUID = 1734415311027284221L;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    public ResourceModalPage(
            final PageReference pageref,
            final ModalWindow window,
            final ResourceTO resourceTO,
            final boolean createFlag) {

        super();

        final Form form = new Form("form");
        form.setModel(new CompoundPropertyModel(resourceTO));

        //--------------------------------
        // Resource details panel
        //--------------------------------
        form.add(new ResourceDetailsPanel("details", resourceTO, createFlag));
        //--------------------------------

        //--------------------------------
        // Resource mapping panle
        //--------------------------------
        form.add(new ResourceMappingPanel("mapping", resourceTO, createFlag));
        //--------------------------------

        //--------------------------------
        // Resource mapping panle
        //--------------------------------
        form.add(new ResourceConnConfPanel("connconf", resourceTO, createFlag));
        //--------------------------------

        //--------------------------------
        // Resource security panel
        //--------------------------------
        form.add(new ResourceSecurityPanel("security", resourceTO));
        //--------------------------------

        final AjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("submit", "submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target, final Form form) {

                final ResourceTO resourceTO =
                        (ResourceTO) form.getDefaultModelObject();

                int accountIdCount = 0;

                for (SchemaMappingTO mapping : resourceTO.getMappings()) {
                    if (mapping.isAccountid()) {
                        accountIdCount++;
                    }
                }

                if (accountIdCount == 0 || accountIdCount > 1) {
                    error(new ResourceModel(
                            "accountIdValidation", "accountIdValidation").
                            getObject());
                    target.add(feedbackPanel);
                } else {
                    try {

                        if (createFlag) {
                            resourceRestClient.create(resourceTO);
                        } else {
                            resourceRestClient.update(resourceTO);
                        }

                        ((Resources) pageref.getPage()).setModalResult(true);
                        window.close(target);

                    } catch (SyncopeClientCompositeErrorException e) {
                        LOG.error("Failuer managing resource {}", resourceTO);
                        error(new ResourceModel("error", "error").getObject()
                                + ":" + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        form.add(submit);
        add(form);

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Resources", createFlag ? "create" : "update"));
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