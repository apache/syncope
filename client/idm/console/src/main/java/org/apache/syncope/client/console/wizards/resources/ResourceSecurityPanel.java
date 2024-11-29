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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.PolicyRenderer;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ResourceSecurityPanel extends WizardStep {

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    protected PolicyRestClient policyRestClient;

    protected final IModel<Map<String, String>> passwordPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.PASSWORD).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> accountPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.ACCOUNT).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> propagationPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.PROPAGATION).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> pullPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.INBOUND).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> pushPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 9089911876466472133L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.PUSH).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName));
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
                new PropertyModel<>(resourceTO, "passwordPolicy"),
                false);
        passwordPolicy.setChoiceRenderer(new PolicyRenderer(passwordPolicies.getObject()));
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
                new PropertyModel<>(resourceTO, "accountPolicy"),
                false);
        accountPolicy.setChoiceRenderer(new PolicyRenderer(accountPolicies.getObject()));
        accountPolicy.setChoices(new ArrayList<>(accountPolicies.getObject().keySet()));
        ((DropDownChoice<?>) accountPolicy.getField()).setNullValid(true);
        container.add(accountPolicy);
        // -------------------------------

        // -------------------------------
        // Propagation policy selection
        // -------------------------------
        AjaxDropDownChoicePanel<String> propagationPolicy = new AjaxDropDownChoicePanel<>(
                "propagationPolicy",
                new ResourceModel("propagationPolicy", "propagationPolicy").getObject(),
                new PropertyModel<>(resourceTO, "propagationPolicy"),
                false);
        propagationPolicy.setChoiceRenderer(new PolicyRenderer(propagationPolicies.getObject()));
        propagationPolicy.setChoices(new ArrayList<>(propagationPolicies.getObject().keySet()));
        ((DropDownChoice<?>) propagationPolicy.getField()).setNullValid(true);
        container.add(propagationPolicy);
        // -------------------------------

        // -------------------------------
        // Pull policy selection
        // -------------------------------
        AjaxDropDownChoicePanel<String> inboundPolicy = new AjaxDropDownChoicePanel<>(
                "inboundPolicy",
                new ResourceModel("inboundPolicy", "inboundPolicy").getObject(),
                new PropertyModel<>(resourceTO, "inboundPolicy"),
                false);
        inboundPolicy.setChoiceRenderer(new PolicyRenderer(pullPolicies.getObject()));
        inboundPolicy.setChoices(new ArrayList<>(pullPolicies.getObject().keySet()));
        ((DropDownChoice<?>) inboundPolicy.getField()).setNullValid(true);
        container.add(inboundPolicy);
        // -------------------------------

        // -------------------------------
        // Push policy selection
        // -------------------------------
        AjaxDropDownChoicePanel<String> pushPolicy = new AjaxDropDownChoicePanel<>(
                "pushPolicy",
                new ResourceModel("pushPolicy", "pushPolicy").getObject(),
                new PropertyModel<>(resourceTO, "pushPolicy"),
                false);
        pushPolicy.setChoiceRenderer(new PolicyRenderer(pushPolicies.getObject()));
        pushPolicy.setChoices(new ArrayList<>(pushPolicies.getObject().keySet()));
        ((DropDownChoice<?>) pushPolicy.getField()).setNullValid(true);
        container.add(pushPolicy);
        // -------------------------------
    }
}
