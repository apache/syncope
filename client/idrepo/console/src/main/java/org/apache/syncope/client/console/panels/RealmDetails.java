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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmPolicyProvider;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxGridFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.model.util.MapModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmDetails extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(RealmDetails.class);

    protected final IModel<List<String>> availableAnyTypeClasses = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return anyTypeClassRestClient.list().stream().
                    map(AnyTypeClassTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final IModel<List<String>> logicActions = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdRepoImplementationType.LOGIC_ACTIONS).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final LoadableDetachableModel<List<String>> resources = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return SyncopeWebApplication.get().getResourceProvider().getForRealms();
        }
    };

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected RealmPolicyProvider realmPolicyProvider;

    protected final WebMarkupContainer container;

    public RealmDetails(final String id, final RealmTO realmTO) {
        this(id, realmTO, null, true);
    }

    public RealmDetails(
            final String id,
            final RealmTO realmTO,
            final ActionsPanel<RealmTO> actionsPanel,
            final boolean unwrapped) {

        super(id);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(unwrapped);
        add(container);

        WebMarkupContainer generics = new WebMarkupContainer("generics");
        container.add(generics);

        FieldPanel<String> name = new AjaxTextFieldPanel(
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME,
                new PropertyModel<>(realmTO, Constants.NAME_FIELD_NAME), false);
        generics.add(name.addRequiredLabel().setVisible(unwrapped));

        FieldPanel<String> fullPath = new AjaxTextFieldPanel(
                "fullPath", "fullPath", new PropertyModel<>(realmTO, "fullPath"), false);
        fullPath.setEnabled(false);
        generics.add(fullPath.setVisible(unwrapped));

        if (unwrapped) {
            generics.add(new AjaxPalettePanel.Builder<String>().
                    setAllowOrder(true).build(
                    "anyTypeClasses",
                    new PropertyModel<>(realmTO, "anyTypeClasses"),
                    new ListModel<>(availableAnyTypeClasses.getObject())).setOutputMarkupId(true));
        } else {
            generics.add(new AjaxTextFieldPanel(
                    "anyTypeClasses", "anyTypeClasses", Model.of(realmTO.getAnyTypeClasses().toString())));
        }

        Map<String, String> attrs = realmTO.getPlainAttrs().stream().
                sorted(Comparator.comparing(Attr::getSchema)).
                collect(Collectors.toMap(Attr::getSchema, attr -> attr.getValues().toString()));
        container.add(new AjaxGridFieldPanel<>(
                "plainAttrs",
                "plainAttrs",
                new MapModel<>(attrs),
                true).setOutputMarkupPlaceholderTag(true).setVisible(!unwrapped));

        RepeatingView policies = new RepeatingView("policies");
        realmPolicyProvider.add(realmTO, policies);
        container.add(policies);

        if (unwrapped) {
            container.add(new AjaxPalettePanel.Builder<String>().
                    setAllowMoveAll(true).setAllowOrder(true).build(
                    "actions",
                    new PropertyModel<>(realmTO, "actions"),
                    new ListModel<>(logicActions.getObject())).
                    setOutputMarkupId(true));
        } else {
            container.add(new AjaxTextFieldPanel(
                    "actions", "actions", Model.of(realmTO.getActions().toString())));
        }

        if (unwrapped) {
            container.add(new AjaxPalettePanel.Builder<String>().build(
                    "resources",
                    new PropertyModel<>(realmTO, "resources"),
                    new ListModel<>(resources.getObject())).
                    setOutputMarkupId(true).
                    setEnabled(!SyncopeConstants.ROOT_REALM.equals(realmTO.getName())).
                    setVisible(!SyncopeConstants.ROOT_REALM.equals(realmTO.getName())));
        } else {
            container.add(new AjaxTextFieldPanel(
                    "resources", "resources", Model.of(realmTO.getResources().toString())));
        }

        if (actionsPanel == null) {
            add(new Fragment("actionsPanel", "emptyFragment", this).setRenderBodyOnly(true));
        } else {
            Fragment fragment = new Fragment("actionsPanel", "actionsFragment", this);
            fragment.add(actionsPanel);
            add(fragment.setRenderBodyOnly(true));
        }
    }

    public RealmDetails setContentEnabled(final boolean enable) {
        container.setEnabled(enable);
        return this;
    }
}
