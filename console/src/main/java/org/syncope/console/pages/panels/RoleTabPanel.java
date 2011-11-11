/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages.panels;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.RoleTO;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.wicket.markup.html.tree.TreeActionLinkPanel;

public class RoleTabPanel extends Panel {

    private static final long serialVersionUID = 859236186975983959L;
    @SpringBean
    protected XMLRolesReader xmlRolesReader;
    private RoleTO roleTO;

    public RoleTabPanel(final String id, final RoleTO roleTO,
            final ModalWindow window, final PageReference callerPageRef) {

        super(id);

        this.roleTO = roleTO;

        final Form form = new Form("RoleForm");

        final TreeActionLinkPanel actionLink =
                new TreeActionLinkPanel("actionLink", roleTO.getId(),
                new CompoundPropertyModel(roleTO),
                window, callerPageRef);

        this.add(actionLink);
        this.add(new Label("displayName", roleTO.getDisplayName()));

        form.setModel(new CompoundPropertyModel(roleTO));
        form.setOutputMarkupId(true);

        final RoleAttributesPanel attributesPanel =
                new RoleAttributesPanel("attributesPanel", form, roleTO);

        attributesPanel.setEnabled(false);
        
        form.add(attributesPanel);

        final WebMarkupContainer userListContainer =
                new WebMarkupContainer("userListContainer");

        userListContainer.setOutputMarkupId(true);
        userListContainer.setEnabled(true);
        userListContainer.add(new ResultSetPanel("userList",
                true, null, callerPageRef));
        userListContainer.add(new IndicatingAjaxButton(
                "search", new ResourceModel("search")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                final MembershipCond membershipCond = new MembershipCond();
                membershipCond.setRoleName(roleTO.getName());
                NodeCond cond = NodeCond.getLeafCond(membershipCond);

                userListContainer.replace(new ResultSetPanel("userList",
                        true, cond, callerPageRef));

                target.add(userListContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {
            }
        });

        form.add(userListContainer);
        add(form);
    }
}