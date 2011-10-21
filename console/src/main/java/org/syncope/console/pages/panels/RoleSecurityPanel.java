/*
 *  Copyright 2011 fabio.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.console.rest.PolicyRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.types.PolicyType;

public class RoleSecurityPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(RoleSecurityPanel.class);

    private static final long serialVersionUID = -7982691107029848579L;

    @SpringBean
    private PolicyRestClient policyRestClient;

    IModel<Map<Long, String>> passwordPolicies = null;

    IModel<Map<Long, String>> accountPolicies = null;

    public <T extends AbstractAttributableTO> RoleSecurityPanel(
            final String id, final T entityTO) {

        super(id);

        setOutputMarkupId(true);

        passwordPolicies = new LoadableDetachableModel<Map<Long, String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<Long, String> load() {
                Map<Long, String> res = new HashMap<Long, String>();
                for (PasswordPolicyTO policyTO :
                        policyRestClient.getPasswordPolicies()) {
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
                for (AccountPolicyTO policyTO :
                        policyRestClient.getAccountPolicies()) {
                    res.put(policyTO.getId(), policyTO.getDescription());
                }
                return res;
            }
        };

        final WebMarkupContainer securityContainer =
                new WebMarkupContainer("security");

        securityContainer.setOutputMarkupId(true);
        add(securityContainer);

        // -------------------------------
        // Password policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> passwordPolicy =
                new AjaxDropDownChoicePanel<Long>(
                "passwordPolicy",
                getString("passwordPolicy"),
                new PropertyModel(entityTO, "passwordPolicy"),
                false);

        passwordPolicy.setChoiceRenderer(
                new PolicyRenderer(PolicyType.PASSWORD));

        passwordPolicy.setChoices(
                new ArrayList<Long>(passwordPolicies.getObject().keySet()));

        ((DropDownChoice) passwordPolicy.getField()).setNullValid(true);

        securityContainer.add(passwordPolicy);

        final AjaxCheckBoxPanel inhPasswordPolicy = new AjaxCheckBoxPanel(
                "inheritPasswordPolicy",
                getString("inheritPasswordPolicy"),
                new PropertyModel<Boolean>(entityTO, "inheritPasswordPolicy"),
                false);

        passwordPolicy.setReadOnly(inhPasswordPolicy.getModelObject());

        inhPasswordPolicy.getField().add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        passwordPolicy.setReadOnly(
                                inhPasswordPolicy.getModelObject());
                        target.add(passwordPolicy);
                    }
                });


        securityContainer.add(inhPasswordPolicy);
        // -------------------------------

        // -------------------------------
        // Account policy specification
        // -------------------------------
        final AjaxDropDownChoicePanel<Long> accountPolicy =
                new AjaxDropDownChoicePanel<Long>(
                "accountPolicy",
                getString("accountPolicy"),
                new PropertyModel(entityTO, "accountPolicy"),
                false);

        accountPolicy.setChoiceRenderer(
                new PolicyRenderer(PolicyType.ACCOUNT));

        accountPolicy.setChoices(
                new ArrayList<Long>(accountPolicies.getObject().keySet()));

        ((DropDownChoice) accountPolicy.getField()).setNullValid(true);

        securityContainer.add(accountPolicy);

        final AjaxCheckBoxPanel inhAccountPolicy = new AjaxCheckBoxPanel(
                "inheritAccountPolicy",
                getString("inheritAccountPolicy"),
                new PropertyModel<Boolean>(entityTO, "inheritAccountPolicy"),
                false);

        accountPolicy.setReadOnly(inhAccountPolicy.getModelObject());

        inhAccountPolicy.getField().add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        accountPolicy.setReadOnly(
                                inhAccountPolicy.getModelObject());
                        target.add(accountPolicy);
                    }
                });

        securityContainer.add(inhAccountPolicy);
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
                case ACCOUNT:
                    return accountPolicies.getObject().get(object);
                case PASSWORD:
                    return passwordPolicies.getObject().get(object);
                default:
                    return "";
            }
        }

        @Override
        public String getIdValue(Long object, int index) {
            return String.valueOf(object != null ? object : 0L);
        }
    };
}
