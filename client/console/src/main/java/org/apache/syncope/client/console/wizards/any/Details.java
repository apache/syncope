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

import java.util.ArrayList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Details<T extends AnyTO> extends WizardStep {

    private static final long serialVersionUID = -8995647450549098844L;

    protected static final Logger LOG = LoggerFactory.getLogger(Details.class);

    protected final PageReference pageRef;

    private final FieldPanel<String> realm;

    public Details(
            final AnyWrapper<T> wrapper,
            final boolean templateMode,
            final boolean includeStatusPanel,
            final PageReference pageRef) {

        this.pageRef = pageRef;

        final T inner = wrapper.getInnerObject();

        if (templateMode) {
            realm = new AjaxTextFieldPanel(
                    "destinationRealm", "destinationRealm", new PropertyModel<String>(inner, "realm"), false);
            AjaxTextFieldPanel.class.cast(realm).enableJexlHelp();
        } else {
            realm = new AjaxDropDownChoicePanel<>(
                    "destinationRealm", "destinationRealm", new PropertyModel<String>(inner, "realm"), false);

            ((AjaxDropDownChoicePanel<String>) realm).setChoices(CollectionUtils.collect(
                    new RealmRestClient().list(),
                    new Transformer<RealmTO, String>() {

                @Override
                public String transform(final RealmTO input) {
                    return input.getFullPath();
                }
            }, new ArrayList<String>()));
        }
        add(realm);
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
