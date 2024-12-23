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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.policy.AbstractCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultInboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.InboundPolicyTO;
import org.apache.syncope.common.lib.policy.ProvisioningPolicyTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ProvisioningPolicyModalPanel extends AbstractModalPanel<ProvisioningPolicyTO> {

    private static final long serialVersionUID = 2988891313881271124L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected PolicyRestClient policyRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    protected final IModel<Map<String, ImplementationTO>> implementations;

    private final IModel<List<CorrelationRule>> model;

    @SuppressWarnings("unchecked")
    public ProvisioningPolicyModalPanel(
            final ProvisioningPolicyTO policyTO,
            final BaseModal<? extends ProvisioningPolicyTO> modal,
            final PageReference pageRef) {

        super((BaseModal<ProvisioningPolicyTO>) modal, pageRef);
        ((BaseModal<ProvisioningPolicyTO>) modal).setFormModel(policyTO);

        implementations = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, ImplementationTO> load() {
                return implementationRestClient.list(policyTO instanceof InboundPolicyTO
                        ? IdMImplementationType.INBOUND_CORRELATION_RULE
                        : IdMImplementationType.PUSH_CORRELATION_RULE).stream().
                        collect(Collectors.toMap(ImplementationTO::getKey, Function.identity()));
            }
        };

        model = new PropertyModel<>(policyTO, "correlationRules") {

            private static final long serialVersionUID = -8168676563540297301L;

            private final List<CorrelationRule> rules = policyTO.getCorrelationRules().keySet().stream().
                    map(anyType -> new CorrelationRule(
                    policyTO instanceof InboundPolicyTO
                            ? DefaultInboundCorrelationRuleConf.class
                            : DefaultPushCorrelationRuleConf.class,
                    anyType,
                    implementations.getObject().get(policyTO.getCorrelationRules().get(anyType)))).
                    collect(Collectors.toList());

            @Override
            public List<CorrelationRule> getObject() {
                return rules;
            }

            @Override
            public void setObject(final List<CorrelationRule> object) {
                policyTO.getCorrelationRules().clear();
                rules.forEach(rule -> policyTO.getCorrelationRules().put(rule.getAnyType(), rule.getImpl().getKey()));
            }
        };

        add(new MultiPanel<>("correlationRules", "correlationRules", model) {

            private static final long serialVersionUID = -2481579077338205547L;

            @Override
            protected CorrelationRule newModelObject() {
                return new CorrelationRule(policyTO instanceof InboundPolicyTO
                        ? DefaultInboundCorrelationRuleConf.class
                        : DefaultPushCorrelationRuleConf.class);
            }

            @Override
            protected CorrelationRulePanel getItemPanel(final ListItem<CorrelationRule> item) {
                return new CorrelationRulePanel("panel", Model.of(item.getModelObject()));
            }

            @Override
            protected void sendError(final String message) {
                SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
            }
        });
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            getItem().getCorrelationRules().clear();
            model.getObject().forEach(rule -> {
                getItem().getCorrelationRules().put(rule.getAnyType(), rule.getImplKey());

                if (rule.getImpl().getEngine() == ImplementationEngine.JAVA && rule.getDefaultRuleConf() != null) {
                    try {
                        implementationRestClient.update(rule.getImpl());
                    } catch (Exception e) {
                        throw new WicketRuntimeException(e);
                    }
                }
            });
            policyRestClient.update(getItem() instanceof InboundPolicyTO
                    ? PolicyType.INBOUND : PolicyType.PUSH, getItem());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating/updating policy", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    protected class CorrelationRulePanel extends Panel {

        private static final long serialVersionUID = -5380414818290018189L;

        CorrelationRulePanel(final String id, final IModel<CorrelationRule> correlationRule) {
            super(id);

            AjaxDropDownChoicePanel<String> anyType = new AjaxDropDownChoicePanel<>(
                    "anyType", "anyType", new PropertyModel<String>(correlationRule.getObject(), "anyType")).
                    setNullValid(true).
                    setChoices(anyTypeRestClient.list());
            anyType.setNullValid(false);
            anyType.setRequired(true);
            anyType.setOutputMarkupId(true);
            add(anyType);

            AjaxDropDownChoicePanel<String> rule = new AjaxDropDownChoicePanel<>(
                    "rule", "rule", new PropertyModel<String>(correlationRule.getObject(), "implKey")).
                    setChoices(implementations.getObject().keySet().stream().sorted().collect(Collectors.toList()));
            rule.setNullValid(false);
            rule.setRequired(true);
            rule.setOutputMarkupId(true);
            add(rule);

            PropertyModel<Boolean> orSchemasModel =
                    new PropertyModel<>(correlationRule.getObject().getDefaultRuleConf(), "orSchemas") {

                private static final long serialVersionUID = 807008909842554829L;

                @Override
                public Boolean getObject() {
                    AbstractCorrelationRuleConf conf = correlationRule.getObject().getDefaultRuleConf();
                    return conf instanceof DefaultInboundCorrelationRuleConf
                            ? DefaultInboundCorrelationRuleConf.class.cast(conf).isOrSchemas()
                            : conf instanceof DefaultPushCorrelationRuleConf
                                    ? DefaultPushCorrelationRuleConf.class.cast(conf).isOrSchemas()
                                    : false;
                }

                @Override
                public void setObject(final Boolean object) {
                    AbstractCorrelationRuleConf conf = correlationRule.getObject().getDefaultRuleConf();
                    if (conf instanceof DefaultInboundCorrelationRuleConf) {
                        DefaultInboundCorrelationRuleConf.class.cast(conf).setOrSchemas(object);
                    } else if (conf instanceof DefaultPushCorrelationRuleConf) {
                        DefaultPushCorrelationRuleConf.class.cast(conf).setOrSchemas(object);
                    }
                }
            };
            AjaxCheckBoxPanel orSchemas = new AjaxCheckBoxPanel("orSchemas", "orSchemas", orSchemasModel, false);
            orSchemas.setOutputMarkupPlaceholderTag(true);
            add(orSchemas.setVisible(correlationRule.getObject().getDefaultRuleConf() != null));

            PropertyModel<List<String>> defaultRuleConfModel =
                    new PropertyModel<>(correlationRule.getObject().getDefaultRuleConf(), "schemas") {

                private static final long serialVersionUID = 3799387950428254072L;

                private List<String> schemas() {
                    AbstractCorrelationRuleConf conf = correlationRule.getObject().getDefaultRuleConf();
                    return conf instanceof DefaultInboundCorrelationRuleConf
                            ? DefaultInboundCorrelationRuleConf.class.cast(conf).getSchemas()
                            : conf instanceof DefaultPushCorrelationRuleConf
                                    ? DefaultPushCorrelationRuleConf.class.cast(conf).getSchemas()
                                    : List.of();
                }

                @Override
                public List<String> getObject() {
                    List<String> schemas = new ArrayList<>();
                    if (correlationRule.getObject().getDefaultRuleConf() != null) {
                        schemas.addAll(schemas());
                    }
                    return schemas;
                }

                @Override
                public void setObject(final List<String> object) {
                    if (correlationRule.getObject().getDefaultRuleConf() != null) {
                        schemas().clear();
                        schemas().addAll(object);
                    }
                }
            };
            AjaxPalettePanel<String> defaultRuleConf = new AjaxPalettePanel.Builder<String>().
                    setName("defaultRuleConf").build("defaultRuleConf",
                    defaultRuleConfModel, new AjaxPalettePanel.Builder.Query<>() {

                private static final long serialVersionUID = -7223078772249308813L;

                @Override
                public List<String> execute(final String filter) {
                    return getSchemas(correlationRule.getObject());
                }
            });
            defaultRuleConf.hideLabel().setOutputMarkupPlaceholderTag(true);
            add(defaultRuleConf.setVisible(correlationRule.getObject().getDefaultRuleConf() != null));

            anyType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (orSchemas.isVisibleInHierarchy()) {
                        target.add(orSchemas);
                    }
                    if (defaultRuleConf.isVisibleInHierarchy()) {
                        correlationRule.getObject().setImpl(null);
                        defaultRuleConf.reload(target);
                        target.add(defaultRuleConf);
                    }
                }
            });

            rule.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (correlationRule.getObject().getDefaultRuleConf() == null) {
                        orSchemas.setVisible(false);
                        defaultRuleConf.setVisible(false);
                    } else {
                        orSchemas.setVisible(true);
                        defaultRuleConf.setVisible(true);
                    }
                    target.add(orSchemas);
                    target.add(defaultRuleConf);
                }
            });
        }

        private List<String> getSchemas(final CorrelationRule rule) {
            List<String> choices = StringUtils.isEmpty(rule.getAnyType())
                    ? new ArrayList<>()
                    : schemaRestClient.getSchemas(SchemaType.PLAIN,
                            rule.getAnyType().equals(AnyTypeKind.USER.name())
                            ? AnyTypeKind.USER
                            : rule.getAnyType().equals(AnyTypeKind.GROUP.name())
                            ? AnyTypeKind.GROUP
                            : AnyTypeKind.ANY_OBJECT).stream().map(SchemaTO::getKey).
                            collect(Collectors.toList());
            choices.add(Constants.KEY_FIELD_NAME);
            choices.add(rule.getAnyType().equals(AnyTypeKind.USER.name())
                    ? Constants.USERNAME_FIELD_NAME : Constants.NAME_FIELD_NAME);
            Collections.sort(choices);
            return choices;
        }
    }

    private class CorrelationRule implements Serializable {

        private static final long serialVersionUID = 4221521483948294336L;

        private final Class<? extends AbstractCorrelationRuleConf> ruleConfClass;

        private String anyType;

        private ImplementationTO impl;

        private AbstractCorrelationRuleConf defaultRuleConf;

        CorrelationRule(final Class<? extends AbstractCorrelationRuleConf> ruleConfClass) {
            this.ruleConfClass = ruleConfClass;
            this.anyType = AnyTypeKind.USER.name();
        }

        CorrelationRule(
                final Class<? extends AbstractCorrelationRuleConf> ruleConfClass,
                final String anyType,
                final ImplementationTO impl) {

            this.ruleConfClass = ruleConfClass;
            this.anyType = anyType;
            setImpl(impl);
        }

        public String getAnyType() {
            return anyType;
        }

        public void setAnyType(final String anyType) {
            this.anyType = anyType;
        }

        public String getImplKey() {
            return Optional.ofNullable(impl).map(ImplementationTO::getKey).orElse(null);
        }

        public void setImplKey(final String key) {
            setImpl(implementations.getObject().get(key));
        }

        public final void setImpl(final ImplementationTO impl) {
            this.impl = impl;
            if (impl != null) {
                this.defaultRuleConf = null;
                try {
                    this.defaultRuleConf = MAPPER.readValue(impl.getBody(), ruleConfClass);
                } catch (Exception e) {
                    LOG.debug("Could not deserialize {} as {}", impl.getBody(), ruleConfClass.getName());
                }
            }
        }

        public ImplementationTO getImpl() {
            if (defaultRuleConf != null) {
                try {
                    this.impl.setBody(MAPPER.writeValueAsString(defaultRuleConf));
                } catch (Exception e) {
                    LOG.error("Could not serialize {}", defaultRuleConf);
                }
            }
            return impl;
        }

        public void setDefaultRuleConf(final DefaultPushCorrelationRuleConf defaultRuleConf) {
            this.defaultRuleConf = defaultRuleConf;
        }

        public AbstractCorrelationRuleConf getDefaultRuleConf() {
            return defaultRuleConf;
        }
    }
}
