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
package org.apache.syncope.client.console.wizards.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.PolicyRenderer;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class ResourceSecurityPanel extends WizardStep {

    private static final long serialVersionUID = -7982691107029848579L;

    private final PolicyRestClient policyRestClient = new PolicyRestClient();

    private final IModel<Map<String, String>> passwordPolicies = new LoadableDetachableModel<Map<String, String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected Map<String, String> load() {
            Map<String, String> res = new HashMap<>();
            for (AbstractPolicyTO policyTO : policyRestClient.getPolicies(PolicyType.PASSWORD)) {
                res.put(policyTO.getKey(), policyTO.getDescription());
            }
            return res;
        }
    };

    private final IModel<Map<String, String>> accountPolicies = new LoadableDetachableModel<Map<String, String>>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            Map<String, String> res = new HashMap<>();
            for (AbstractPolicyTO policyTO : policyRestClient.getPolicies(PolicyType.ACCOUNT)) {
                res.put(policyTO.getKey(), policyTO.getDescription());
            }
            return res;
        }
    };

    private final IModel<Map<String, String>> pullPolicies = new LoadableDetachableModel<Map<String, String>>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            Map<String, String> res = new HashMap<>();
            for (AbstractPolicyTO policyTO : policyRestClient.getPolicies(PolicyType.PULL)) {
                res.put(policyTO.getKey(), policyTO.getDescription());
            }
            return res;
        }
    };

    public ResourceSecurityPanel(final ResourceTO resourceTO) {
        super();
        setOutputMarkupId(true);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(true);
        add(container);

        // -------------------------------
        // Password policy selection
        // -------------------------------
        AjaxDropDownChoicePanel<String> passwordPolicy = new AjaxDropDownChoicePanel<>(
                "passwordPolicy",
                new ResourceModel("passwordPolicy", "passwordPolicy").getObject(),
                new PropertyModel<String>(resourceTO, "passwordPolicy"),
                false);
        passwordPolicy.setChoiceRenderer(new PolicyRenderer(passwordPolicies));
        passwordPolicy.setChoices(new ArrayList<>(passwordPolicies.getObject().keySet()));
        ((DropDownChoice<?>) passwordPolicy.getField()).setNullValid(true);
        container.add(passwordPolicy);
        // -------------------------------

        // -------------------------------
        // Account policy selection
        // -------------------------------
        AjaxDropDownChoicePanel<String> accountPolicy = new AjaxDropDownChoicePanel<>(
                "accountPolicy",
                new ResourceModel("accountPolicy", "accountPolicy").getObject(),
                new PropertyModel<String>(resourceTO, "accountPolicy"),
                false);
        accountPolicy.setChoiceRenderer(new PolicyRenderer(accountPolicies));
        accountPolicy.setChoices(new ArrayList<>(accountPolicies.getObject().keySet()));
        ((DropDownChoice<?>) accountPolicy.getField()).setNullValid(true);
        container.add(accountPolicy);
        // -------------------------------

        // -------------------------------
        // Pull policy selection
        // -------------------------------
        AjaxDropDownChoicePanel<String> pullPolicy = new AjaxDropDownChoicePanel<>(
                "pullPolicy",
                new ResourceModel("pullPolicy", "pullPolicy").getObject(),
                new PropertyModel<String>(resourceTO, "pullPolicy"),
                false);
        pullPolicy.setChoiceRenderer(new PolicyRenderer(pullPolicies));
        pullPolicy.setChoices(new ArrayList<>(pullPolicies.getObject().keySet()));
        ((DropDownChoice<?>) pullPolicy.getField()).setNullValid(true);
        container.add(pullPolicy);
        // -------------------------------
    }
}
