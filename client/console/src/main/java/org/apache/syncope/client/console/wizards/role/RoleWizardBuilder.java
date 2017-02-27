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
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;

public class RoleWizardBuilder extends AjaxWizardBuilder<RoleWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final RoleRestClient groupRestClient = new RoleRestClient();

    /**
     * Construct.
     *
     * @param roleTO role
     * @param pageRef Caller page reference.
     */
    public RoleWizardBuilder(final RoleTO roleTO, final PageReference pageRef) {
        super(new RoleWrapper(roleTO), pageRef);
    }

    /**
     * This method has been overridden to manage asynchronous translation of FIQL string to search clases list and
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
            groupRestClient.create(modelObject.getInnerObject());
        } else {
            groupRestClient.update(modelObject.getInnerObject());
        }
        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final RoleWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        wizardModel.add(new Entitlements(modelObject.getInnerObject()));
        wizardModel.add(new Realms(modelObject.getInnerObject()));
        return wizardModel;
    }

    public static class Details extends WizardStep {

        private static final long serialVersionUID = 5514523040031722255L;

        public Details(final RoleWrapper modelObject) {
            add(new AjaxTextFieldPanel(
                    "key", "key", new PropertyModel<String>(modelObject.getInnerObject(), "key"), false).
                    setEnabled(StringUtils.isEmpty(modelObject.getInnerObject().getKey())));

            // ------------------------
            // dynMembershipCond
            // ------------------------
            add(new Accordion("dynMembershipCond", Collections.<ITab>singletonList(
                    new AbstractTab(new ResourceModel("dynMembershipCond", "Dynamic USER Membership Conditions")) {

                private static final long serialVersionUID = 1037272333056449378L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new UserSearchPanel.Builder(
                            new PropertyModel<List<SearchClause>>(modelObject, "dynClauses")).
                            required(false).build(panelId);
                }
            }), Model.of(StringUtils.isBlank(modelObject.getDynMembershipCond()) ? -1 : 0)).setOutputMarkupId(true));
            // ------------------------
        }
    }

    public static class Entitlements extends WizardStep {

        private static final long serialVersionUID = 5514523040031722256L;

        public Entitlements(final RoleTO modelObject) {
            setTitleModel(new ResourceModel("entitlements"));
            add(new AjaxPalettePanel.Builder<String>().build("entitlements",
                    new PropertyModel<List<String>>(modelObject, "entitlements") {

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
            }, new ListModel<>(new RoleRestClient().getAllAvailableEntitlements())).
                    hideLabel().setOutputMarkupId(true));
        }
    }

    public static class Realms extends WizardStep {

        private static final long serialVersionUID = 5514523040031722257L;

        public Realms(final RoleTO modelObject) {
            setTitleModel(new ResourceModel("realms"));
            add(new AjaxPalettePanel.Builder<String>().build("realms",
                    new PropertyModel<List<String>>(modelObject, "realms"),
                    new ListModel<>(
                            CollectionUtils.collect(new RealmRestClient().list(), new Transformer<RealmTO, String>() {

                                @Override
                                public String transform(final RealmTO input) {
                                    return input.getFullPath();
                                }
                            }, new ArrayList<String>()))).hideLabel().setOutputMarkupId(true));
        }
    }

}
