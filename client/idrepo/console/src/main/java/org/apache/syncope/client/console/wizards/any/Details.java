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

import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Details<T extends AnyTO> extends WizardStep {

    private static final long serialVersionUID = -8995647450549098844L;

    protected static final Logger LOG = LoggerFactory.getLogger(Details.class);

    protected static List<RealmTO> getRealmsFromLinks(final List<AbstractLink> realmLinks) {
        return realmLinks.stream().
                map(Component::getDefaultModelObject).
                filter(RealmTO.class::isInstance).
                map(RealmTO.class::cast).
                toList();
    }

    @SpringBean
    protected RealmRestClient realmRestClient;

    protected final PageReference pageRef;

    protected final FieldPanel<String> realm;

    public Details(
            final AnyWrapper<T> wrapper,
            final boolean templateMode,
            final boolean includeStatusPanel,
            final PageReference pageRef) {

        this.pageRef = pageRef;
        final List<String> authRealms = SyncopeConsoleSession.get().getAuthRealms();
        final T inner = wrapper.getInnerObject();
        final Fragment fragment;

        if (templateMode) {
            realm = new AjaxTextFieldPanel(
                    "destinationRealm", "destinationRealm", new PropertyModel<>(inner, "realm"), false);
            AjaxTextFieldPanel.class.cast(realm).enableJexlHelp();
            fragment = new Fragment("realmsFragment", "realmsTemplateFragment", this);
        } else {
            boolean fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);
            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(fullRealmsTree);
            settings.setShowListOnEmptyInput(fullRealmsTree);

            realm = new AjaxSearchFieldPanel("destinationRealm", "destinationRealm",
                    new PropertyModel<>(inner, "realm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (pageRef.getPage() instanceof Realms
                            ? getRealmsFromLinks(Realms.class.cast(pageRef.getPage()).getRealmChoicePanel().getLinks())
                            : (fullRealmsTree
                                    ? realmRestClient.search(RealmsUtils.buildBaseQuery())
                                    : realmRestClient.search(RealmsUtils.buildKeywordQuery(input))).getResult()).
                            stream().map(RealmTO::getFullPath).
                        filter(fullPath -> authRealms.stream().anyMatch(
                        authRealm -> fullPath.startsWith(authRealm))).iterator();
                }
            };

            fragment = new Fragment("realmsFragment", "realmsSearchFragment", this);
        }
        fragment.addOrReplace(realm);
        addOrReplace(fragment);
        add(getGeneralStatusInformation("generalStatusInformation", inner).
                setEnabled(includeStatusPanel).setVisible(includeStatusPanel).setRenderBodyOnly(true));

    }

    public Details<T> disableRealmSpecification() {
        this.realm.setReadOnly(true);
        return this;
    }

    protected AnnotatedBeanPanel getGeneralStatusInformation(final String id, final T anyTO) {
        return new AnnotatedBeanPanel(id, anyTO);
    }
}
