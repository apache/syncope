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
package org.apache.syncope.client.console.policies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.ComposablePolicy;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class PolicyRuleWizardBuilder
        extends AjaxWizardBuilder<PolicyRuleDirectoryPanel.PolicyRuleWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final PolicyRestClient restClient = new PolicyRestClient();

    private final String policy;

    private final PolicyType type;

    public PolicyRuleWizardBuilder(
            final String policy,
            final PolicyType type,
            final PolicyRuleDirectoryPanel.PolicyRuleWrapper reportlet,
            final PageReference pageRef) {
        super(reportlet, pageRef);
        this.policy = policy;
        this.type = type;
    }

    @Override
    protected Serializable onApplyInternal(final PolicyRuleDirectoryPanel.PolicyRuleWrapper modelObject) {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(modelObject.getConf());
        wrapper.setPropertyValue("name", modelObject.getName());

        AbstractPolicyTO policyTO = restClient.getPolicy(policy);

        final ComposablePolicy<RuleConf> composable;
        if (policyTO instanceof ComposablePolicy) {
            composable = (ComposablePolicy<RuleConf>) policyTO;
        } else {
            throw new IllegalStateException("Non composable policy");
        }

        if (modelObject.isNew()) {
            composable.getRuleConfs().add(modelObject.getConf());
        } else {
            composable.getRuleConfs().removeAll(composable.getRuleConfs().stream().
                    filter(conf -> conf.getName().equals(modelObject.getOldName())).collect(Collectors.toList()));
            composable.getRuleConfs().add(modelObject.getConf());
        }

        restClient.updatePolicy(policyTO);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(
            final PolicyRuleDirectoryPanel.PolicyRuleWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Configuration(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        public Profile(final PolicyRuleDirectoryPanel.PolicyRuleWrapper rule) {

            final AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    "name", "rule", new PropertyModel<>(rule, "name"), false);
            name.addRequiredLabel();
            add(name);

            final AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>(
                    "configuration", "configuration", new PropertyModel<String>(rule, "conf") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return rule.getConf() == null ? null : rule.getConf().getClass().getName();
                }

                @Override
                public void setObject(final String object) {
                    RuleConf conf = null;

                    try {
                        conf = RuleConf.class.cast(Class.forName(object).newInstance());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        LOG.warn("Error retrieving reportlet configuration instance", e);
                    }

                    rule.setConf(conf);
                }
            });

            List<String> choices;
            switch (type) {
                case ACCOUNT:
                    choices = new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getAccountRules());
                    break;

                case PASSWORD:
                    choices = new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getPasswordRules());
                    break;

                default:
                    choices = new ArrayList<>();
            }

            Collections.<String>sort(choices);
            conf.setChoices(choices);

            conf.addRequiredLabel();
            add(conf);
        }
    }

    public class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        private final LoadableDetachableModel<Serializable> bean;

        public Configuration(final PolicyRuleDirectoryPanel.PolicyRuleWrapper rule) {
            bean = new LoadableDetachableModel<Serializable>() {

                private static final long serialVersionUID = 2092144708018739371L;

                @Override
                protected Serializable load() {
                    return rule.getConf();
                }
            };

            add(new BeanPanel<>("bean", bean).setRenderBodyOnly(true));
        }
    }
}
