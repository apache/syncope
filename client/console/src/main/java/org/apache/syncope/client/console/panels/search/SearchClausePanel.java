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
package org.apache.syncope.client.console.panels.search;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.search.SearchClause.Comparator;
import org.apache.syncope.client.console.panels.search.SearchClause.Operator;
import org.apache.syncope.client.console.panels.search.SearchClause.Type;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class SearchClausePanel extends FieldPanel<SearchClause> {

    private static final long serialVersionUID = -527351923968737757L;

    private final boolean required;

    private final IModel<List<SearchClause.Type>> types;

    private final IModel<List<String>> anames;

    private final IModel<List<String>> dnames;

    private final IModel<List<Pair<Long, String>>> groupNames;

    private final IModel<List<String>> resourceNames;

    private IModel<SearchClause> clause;

    private final LoadableDetachableModel<List<Comparator>> comparators;

    private final LoadableDetachableModel<List<Pair<Long, String>>> properties;

    private final Fragment operatorFragment;

    private final Fragment searchButtonFragment;

    private final AjaxSubmitLink searchButton;

    public SearchClausePanel(
            final String id,
            final String name,
            final Model<SearchClause> clause,
            final boolean required,
            final IModel<List<SearchClause.Type>> types,
            final IModel<List<String>> anames,
            final IModel<List<String>> dnames,
            final IModel<List<Pair<Long, String>>> groupNames,
            final IModel<List<String>> resourceNames
    ) {

        super(id, name, clause);

        this.clause = clause == null ? new Model<SearchClause>(null) : clause;

        this.required = required;
        this.types = types;
        this.anames = anames;
        this.dnames = dnames;
        this.groupNames = groupNames;
        this.resourceNames = resourceNames;

        searchButton = new AjaxSubmitLink("search") {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                send(this, Broadcast.BUBBLE, new SearchEvent(target));
            }
        };

        searchButtonFragment = new Fragment("operator", "searchButtonFragment", this);
        searchButtonFragment.add(searchButton.setEnabled(false));

        operatorFragment = new Fragment("operator", "operatorFragment", this);

        field = new FormComponent<SearchClause>("container", this.clause) {

            private static final long serialVersionUID = 1L;

        };

        add(field);

        comparators = new LoadableDetachableModel<List<Comparator>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<Comparator> load() {
                if (field.getModel().getObject() == null || field.getModel().getObject().getType() == null) {
                    return Collections.<Comparator>emptyList();
                }

                switch (field.getModel().getObject().getType()) {
                    case ATTRIBUTE:
                        return Arrays.asList(SearchClause.Comparator.values());

                    case MEMBERSHIP:
                    case RESOURCE:
                        return Arrays.asList(SearchClause.Comparator.EQUALS, SearchClause.Comparator.NOT_EQUALS);
                    default:
                        return Collections.<Comparator>emptyList();
                }
            }
        };

        properties = new LoadableDetachableModel<List<Pair<Long, String>>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<Pair<Long, String>> load() {
                if (field.getModel().getObject() == null || field.getModel().getObject().getType() == null) {
                    return Collections.<Pair<Long, String>>emptyList();
                }

                switch (field.getModel().getObject().getType()) {
                    case ATTRIBUTE:
                        final List<String> names = new ArrayList<String>(dnames.getObject());
                        if (anames != null && anames.getObject() != null && !anames.getObject().isEmpty()) {
                            names.addAll(anames.getObject());
                        }
                        Collections.sort(names);
                        return CollectionUtils.collect(names, new Transformer<String, Pair<Long, String>>() {

                            @Override
                            public Pair<Long, String> transform(final String input) {
                                return Pair.of(-1L, input);
                            }
                        }, new ArrayList<Pair<Long, String>>());

                    case MEMBERSHIP:
                        return groupNames.getObject();

                    case RESOURCE:
                        return CollectionUtils.collect(resourceNames.getObject(),
                                new Transformer<String, Pair<Long, String>>() {

                            @Override
                            public Pair<Long, String> transform(final String input) {
                                return Pair.of(-1L, input);
                            }
                        }, new ArrayList<Pair<Long, String>>());
                    default:
                        return Collections.<Pair<Long, String>>emptyList();
                }
            }
        };
    }

    public void enableSearch() {
        this.searchButton.setEnabled(true);
    }

    @Override
    public SearchClause getModelObject() {
        return this.clause.getObject();
    }

    @Override
    public FieldPanel<SearchClause> setModelObject(final SearchClause object) {
        this.clause.setObject(object);
        return super.setModelObject(object);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public FieldPanel<SearchClause> setNewModel(final ListItem item) {
        clause.setObject(SearchClause.class.cast(item.getModelObject()));
        return this;
    }

    @Override
    public FieldPanel<SearchClause> setNewModel(final IModel<SearchClause> model) {
        clause = model;
        return super.setNewModel(model);
    }

    @Override
    public final MarkupContainer add(final Component... childs) {
        return super.add(childs);
    }

    @Override
    public FieldPanel<SearchClause> settingsDependingComponents() {
        final SearchClause searchClause = this.clause.getObject();

        final WebMarkupContainer operatorContainer = new WebMarkupContainer("operatorContainer");
        operatorContainer.setOutputMarkupId(true);
        field.add(operatorContainer);

        final BootstrapToggleConfig config = new BootstrapToggleConfig();
        config
                .withOnStyle(BootstrapToggleConfig.Style.info).withOffStyle(BootstrapToggleConfig.Style.warning)
                .withSize(BootstrapToggleConfig.Size.mini)
                .withOnLabel("AND")
                .withOffLabel("OR");

        operatorFragment.add(new BootstrapToggle("operator", new Model<Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return searchClause.getOperator() == Operator.AND;
            }

            @Override
            public void setObject(final Boolean object) {
                searchClause.setOperator(object ? Operator.AND : Operator.OR);
            }
        }, config) {

            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> getOffLabel() {
                return Model.of(getString("Off", null, "OR"));
            }

            @Override
            protected IModel<String> getOnLabel() {
                return Model.of(getString("On", null, "AND"));
            }

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }
                });
                return checkBox;
            }
        }.setOutputMarkupPlaceholderTag(true));

        if (getIndex() > 0) {
            operatorContainer.add(operatorFragment);
        } else {
            operatorContainer.add(searchButtonFragment);
        }

        final AjaxDropDownChoicePanel<Pair<Long, String>> property = new AjaxDropDownChoicePanel<Pair<Long, String>>(
                "property", "property", new PropertyModel<Pair<Long, String>>(searchClause, "property") {

            private static final long serialVersionUID = -8430020195995502040L;

            @Override
            public Pair<Long, String> getObject() {
                return Pair.of(
                        searchClause.getType() == Type.MEMBERSHIP && searchClause.getProperty() != null
                                ? Long.parseLong(searchClause.getProperty()) : -1L,
                        searchClause.getProperty());
            }

            @Override
            public void setObject(final Pair<Long, String> object) {
                if (object != null) {
                    searchClause.setProperty(
                            object.getLeft() >= 0 ? String.valueOf(object.getLeft()) : object.getRight());
                }
            }
        });
        property.hideLabel().setRequired(required).setOutputMarkupId(true);
        property.setChoices(properties);
        field.add(property);

        final AjaxDropDownChoicePanel<SearchClause.Comparator> comparator = new AjaxDropDownChoicePanel<>(
                "comparator", "comparator", new PropertyModel<SearchClause.Comparator>(searchClause, "comparator"));
        comparator.setChoices(comparators);
        comparator.setNullValid(false).hideLabel().setOutputMarkupId(true);
        comparator.setRequired(required);
        comparator.setChoiceRenderer(getComparatorRender(field.getModel()));
        field.add(comparator);

        final AjaxTextFieldPanel value = new AjaxTextFieldPanel(
                "value", "value", new PropertyModel<String>(searchClause, "value"));
        value.hideLabel().setOutputMarkupId(true);
        field.add(value);

        final AjaxDropDownChoicePanel<SearchClause.Type> type = new AjaxDropDownChoicePanel<>(
                "type", "type", new PropertyModel<SearchClause.Type>(searchClause, "type"));
        type.setChoices(types).hideLabel().setRequired(required).setOutputMarkupId(true);
        type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                setFieldAccess(searchClause.getType(), property, comparator, value);
                target.add(property);
                target.add(comparator);
                target.add(value);
            }
        });
        field.add(type);

        comparator.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (type.getModelObject() == SearchClause.Type.ATTRIBUTE) {
                    if (comparator.getModelObject() == SearchClause.Comparator.IS_NULL
                            || comparator.getModelObject() == SearchClause.Comparator.IS_NOT_NULL) {

                        value.setModelObject(null);
                        value.setEnabled(false);
                    } else {
                        value.setEnabled(true);
                    }
                    target.add(value);
                }
            }
        });

        setFieldAccess(searchClause.getType(), property, comparator, value);

        return this;
    }

    private void setFieldAccess(
            final Type type,
            final AjaxDropDownChoicePanel<Pair<Long, String>> property,
            final FieldPanel<Comparator> comparator,
            final FieldPanel<String> value) {

        if (type != null) {
            switch (type) {
                case ATTRIBUTE:
                    if (!comparator.isEnabled()) {
                        comparator.setEnabled(true);
                        comparator.setRequired(true);
                    }

                    value.setEnabled(comparator.getModelObject() != SearchClause.Comparator.IS_NULL
                            && comparator.getModelObject() != SearchClause.Comparator.IS_NOT_NULL);
                    property.setChoiceRenderer(new DefaultChoiceRender());
                    break;
                case MEMBERSHIP:
                    property.setChoiceRenderer(new GroupChoiceRender());
                    value.setEnabled(false);
                    value.setModelObject("");
                    break;
                case RESOURCE:
                    property.setChoiceRenderer(new DefaultChoiceRender());
                    value.setEnabled(false);
                    value.setModelObject("");
                    break;
                default:
            }
        }
    }

    private IChoiceRenderer<SearchClause.Comparator> getComparatorRender(final IModel<SearchClause> clause) {
        return new IChoiceRenderer<SearchClause.Comparator>() {

            private static final long serialVersionUID = -9086043750227867686L;

            @Override
            public Object getDisplayValue(final SearchClause.Comparator object) {

                if (clause == null || clause.getObject() == null || clause.getObject().getType() == null) {
                    return object.toString();
                }

                String display;

                switch (clause.getObject().getType()) {
                    case ATTRIBUTE:
                        switch (object) {
                            case IS_NULL:
                                display = "NULL";
                                break;

                            case IS_NOT_NULL:
                                display = "NOT NULL";
                                break;

                            case EQUALS:
                                display = "==";
                                break;

                            case NOT_EQUALS:
                                display = "!=";
                                break;

                            case LESS_THAN:
                                display = "<";
                                break;

                            case LESS_OR_EQUALS:
                                display = "<=";
                                break;

                            case GREATER_THAN:
                                display = ">";
                                break;

                            case GREATER_OR_EQUALS:
                                display = ">=";
                                break;

                            default:
                                display = StringUtils.EMPTY;
                        }
                        break;
                    case MEMBERSHIP:
                        switch (object) {
                            case EQUALS:
                                display = "IN";
                                break;

                            case NOT_EQUALS:
                                display = "NOT IN";
                                break;

                            default:
                                display = StringUtils.EMPTY;
                        }
                        break;
                    case RESOURCE:
                        switch (object) {
                            case EQUALS:
                                display = "HAS";
                                break;

                            case NOT_EQUALS:
                                display = "HAS NOT";
                                break;

                            default:
                                display = StringUtils.EMPTY;
                        }
                        break;
                    default:
                        display = object.toString();
                }
                return display;
            }

            @Override
            public String getIdValue(final SearchClause.Comparator object, final int index) {
                return getDisplayValue(object).toString();
            }

            @Override
            public SearchClause.Comparator getObject(
                    final String id, final IModel<? extends List<? extends SearchClause.Comparator>> choices) {

                final SearchClause.Comparator res;
                switch (id) {
                    case "HAS":
                    case "IN":
                        res = SearchClause.Comparator.EQUALS;
                        break;
                    case "HAS NOT":
                    case "NOT IN":
                        res = SearchClause.Comparator.NOT_EQUALS;
                        break;
                    case "NULL":
                        res = SearchClause.Comparator.IS_NULL;
                        break;
                    case "NOT NULL":
                        res = SearchClause.Comparator.IS_NOT_NULL;
                        break;
                    case "==":
                        res = SearchClause.Comparator.EQUALS;
                        break;
                    case "!=":
                        res = SearchClause.Comparator.NOT_EQUALS;
                        break;
                    case "<":
                        res = SearchClause.Comparator.LESS_THAN;
                        break;
                    case "<=":
                        res = SearchClause.Comparator.LESS_OR_EQUALS;
                        break;
                    case ">":
                        res = SearchClause.Comparator.GREATER_THAN;
                        break;
                    case ">=":
                        res = SearchClause.Comparator.GREATER_OR_EQUALS;
                        break;
                    default:
                        // EQUALS to be used as default value
                        res = SearchClause.Comparator.EQUALS;
                        break;
                }
                return res;
            }
        };
    }

    @Override
    public FieldPanel<SearchClause> clone() {
        final SearchClausePanel panel = new SearchClausePanel(
                getId(), name, null, required, types, anames, dnames, groupNames, resourceNames);
        panel.setReadOnly(this.isReadOnly());
        panel.setRequired(this.isRequired());
        if (searchButton.isEnabled()) {
            panel.enableSearch();
        }
        return panel;
    }

    private class DefaultChoiceRender implements IChoiceRenderer<Pair<Long, String>> {

        private static final long serialVersionUID = -8034248752951761058L;

        @Override
        public Object getDisplayValue(final Pair<Long, String> object) {
            return object.getRight();
        }

        @Override
        public String getIdValue(final Pair<Long, String> object, final int index) {
            return object.getRight();
        }

        @Override
        public Pair<Long, String> getObject(
                final String id, final IModel<? extends List<? extends Pair<Long, String>>> choices) {
            return IterableUtils.find(choices.getObject(), new Predicate<Pair<Long, String>>() {

                @Override
                public boolean evaluate(final Pair<Long, String> object) {
                    return id.equals(object.getRight());
                }
            });
        }
    }

    private class GroupChoiceRender extends DefaultChoiceRender {

        private static final long serialVersionUID = -8034248752951761058L;

        @Override
        public String getIdValue(final Pair<Long, String> object, final int index) {
            return String.valueOf(object.getLeft());
        }

        @Override
        public Pair<Long, String> getObject(
                final String id, final IModel<? extends List<? extends Pair<Long, String>>> choices) {
            return IterableUtils.find(choices.getObject(), new Predicate<Pair<Long, String>>() {

                @Override
                public boolean evaluate(final Pair<Long, String> object) {
                    return id.equals(String.valueOf(object.getLeft()));
                }
            });
        }
    }

    public static class SearchEvent implements Serializable {

        private static final long serialVersionUID = 2693338614198749301L;

        private final AjaxRequestTarget target;

        public SearchEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
