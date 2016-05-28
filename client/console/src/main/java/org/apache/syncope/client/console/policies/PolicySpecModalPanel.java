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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiPanel;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public class PolicySpecModalPanel extends AbstractModalPanel<PullPolicyTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IModel<List<CorrelationRule>> model;

    private final PolicyRestClient restClient = new PolicyRestClient();

    public PolicySpecModalPanel(
            final PullPolicyTO policyTO, final BaseModal<PullPolicyTO> modal, final PageReference pageRef) {
        super(modal, pageRef);
        modal.setFormModel(policyTO);

        add(new AjaxDropDownChoicePanel<ConflictResolutionAction>(
                "conflictResolutionAction",
                "conflictResolutionAction",
                new PropertyModel<ConflictResolutionAction>(policyTO.getSpecification(), "conflictResolutionAction")).
                setChoices(Arrays.asList(ConflictResolutionAction.values())));

        model = new PropertyModel<List<CorrelationRule>>(policyTO.getSpecification(), "correlationRules") {

            private static final long serialVersionUID = -8168676563540297301L;

            private List<CorrelationRule> rules = CollectionUtils.collect(
                    policyTO.getSpecification().getCorrelationRules() == null
                            ? Collections.<String>emptySet()
                            : policyTO.getSpecification().getCorrelationRules().keySet(),
                    new Transformer<String, CorrelationRule>() {

                @Override
                public CorrelationRule transform(final String input) {
                    return new CorrelationRule(input, policyTO.getSpecification().getCorrelationRules().get(
                            input));
                }
            }, new ArrayList<CorrelationRule>());

            @Override
            public List<CorrelationRule> getObject() {
                return rules;
            }

            @Override
            public void setObject(final List<CorrelationRule> object) {
                policyTO.getSpecification().getCorrelationRules().clear();
                for (CorrelationRule rule : rules) {
                    policyTO.getSpecification().getCorrelationRules().put(rule.getAny(), rule.getRule());
                }
            }

        };

        add(new MultiPanel<CorrelationRule>("correlationRules", "correlationRules", model, false) {

            private static final long serialVersionUID = -2481579077338205547L;

            @Override
            protected CorrelationRule newModelObject() {
                return new CorrelationRule();
            }

            @Override
            protected CorrelationRulePanel getItemPanel(final ListItem<CorrelationRule> item) {
                return new CorrelationRulePanel("panel", item.getModelObject());
            }
        });
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            getItem().getSpecification().getCorrelationRules().clear();
            for (CorrelationRule rule : model.getObject()) {
                getItem().getSpecification().getCorrelationRules().put(rule.getAny(), rule.getRule());
            }
            restClient.updatePolicy(getItem());
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating/updating policy", e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                    getMessage());
        }
        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
    }

    public static class CorrelationRulePanel extends Panel {

        private static final long serialVersionUID = -4708008994320210839L;

        public CorrelationRulePanel(final String id, final CorrelationRule rule) {
            super(id);

            final AjaxDropDownChoicePanel<String> anyType = new AjaxDropDownChoicePanel<>(
                    "anyType", "any.type", new PropertyModel<String>(rule, "any")).
                    setNullValid(true).
                    setChoices(CollectionUtils.collect(
                            new AnyTypeRestClient().list(),
                            new Transformer<AnyTypeTO, String>() {

                        @Override
                        public String transform(final AnyTypeTO input) {
                            return input.getKey();
                        }
                    }, new ArrayList<String>()));
            add(anyType);

            final AjaxDropDownChoicePanel<String> ruleType = new AjaxDropDownChoicePanel<>(
                    "ruleType", "rule.type", new PropertyModel<String>(rule, "type"), false).
                    setNullValid(true).
                    setChoices(Arrays.asList("PLAIN", "JAVA"));
            add(ruleType);

            // ---------------------------------------------------------------
            // Java rule palette
            // ---------------------------------------------------------------
            final AjaxDropDownChoicePanel<String> javaRule = new AjaxDropDownChoicePanel<>(
                    "javaRule", "rule.java", new PropertyModel<String>(rule, "rule")).setChoices(
                    new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getPullCorrelationRules()));
            javaRule.setOutputMarkupPlaceholderTag(true);
            add(javaRule.setVisible("JAVA".equals(rule.getType())));
            // ---------------------------------------------------------------

            // ---------------------------------------------------------------
            // Json rule palette
            // ---------------------------------------------------------------
            final LoadableDetachableModel<List<String>> plainSchemas = new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID = 5275935387613157437L;

                @Override
                protected List<String> load() {
                    return getPlainSchemas(rule);
                }
            };

            final AjaxPalettePanel.Builder<String> jsonRuleBuilder
                    = new AjaxPalettePanel.Builder<String>().setName("rule.json");

            final PropertyModel<List<String>> jsonRuleModel = new PropertyModel<List<String>>(rule, "rule") {

                private static final long serialVersionUID = 3799387950428254072L;

                @Override
                public List<String> getObject() {
                    final List<String> res = new ArrayList<>();
                    try {
                        JsonNode obj = OBJECT_MAPPER.readTree(rule.getRule());
                        if (obj.isArray()) {
                            for (final JsonNode objNode : obj) {
                                res.add(objNode.asText());
                            }
                        }
                    } catch (IOException e) {
                        LOG.warn("Error deserializing json tree", e);
                    }
                    return res;
                }

                @Override
                public void setObject(final List<String> object) {
                    final StringBuilder bld = new StringBuilder();
                    bld.append("[");

                    boolean comma = false;
                    for (String obj : object) {
                        if (comma) {
                            bld.append(",");
                        } else {
                            comma = true;
                        }
                        bld.append("\"").append(obj).append("\"");
                    }
                    bld.append("]");
                    rule.setRule(bld.toString());
                }
            };

            final AjaxPalettePanel<String> jsonRule = jsonRuleBuilder.build("jsonRule", jsonRuleModel, plainSchemas);

            jsonRule.setOutputMarkupPlaceholderTag(true);

            anyType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (jsonRule.isVisibleInHierarchy()) {
                        jsonRule.getChoicesModel().setObject(getPlainSchemas(rule));
                        jsonRule.reload(target);
                    }
                }
            });

            add(jsonRule.setVisible("PLAIN".equals(rule.getType())));
            // ---------------------------------------------------------------

            ruleType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    switch (ruleType.getModelObject() == null ? StringUtils.EMPTY : ruleType.getModelObject()) {
                        case "PLAIN":
                            jsonRule.setVisible(true);
                            jsonRule.getChoicesModel().setObject(getPlainSchemas(rule));
                            jsonRule.reload(target);

                            javaRule.setVisible(false);

                            break;
                        case "JAVA":
                            jsonRule.setVisible(false);
                            javaRule.setVisible(true);
                            break;
                        default:
                            javaRule.setVisible(false);
                            jsonRule.setVisible(false);

                    }
                    target.add(jsonRule);
                    target.add(javaRule);
                }
            });
        }

        private static List<String> getPlainSchemas(final CorrelationRule rule) {
            final List<String> choices = StringUtils.isEmpty(rule.getAny())
                    ? new ArrayList<String>()
                    : CollectionUtils.collect(
                            new SchemaRestClient().getSchemas(SchemaType.PLAIN,
                                    rule.getAny().equals(AnyTypeKind.USER.name())
                                    ? AnyTypeKind.USER : rule.getAny().equals(AnyTypeKind.GROUP.name())
                                    ? AnyTypeKind.GROUP
                                    : AnyTypeKind.ANY_OBJECT), new Transformer<AbstractSchemaTO, String>() {

                        @Override
                        public String transform(final AbstractSchemaTO input) {
                            return input.getKey();
                        }
                    }, new ArrayList<String>());

            Collections.sort(choices);
            return choices;
        }
    }

    protected static class CorrelationRule implements Serializable {

        private static final long serialVersionUID = 5250228867297353011L;

        private String any;

        private String type;

        private String rule;

        public CorrelationRule() {
            this.any = AnyTypeKind.USER.name();
            this.type = "PLAIN";
            this.rule = "[]";
        }

        public CorrelationRule(final String any, final String rule) {
            this.any = any;
            this.type = StringUtils.isEmpty(rule) || rule.trim().startsWith("[") ? "PLAIN" : "JAVA";
            this.rule = rule;
        }

        public String getAny() {
            return any;
        }

        public String getType() {
            return type;
        }

        public String getRule() {
            return rule;
        }

        public void setAny(final String any) {
            this.any = any;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public void setRule(final String rule) {
            this.rule = rule;
        }
    }
}
