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
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class ResourceSecurityPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    private final PolicyRestClient policyRestClient = new PolicyRestClient();

    private IModel<Map<Long, String>> passwordPolicies = null;

    private IModel<Map<Long, String>> accountPolicies = null;

    private IModel<Map<Long, String>> syncPolicies = null;

    public ResourceSecurityPanel(final String id, final IModel<ResourceTO> model) {

        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(true);
        add(container);

        setOutputMarkupId(true);

        passwordPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<>();
                for (AbstractPolicyTO policyTO : policyRestClient.getPolicies(PolicyType.PASSWORD)) {
                    res.put(policyTO.getKey(), policyTO.getDescription());
                }
                return res;
            }
        };

        accountPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = -2012833443695917883L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<>();
                for (AbstractPolicyTO policyTO : policyRestClient.getPolicies(PolicyType.ACCOUNT)) {
                    res.put(policyTO.getKey(), policyTO.getDescription());
                }
                return res;
            }
        };

        syncPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = -2012833443695917883L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<>();
                for (AbstractPolicyTO policyTO : policyRestClient.getPolicies(PolicyType.SYNC)) {
                    res.put(policyTO.getKey(), policyTO.getDescription());
                }
                return res;
            }
        };

        // -------------------------------
        // Password policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> passwordPolicy = new AjaxDropDownChoicePanel<Long>(
                "passwordPolicy",
                new ResourceModel("passwordPolicy", "passwordPolicy").getObject(),
                new PropertyModel<Long>(model, "passwordPolicy"),
                false);

        passwordPolicy.setChoiceRenderer(new PolicyRenderer(PolicyType.PASSWORD));
        passwordPolicy.setChoices(new ArrayList<>(passwordPolicies.getObject().keySet()));
        ((DropDownChoice<?>) passwordPolicy.getField()).setNullValid(true);
        container.add(passwordPolicy);
        // -------------------------------

        // -------------------------------
        // Account policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> accountPolicy = new AjaxDropDownChoicePanel<Long>(
                "accountPolicy",
                new ResourceModel("accountPolicy", "accountPolicy").getObject(),
                new PropertyModel<Long>(model, "accountPolicy"),
                false);

        accountPolicy.setChoiceRenderer(new PolicyRenderer(PolicyType.ACCOUNT));
        accountPolicy.setChoices(new ArrayList<Long>(accountPolicies.getObject().keySet()));
        ((DropDownChoice<?>) accountPolicy.getField()).setNullValid(true);
        container.add(accountPolicy);
        // -------------------------------

        // -------------------------------
        // Sync policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> syncPolicy = new AjaxDropDownChoicePanel<Long>(
                "syncPolicy",
                new ResourceModel("syncPolicy", "syncPolicy").getObject(),
                new PropertyModel<Long>(model, "syncPolicy"),
                false);

        syncPolicy.setChoiceRenderer(new PolicyRenderer(PolicyType.SYNC));
        syncPolicy.setChoices(new ArrayList<Long>(syncPolicies.getObject().keySet()));
        ((DropDownChoice<?>) syncPolicy.getField()).setNullValid(true);
        container.add(syncPolicy);
        // -------------------------------
    }

    private class PolicyRenderer extends ChoiceRenderer<Long> {

        private static final long serialVersionUID = 8060500161321947000L;

        private final PolicyType type;

        PolicyRenderer(final PolicyType type) {
            super();
            this.type = type;
        }

        @Override
        public Object getDisplayValue(final Long object) {
            switch (type) {
                case ACCOUNT:
                    return accountPolicies.getObject().get(object);
                case PASSWORD:
                    return passwordPolicies.getObject().get(object);
                case SYNC:
                    return syncPolicies.getObject().get(object);
                default:
                    return "";
            }
        }

        @Override
        public String getIdValue(final Long object, final int index) {
            return String.valueOf(object != null ? object : 0L);
        }
    };
}
