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

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.policy.ComposablePolicy;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public class PolicyRuleWizardBuilder extends BaseAjaxWizardBuilder<PolicyRuleWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final String policy;

    protected final PolicyType type;

    protected final String implementationType;

    protected final PolicyRestClient policyRestClient;

    protected final ImplementationRestClient implementationRestClient;

    public PolicyRuleWizardBuilder(
            final String policy,
            final PolicyType type,
            final PolicyRuleWrapper policyWrapper,
            final PolicyRestClient policyRestClient,
            final ImplementationRestClient implementationRestClient,
            final PageReference pageRef) {

        super(policyWrapper, pageRef);

        this.policy = policy;
        this.type = type;
        this.implementationType = type == PolicyType.ACCOUNT
                ? IdRepoImplementationType.ACCOUNT_RULE
                : IdRepoImplementationType.PASSWORD_RULE;
        this.policyRestClient = policyRestClient;
        this.implementationRestClient = implementationRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final PolicyRuleWrapper modelObject) {
        PolicyTO policyTO = policyRestClient.read(type, policy);

        ComposablePolicy composable;
        if (policyTO instanceof final ComposablePolicy composablePolicy) {
            composable = composablePolicy;
        } else {
            throw new IllegalStateException("Non composable policy");
        }

        if (modelObject.getImplementationEngine() == ImplementationEngine.JAVA) {
            ImplementationTO rule = implementationRestClient.
                    read(implementationType, modelObject.getImplementationKey());
            try {
                rule.setBody(MAPPER.writeValueAsString(modelObject.getConf()));
                implementationRestClient.update(rule);
            } catch (Exception e) {
                throw new WicketRuntimeException(e);
            }
        }

        if (modelObject.isNew()) {
            composable.getRules().add(modelObject.getImplementationKey());
        }

        policyRestClient.update(type, policyTO);
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

        private final PolicyRuleWrapper rule;

        public Profile(final PolicyRuleWrapper rule) {
            this.rule = rule;

            AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>(
                    "rule", "rule", new PropertyModel<>(rule, "implementationKey"));

            List<String> choices;
            switch (type) {
                case ACCOUNT:
                    choices = implementationRestClient.list(IdRepoImplementationType.ACCOUNT_RULE).stream().
                            map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
                    break;

                case PASSWORD:
                    choices = implementationRestClient.list(IdRepoImplementationType.PASSWORD_RULE).stream().
                            map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
                    break;

                default:
                    choices = new ArrayList<>();
            }

            conf.setChoices(choices);
            conf.addRequiredLabel();
            conf.setNullValid(false);
            conf.setEnabled(rule.isNew());
            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    ImplementationTO impl = implementationRestClient.read(implementationType, conf.getModelObject());
                    rule.setImplementationEngine(impl.getEngine());
                    if (impl.getEngine() == ImplementationEngine.JAVA) {
                        try {
                            rule.setConf(MAPPER.readValue(impl.getBody(), RuleConf.class));
                        } catch (Exception e) {
                            LOG.error("During deserialization", e);
                        }
                    }
                }
            });
            add(conf);
        }

        @Override
        public void applyState() {
            if (rule.getImplementationEngine() == ImplementationEngine.GROOVY) {
                getWizardModel().finish();
            }
        }
    }

    public class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Configuration(final PolicyRuleWrapper rule) {
            LoadableDetachableModel<Serializable> bean = new LoadableDetachableModel<>() {

                private static final long serialVersionUID = 2092144708018739371L;

                @Override
                protected Serializable load() {
                    return rule.getConf();
                }
            };
            add(new BeanPanel<>("bean", bean, pageRef).setRenderBodyOnly(true));
        }
    }
}
