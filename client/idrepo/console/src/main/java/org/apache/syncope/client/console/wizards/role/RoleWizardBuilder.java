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
package org.apache.syncope.client.console.wizards.role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.DynRealmRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;

public class RoleWizardBuilder extends BaseAjaxWizardBuilder<RoleWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected final RoleRestClient roleRestClient;

    protected final RealmRestClient realmRestClient;

    protected final DynRealmRestClient dynRealmRestClient;

    public RoleWizardBuilder(
            final RoleTO roleTO,
            final RoleRestClient roleRestClient,
            final RealmRestClient realmRestClient,
            final DynRealmRestClient dynRealmRestClient,
            final PageReference pageRef) {

        super(new RoleWrapper(roleTO), pageRef);

        this.roleRestClient = roleRestClient;
        this.realmRestClient = realmRestClient;
        this.dynRealmRestClient = dynRealmRestClient;
    }

    /**
     * This method has been overridden to manage asynchronous translation of FIQL string to search classes list and
     * viceversa.
     *
     * @param item wizard backend item.
     * @return the current builder.
     */
    @Override
    public AjaxWizardBuilder<RoleWrapper> setItem(final RoleWrapper item) {
        return (AjaxWizardBuilder<RoleWrapper>) (item != null
                ? super.setItem(new RoleWrapper(item.getInnerObject()))
                : super.setItem(null));
    }

    @Override
    protected Serializable onApplyInternal(final RoleWrapper modelObject) {
        modelObject.fillDynamicConditions();
        if (getOriginalItem() == null || getOriginalItem().getInnerObject() == null
                || StringUtils.isBlank(getOriginalItem().getInnerObject().getKey())) {
            roleRestClient.create(modelObject.getInnerObject());
        } else {
            roleRestClient.update(modelObject.getInnerObject());
        }
        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final RoleWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        wizardModel.add(new Entitlements(modelObject.getInnerObject()));
        wizardModel.add(new Realms(modelObject.getInnerObject()));
        wizardModel.add(new DynRealms(modelObject.getInnerObject()));
        return wizardModel;
    }

    protected class Details extends WizardStep {

        private static final long serialVersionUID = 5514523040031722255L;

        public Details(final RoleWrapper modelObject) {
            add(new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME,
                    Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(modelObject.getInnerObject(), Constants.KEY_FIELD_NAME), false).
                    setEnabled(StringUtils.isEmpty(modelObject.getInnerObject().getKey())));

            // ------------------------
            // dynMembershipCond
            // ------------------------
            add(new Accordion("dynMembershipCond", List.of(
                    new AbstractTab(new ResourceModel("dynMembershipCond", "Dynamic USER Membership Conditions")) {

                private static final long serialVersionUID = 1037272333056449378L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new UserSearchPanel.Builder(
                            new PropertyModel<>(modelObject, "dynClauses"), pageRef).
                            required(true).build(panelId);
                }
            }), Model.of(StringUtils.isBlank(modelObject.getDynMembershipCond()) ? -1 : 0)).setOutputMarkupId(true));
            // ------------------------
        }
    }

    protected class Entitlements extends WizardStep {

        private static final long serialVersionUID = 5514523040031722256L;

        public Entitlements(final RoleTO modelObject) {
            setTitleModel(new ResourceModel("entitlements"));
            add(new AjaxPalettePanel.Builder<String>().build("entitlements",
                    new PropertyModel<>(modelObject, "entitlements") {

                private static final long serialVersionUID = -7809699384012595307L;

                @Override
                public List<String> getObject() {
                    return new ArrayList<>(modelObject.getEntitlements());
                }

                @Override
                public void setObject(final List<String> object) {
                    modelObject.getEntitlements().clear();
                    modelObject.getEntitlements().addAll(object);
                }
            }, new ListModel<>(roleRestClient.getAllAvailableEntitlements())).
                    hideLabel().setOutputMarkupId(true));
        }
    }

    protected class Realms extends WizardStep {

        private static final long serialVersionUID = 5514523040031722257L;

        @SuppressWarnings("unchecked")
        public Realms(final RoleTO modelObject) {
            setTitleModel(new ResourceModel("realms"));

            boolean fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);
            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(fullRealmsTree);
            settings.setShowListOnEmptyInput(fullRealmsTree);
            AbstractFieldPanel<?> realm = new AjaxSearchFieldPanel(
                    "panel", "realm", new Model<>(), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return realmRestClient.search(fullRealmsTree
                            ? RealmsUtils.buildBaseQuery()
                            : RealmsUtils.buildKeywordQuery(input)).getResult().stream().
                            map(RealmTO::getFullPath).iterator();
                }
            };
            add(new MultiFieldPanel.Builder<>(
                    new PropertyModel<>(modelObject, "realms")).build(
                    "realms",
                    "realms",
                    (FieldPanel) realm).hideLabel());
        }
    }

    protected class DynRealms extends WizardStep {

        private static final long serialVersionUID = 6846234574424462255L;

        public DynRealms(final RoleTO modelObject) {
            setTitleModel(new ResourceModel("dynRealms"));
            add(new AjaxPalettePanel.Builder<>().build("dynRealms",
                    new PropertyModel<>(modelObject, "dynRealms"),
                    new ListModel<>(dynRealmRestClient.list().stream().
                            map(DynRealmTO::getKey).collect(Collectors.toList()))).
                    hideLabel().setOutputMarkupId(true));
        }
    }
}
