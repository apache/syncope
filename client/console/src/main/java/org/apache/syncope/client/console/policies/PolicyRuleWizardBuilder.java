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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.ComposablePolicy;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public class PolicyRuleWizardBuilder extends AjaxWizardBuilder<PolicyRuleWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ImplementationRestClient IMPLEMENTATION_CLIENT = new ImplementationRestClient();

    @SuppressWarnings("unchecked")
    public static List<PolicyRuleWrapper> getPolicyRuleWrappers(final AbstractPolicyTO policyTO) {
        Object rules = PropertyResolver.getValue("rules", policyTO);
        if (rules instanceof List) {
            return ((List<String>) rules).stream().map(rule -> {
                ImplementationTO implementation = IMPLEMENTATION_CLIENT.read(rule);

                PolicyRuleWrapper wrapper = null;
                if (implementation.getEngine() == ImplementationEngine.JAVA) {
                    try {
                        RuleConf ruleConf = MAPPER.readValue(implementation.getBody(), RuleConf.class);
                        wrapper = new PolicyRuleWrapper().
                                setImplementationKey(implementation.getKey()).
                                setName(ruleConf.getName());
                    } catch (Exception e) {
                        LOG.error("During deserialization", e);
                    }
                }

                return wrapper;
            }).filter(wrapper -> wrapper != null).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    private final PolicyRestClient restClient = new PolicyRestClient();

    private final String policy;

    private final PolicyType type;

    public PolicyRuleWizardBuilder(
            final String policy,
            final PolicyType type,
            final PolicyRuleWrapper reportlet,
            final PageReference pageRef) {
        super(reportlet, pageRef);
        this.policy = policy;
        this.type = type;
    }

    @Override
    protected Serializable onApplyInternal(final PolicyRuleWrapper modelObject) {
        AbstractPolicyTO policyTO = restClient.getPolicy(policy);

        ComposablePolicy composable;
        if (policyTO instanceof ComposablePolicy) {
            composable = (ComposablePolicy) policyTO;
        } else {
            throw new IllegalStateException("Non composable policy");
        }

        ImplementationTO rule = new ImplementationTO();
        rule.setKey(modelObject.getName());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(composable instanceof AccountPolicyTO
                ? ImplementationType.ACCOUNT_RULE : ImplementationType.PASSWORD_RULE);
        try {
            rule.setBody(MAPPER.writeValueAsString(modelObject.getConf()));

            rule = IMPLEMENTATION_CLIENT.create(rule);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create rule", e);
        }

        if (!modelObject.isNew()) {
            List<PolicyRuleWrapper> wrappers = getPolicyRuleWrappers(policyTO);
            wrappers.removeAll(wrappers.stream().
                    filter(wrapper -> wrapper.getName().equals(modelObject.getOldName())).collect(Collectors.toSet()));
            composable.getRules().clear();
            composable.getRules().addAll(wrappers.stream().
                    map(PolicyRuleWrapper::getImplementationKey).collect(Collectors.toSet()));
        }
        composable.getRules().add(rule.getKey());

        restClient.updatePolicy(policyTO);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final PolicyRuleWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Configuration(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        public Profile(final PolicyRuleWrapper rule) {
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

            Optional<JavaImplInfo> providers;
            List<String> choices;
            switch (type) {
                case ACCOUNT:
                    providers = SyncopeConsoleSession.get().getPlatformInfo().
                            getJavaImplInfo(ImplementationType.ACCOUNT_RULE);
                    choices = providers.isPresent()
                            ? new ArrayList<>(providers.get().getClasses())
                            : new ArrayList<>();
                    break;

                case PASSWORD:
                    providers = SyncopeConsoleSession.get().getPlatformInfo().
                            getJavaImplInfo(ImplementationType.PASSWORD_RULE);
                    choices = providers.isPresent()
                            ? new ArrayList<>(providers.get().getClasses())
                            : new ArrayList<>();
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

        public Configuration(final PolicyRuleWrapper rule) {
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
