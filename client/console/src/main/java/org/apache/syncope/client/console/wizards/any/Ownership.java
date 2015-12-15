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
package org.apache.syncope.client.console.wizards.any;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.util.ArrayList;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class Ownership extends WizardStep {

    private static final long serialVersionUID = 855618618337931784L;

    private final WebMarkupContainer ownerContainer;

    private final Fragment groupSearchFragment;

    private final Fragment userSearchFragment;

    public Ownership(final GroupHandler groupHandler) {
        super();

        final Model<Boolean> isGroupOwnership = Model.of(groupHandler.getInnerObject().getGroupOwner() != null);

        final BootstrapToggleConfig config = new BootstrapToggleConfig();
        config
                .withOnStyle(BootstrapToggleConfig.Style.info).withOffStyle(BootstrapToggleConfig.Style.warning)
                .withSize(BootstrapToggleConfig.Size.mini)
                .withOnLabel(AnyTypeKind.GROUP.name())
                .withOffLabel(AnyTypeKind.USER.name());

        add(new BootstrapToggle("ownership", new Model<Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return isGroupOwnership.getObject();
            }

            @Override
            public void setObject(final Boolean object) {

            }
        }, config) {

            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> getOffLabel() {
                return Model.of(getString("Off", null, "USER Owner"));
            }

            @Override
            protected IModel<String> getOnLabel() {
                return Model.of(getString("On", null, "GROUP Owner"));
            }

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        isGroupOwnership.setObject(!isGroupOwnership.getObject());
                        if (isGroupOwnership.getObject()) {
                            ownerContainer.addOrReplace(groupSearchFragment);
                        } else {
                            ownerContainer.addOrReplace(userSearchFragment);
                        }
                        target.add(ownerContainer);
                    }
                });
                return checkBox;
            }
        });

        ownerContainer = new WebMarkupContainer("ownerContainer");
        ownerContainer.setOutputMarkupId(true);
        this.add(ownerContainer);

        groupSearchFragment = new Fragment("search", "groupSearchFragment", this);
        final GroupSearchPanel groupSearchPanel = new GroupSearchPanel.Builder(
                new ListModel<>(new ArrayList<SearchClause>())).required(false).enableSearch().build("groupsearch");
        groupSearchFragment.add(groupSearchPanel.setRenderBodyOnly(true));

        userSearchFragment = new Fragment("search", "userSearchFragment", this);
        final AnyObjectSearchPanel userSearchPanel = new UserSearchPanel.Builder(
                new ListModel<>(new ArrayList<SearchClause>())).required(false).enableSearch().build("usersearch");
        userSearchFragment.add(userSearchPanel.setRenderBodyOnly(true));

        if (isGroupOwnership.getObject()) {
            ownerContainer.add(groupSearchFragment);
        } else {
            ownerContainer.add(userSearchFragment);
        }

        final GroupTO groupTO = GroupHandler.class.cast(groupHandler).getInnerObject();

        final AjaxTextFieldPanel userOwner
                = new AjaxTextFieldPanel("userOwner", "userOwner", new Model<String>(), false);
        userOwner.setPlaceholder("userOwner");
        userOwner.hideLabel();
        userOwner.setReadOnly(true).setOutputMarkupId(true);
        userSearchFragment.add(userOwner);

        final IndicatingAjaxLink<Void> userOwnerReset = new IndicatingAjaxLink<Void>("userOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(userOwner);
            }
        };
        userSearchFragment.add(userOwnerReset.setEnabled(false));

        final AjaxTextFieldPanel groupOwner
                = new AjaxTextFieldPanel("groupOwner", "groupOwner", new Model<String>(), false);
        groupOwner.setPlaceholder("groupOwner");
        groupOwner.hideLabel();
        groupOwner.setReadOnly(true).setOutputMarkupId(true);
        groupSearchFragment.add(groupOwner);

        final IndicatingAjaxLink<Void> groupOwnerReset = new IndicatingAjaxLink<Void>("groupOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.add(groupOwner);
            }
        };
        groupSearchFragment.add(groupOwnerReset.setEnabled(false));
    }
}
