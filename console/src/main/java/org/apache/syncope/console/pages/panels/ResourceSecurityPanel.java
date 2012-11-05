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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.console.rest.PolicyRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.to.PolicyTO;
import org.apache.syncope.to.ResourceTO;
import org.apache.syncope.types.PolicyType;

public class ResourceSecurityPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ResourceSecurityPanel.class);

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    private PolicyRestClient policyRestClient;

    IModel<Map<Long, String>> passwordPolicies = null;

    IModel<Map<Long, String>> accountPolicies = null;

    IModel<Map<Long, String>> syncPolicies = null;

    public ResourceSecurityPanel(final String id, final ResourceTO resourceTO) {

        super(id);

        setOutputMarkupId(true);

        passwordPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<Long, String>();
                for (PolicyTO policyTO : policyRestClient.getPolicies(PolicyType.PASSWORD)) {
                    res.put(policyTO.getId(), policyTO.getDescription());
                }
                return res;
            }
        };

        accountPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = -2012833443695917883L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<Long, String>();
                for (PolicyTO policyTO : policyRestClient.getPolicies(PolicyType.ACCOUNT)) {
                    res.put(policyTO.getId(), policyTO.getDescription());
                }
                return res;
            }
        };

        syncPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = -2012833443695917883L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<Long, String>();
                for (PolicyTO policyTO : policyRestClient.getPolicies(PolicyType.SYNC)) {
                    res.put(policyTO.getId(), policyTO.getDescription());
                }
                return res;
            }
        };

        final WebMarkupContainer securityContainer = new WebMarkupContainer("security");

        securityContainer.setOutputMarkupId(true);
        add(securityContainer);

        // -------------------------------
        // Password policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> passwordPolicy = new AjaxDropDownChoicePanel<Long>("passwordPolicy",
                new ResourceModel("passwordPolicy", "passwordPolicy").getObject(), new PropertyModel<Long>(resourceTO,
                        "passwordPolicy"));

        passwordPolicy.setChoiceRenderer(new PolicyRenderer(PolicyType.PASSWORD));

        passwordPolicy.setChoices(new ArrayList<Long>(passwordPolicies.getObject().keySet()));

        ((DropDownChoice<?>) passwordPolicy.getField()).setNullValid(true);

        securityContainer.add(passwordPolicy);
        // -------------------------------

        // -------------------------------
        // Account policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> accountPolicy = new AjaxDropDownChoicePanel<Long>("accountPolicy",
                new ResourceModel("accountPolicy", "accountPolicy").getObject(), new PropertyModel<Long>(resourceTO,
                        "accountPolicy"));

        accountPolicy.setChoiceRenderer(new PolicyRenderer(PolicyType.ACCOUNT));

        accountPolicy.setChoices(new ArrayList<Long>(accountPolicies.getObject().keySet()));

        ((DropDownChoice<?>) accountPolicy.getField()).setNullValid(true);

        securityContainer.add(accountPolicy);
        // -------------------------------

        // -------------------------------
        // Sync policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> syncPolicy = new AjaxDropDownChoicePanel<Long>("syncPolicy",
                new ResourceModel("syncPolicy", "syncPolicy").getObject(), new PropertyModel<Long>(resourceTO, "syncPolicy"));

        syncPolicy.setChoiceRenderer(new PolicyRenderer(PolicyType.SYNC));

        syncPolicy.setChoices(new ArrayList<Long>(syncPolicies.getObject().keySet()));

        ((DropDownChoice<?>) syncPolicy.getField()).setNullValid(true);

        securityContainer.add(syncPolicy);
        // -------------------------------
    }

    private class PolicyRenderer extends ChoiceRenderer<Long> {

        private static final long serialVersionUID = 8060500161321947000L;

        private PolicyType type;

        public PolicyRenderer(final PolicyType type) {
            super();
            this.type = type;
        }

        @Override
        public Object getDisplayValue(final Long object) {
            switch (type) {
                case GLOBAL_ACCOUNT:
                case ACCOUNT:
                    return accountPolicies.getObject().get(object);
                case GLOBAL_PASSWORD:
                case PASSWORD:
                    return passwordPolicies.getObject().get(object);
                case GLOBAL_SYNC:
                case SYNC:
                    return syncPolicies.getObject().get(object);
                default:
                    return "";
            }
        }

        @Override
        public String getIdValue(final Long object, final int index) {
            return String.valueOf(object != null
                    ? object
                    : 0L);
        }
    };
}
