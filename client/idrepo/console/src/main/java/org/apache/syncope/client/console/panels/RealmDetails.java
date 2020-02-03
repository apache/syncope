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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.PolicyRenderer;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmDetails extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(RealmDetails.class);

    private final IModel<Map<String, String>> accountPolicies = new LoadableDetachableModel<Map<String, String>>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return PolicyRestClient.getPolicies(PolicyType.ACCOUNT).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getDescription));
        }
    };

    private final IModel<Map<String, String>> passwordPolicies = new LoadableDetachableModel<Map<String, String>>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return PolicyRestClient.getPolicies(PolicyType.PASSWORD).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getDescription));
        }
    };

    private final IModel<List<String>> logicActions = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return ImplementationRestClient.list(IdRepoImplementationType.LOGIC_ACTIONS).stream().
                    map(EntityTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    private final LoadableDetachableModel<List<String>> resources = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return SyncopeWebApplication.get().getResourceProvider().get();
        }
    };

    private final WebMarkupContainer container;

    public RealmDetails(final String id, final RealmTO realmTO) {
        this(id, realmTO, null, true);
    }

    public RealmDetails(
            final String id,
            final RealmTO realmTO,
            final ActionsPanel<?> actionsPanel,
            final boolean unwrapped) {

        super(id);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(unwrapped);
        add(container);

        final WebMarkupContainer generics = new WebMarkupContainer("generics");
        container.add(generics.setVisible(unwrapped));

        FieldPanel<String> name = new AjaxTextFieldPanel(
                "name", "name", new PropertyModel<>(realmTO, "name"), false);
        name.addRequiredLabel();
        generics.add(name);

        FieldPanel<String> fullPath = new AjaxTextFieldPanel(
                "fullPath", "fullPath", new PropertyModel<>(realmTO, "fullPath"), false);
        fullPath.setEnabled(false);
        generics.add(fullPath);

        AjaxDropDownChoicePanel<String> accountPolicy = new AjaxDropDownChoicePanel<>(
                "accountPolicy",
                new ResourceModel("accountPolicy", "accountPolicy").getObject(),
                new PropertyModel<>(realmTO, "accountPolicy"),
                false);
        accountPolicy.setChoiceRenderer(new PolicyRenderer(accountPolicies));
        accountPolicy.setChoices(new ArrayList<>(accountPolicies.getObject().keySet()));
        ((DropDownChoice<?>) accountPolicy.getField()).setNullValid(true);
        container.add(accountPolicy);

        AjaxDropDownChoicePanel<String> passwordPolicy = new AjaxDropDownChoicePanel<>(
                "passwordPolicy",
                new ResourceModel("passwordPolicy", "passwordPolicy").getObject(),
                new PropertyModel<>(realmTO, "passwordPolicy"),
                false);
        passwordPolicy.setChoiceRenderer(new PolicyRenderer(passwordPolicies));
        passwordPolicy.setChoices(new ArrayList<>(passwordPolicies.getObject().keySet()));
        ((DropDownChoice<?>) passwordPolicy.getField()).setNullValid(true);
        container.add(passwordPolicy);

        AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                setAllowMoveAll(true).setAllowOrder(true).
                build("actions",
                        new PropertyModel<>(realmTO, "actions"),
                        new ListModel<>(logicActions.getObject()));
        actions.setOutputMarkupId(true);
        container.add(actions);

        container.add(new AjaxPalettePanel.Builder<String>().build("resources",
                new PropertyModel<>(realmTO, "resources"),
                new ListModel<>(resources.getObject())).
                setOutputMarkupId(true).
                setEnabled(!SyncopeConstants.ROOT_REALM.equals(realmTO.getName())).
                setVisible(!SyncopeConstants.ROOT_REALM.equals(realmTO.getName())));

        if (actionsPanel == null) {
            add(new Fragment("actions", "emptyFragment", this).setRenderBodyOnly(true));
        } else {
            Fragment fragment = new Fragment("actions", "actionsFragment", this);
            fragment.add(actionsPanel);
            add(fragment.setRenderBodyOnly(true));
        }
    }

    public RealmDetails setContentEnabled(final boolean enable) {
        container.setEnabled(enable);
        return this;
    }
}
