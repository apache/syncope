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

import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.pages.panels.AttributesPanel;
import org.apache.syncope.console.pages.panels.DerivedAttributesPanel;
import org.apache.syncope.console.pages.panels.VirtualAttributesPanel;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

public class MembershipModalPage extends BaseModalPage {

    private static final long serialVersionUID = -4360802478081432549L;

    private AjaxButton submit;

    public MembershipModalPage(final PageReference pageRef, final ModalWindow window, final MembershipTO membershipTO,
            final boolean templateMode) {

        final Form form = new Form("MembershipForm");

        final UserTO userTO = ((UserModalPage) pageRef.getPage()).getUserTO();

        form.setModel(new CompoundPropertyModel(membershipTO));

        submit = new AjaxButton("submit", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {

                userTO.removeMembership(membershipTO);
                userTO.addMembership(membershipTO);

                ((UserModalPage) pageRef.getPage()).setUserTO(userTO);

                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        form.add(submit);

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {

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

        //--------------------------------
        // Attributes panel
        //--------------------------------
        form.add(new AttributesPanel("attributes", membershipTO, form, templateMode));
        //--------------------------------

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        form.add(new DerivedAttributesPanel("derivedAttributes", membershipTO));
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        form.add(new VirtualAttributesPanel("virtualAttributes", membershipTO, templateMode));
        //--------------------------------

        add(form);
        add(new CloseOnESCBehavior(window));
    }
}
