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
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.search.SearchClause.Comparator;
import org.apache.syncope.client.console.panels.search.SearchClause.Operator;
import org.apache.syncope.client.console.panels.search.SearchClause.Type;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.RelationshipTypeRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxEventBehavior;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.springframework.util.CollectionUtils;

public class SearchClausePanel extends FieldPanel<SearchClause> {

    private static final long serialVersionUID = -527351923968737757L;

    protected static final AttributeModifier PREVENT_DEFAULT_RETURN = AttributeModifier.replace(
            "onkeydown",
            Model.of("if (event.keyCode == 13) { event.preventDefault(); }"));

    protected static final Consumer<AjaxRequestAttributes> AJAX_SUBMIT_ON_RETURN =
            attributes -> attributes.getAjaxCallListeners().add(new AjaxCallListener() {

                private static final long serialVersionUID = 7160235486520935153L;

                @Override
                public CharSequence getPrecondition(final Component component) {
                    return "return (Wicket.Event.keyCode(attrs.event) == 13);";
                }
            });

    public interface Customizer extends Serializable {

        default IChoiceRenderer<SearchClause.Type> typeRenderer() {
            return new ChoiceRenderer<>();
        }

        default List<Comparator> comparators() {
            return List.of();
        }

        default String comparatorDisplayValue(Comparator object) {
            return object.toString();
        }

        default Optional<SearchClause.Comparator> comparatorGetObject(String id) {
            return Optional.empty();
        }

        default List<String> properties() {
            return List.of();
        }

        default void adjust(
                Type type,
                AjaxTextFieldPanel property,
                FieldPanel<Comparator> comparator,
                FieldPanel<?> value,
                LoadableDetachableModel<List<Pair<String, String>>> properties) {

            value.setEnabled(true);
            value.setModelObject(null);
            property.setEnabled(true);

            // reload properties list
            properties.detach();
            property.setChoices(properties.getObject().stream().map(Pair::getKey).toList());
        }
    }

    protected class ComparatorRenderer implements IChoiceRenderer<SearchClause.Comparator> {

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

                case GROUP_MEMBERSHIP:
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

                case GROUP_MEMBER:
                    switch (object) {
                        case EQUALS:
                            display = "WITH";
                            break;

                        case NOT_EQUALS:
                            display = "WITHOUT";
                            break;

                        default:
                            display = StringUtils.EMPTY;
                    }
                    break;

                case AUX_CLASS:
                case ROLE_MEMBERSHIP:
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

                case RELATIONSHIP:
                    switch (object) {
                        case IS_NOT_NULL:
                            display = "EXIST";
                            break;

                        case IS_NULL:
                            display = "NOT EXIST";
                            break;

                        case EQUALS:
                            display = "WITH";
                            break;

                        case NOT_EQUALS:
                            display = "WITHOUT";
                            break;

                        default:
                            display = StringUtils.EMPTY;
                    }
                    break;

                case CUSTOM:
                    display = customizer.comparatorDisplayValue(object);
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

            if (id == null) {
                return SearchClause.Comparator.EQUALS;
            }

            final SearchClause.Comparator comparator;
            switch (id) {
                case "HAS":
                case "IN":
                case "WITH":
                    comparator = SearchClause.Comparator.EQUALS;
                    break;

                case "HAS NOT":
                case "NOT IN":
                case "WITHOUT":
                    comparator = SearchClause.Comparator.NOT_EQUALS;
                    break;

                case "NULL":
                case "NOT EXIST":
                    comparator = SearchClause.Comparator.IS_NULL;
                    break;

                case "NOT NULL":
                case "EXIST":
                    comparator = SearchClause.Comparator.IS_NOT_NULL;
                    break;

                case "==":
                    comparator = SearchClause.Comparator.EQUALS;
                    break;

                case "!=":
                    comparator = SearchClause.Comparator.NOT_EQUALS;
                    break;

                case "<":
                    comparator = SearchClause.Comparator.LESS_THAN;
                    break;

                case "<=":
                    comparator = SearchClause.Comparator.LESS_OR_EQUALS;
                    break;

                case ">":
                    comparator = SearchClause.Comparator.GREATER_THAN;
                    break;

                case ">=":
                    comparator = SearchClause.Comparator.GREATER_OR_EQUALS;
                    break;

                default:
                    // EQUALS to be used as default value
                    comparator = customizer.comparatorGetObject(id).orElse(SearchClause.Comparator.EQUALS);
                    break;
            }

            return comparator;
        }
    }

    @SpringBean
    protected RelationshipTypeRestClient relationshipTypeRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    protected final boolean required;

    protected final IModel<List<SearchClause.Type>> types;

    protected final Customizer customizer;

    protected final IModel<Map<String, PlainSchemaTO>> anames;

    protected final IModel<Map<String, PlainSchemaTO>> dnames;

    protected final Pair<IModel<List<String>>, IModel<Long>> groupInfo;

    protected final IModel<List<String>> roleNames;

    protected final IModel<List<String>> auxClassNames;

    protected final IModel<List<String>> resourceNames;

    protected IModel<SearchClause> clause;

    protected final LoadableDetachableModel<List<Comparator>> comparators;

    protected final LoadableDetachableModel<List<Pair<String, String>>> properties;

    protected final Fragment operatorFragment;

    protected final Fragment searchButtonFragment;

    protected final AjaxLink<Void> searchButton;

    protected IEventSink resultContainer;

    private FieldPanel<?> value;

    public SearchClausePanel(
            final String id,
            final String name,
            final Model<SearchClause> clause,
            final boolean required,
            final IModel<List<SearchClause.Type>> types,
            final Customizer customizer,
            final IModel<Map<String, PlainSchemaTO>> anames,
            final IModel<Map<String, PlainSchemaTO>> dnames,
            final Pair<IModel<List<String>>, IModel<Long>> groupInfo,
            final IModel<List<String>> roleNames,
            final IModel<List<String>> auxClassNames,
            final IModel<List<String>> resourceNames) {

        super(id, name, clause);

        this.clause = Optional.ofNullable(clause).orElseGet(() -> new Model<>(null));

        this.required = required;
        this.types = types;
        this.customizer = customizer;
        this.anames = anames;
        this.dnames = dnames;
        this.groupInfo = groupInfo;
        this.roleNames = roleNames;
        this.auxClassNames = auxClassNames;
        this.resourceNames = resourceNames;

        searchButton = new AjaxLink<>("search") {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                Optional.ofNullable(resultContainer).ifPresentOrElse(
                        container -> send(container, Broadcast.EXACT, new SearchEvent(target)),
                        () -> send(SearchClausePanel.this, Broadcast.BUBBLE, new SearchEvent(target)));
            }
        };

        searchButtonFragment = new Fragment("operator", "searchButtonFragment", this);
        searchButtonFragment.add(searchButton.setEnabled(false).setVisible(false));

        operatorFragment = new Fragment("operator", "operatorFragment", this);

        field = new FormComponent<>("container", this.clause) {

            private static final long serialVersionUID = -8204140666393922700L;

        };
        add(field);

        comparators = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<Comparator> load() {
                if (field.getModel().getObject() == null || field.getModel().getObject().getType() == null) {
                    return List.of();
                }

                switch (field.getModel().getObject().getType()) {
                    case ATTRIBUTE:
                        return List.of(SearchClause.Comparator.values());

                    case AUX_CLASS:
                    case ROLE_MEMBERSHIP:
                    case GROUP_MEMBERSHIP:
                    case GROUP_MEMBER:
                    case RESOURCE:
                        return List.of(
                                SearchClause.Comparator.EQUALS,
                                SearchClause.Comparator.NOT_EQUALS);

                    case RELATIONSHIP:
                        return List.of(
                                SearchClause.Comparator.IS_NOT_NULL,
                                SearchClause.Comparator.IS_NULL,
                                SearchClause.Comparator.EQUALS,
                                SearchClause.Comparator.NOT_EQUALS);

                    case CUSTOM:
                        return customizer.comparators();

                    default:
                        return List.of();
                }
            }
        };

        properties = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<Pair<String, String>> load() {
                if (field.getModel().getObject() == null || field.getModel().getObject().getType() == null) {
                    return List.of();
                }

                switch (field.getModel().getObject().getType()) {
                    case ATTRIBUTE -> {
                        Locale locale = SyncopeConsoleSession.get().getLocale();
                        List<Pair<String, String>> names = dnames.getObject().entrySet().stream().
                                map(item -> Pair.of(
                                item.getKey(),
                                Optional.ofNullable(item.getValue().getLabel(locale)).orElseGet(item::getKey))).
                                collect(Collectors.toList());
                        if (anames != null && !CollectionUtils.isEmpty(anames.getObject())) {
                            names.addAll(anames.getObject().entrySet().stream().
                                    map(item -> Pair.of(
                                    item.getKey(),
                                    Optional.ofNullable(item.getValue().getLabel(locale)).orElseGet(item::getKey))).
                                    toList());
                        }
                        return names.stream().
                                sorted(java.util.Comparator.comparing(name -> name.getValue().toLowerCase())).
                                collect(Collectors.toList());
                    }

                    case GROUP_MEMBERSHIP -> {
                        return groupInfo.getLeft().getObject().stream().map(item -> Pair.of(item, item)).toList();
                    }

                    case ROLE_MEMBERSHIP -> {
                        return Optional.ofNullable(roleNames).
                                map(r -> r.getObject().stream().sorted().map(item -> Pair.of(item, item)).
                                collect(Collectors.toList())).
                                orElseGet(List::of);
                    }

                    case AUX_CLASS -> {
                        return auxClassNames.getObject().stream().sorted().
                                map(item -> Pair.of(item, item)).toList();
                    }

                    case RESOURCE -> {
                        return resourceNames.getObject().stream().sorted().
                                map(item -> Pair.of(item, item)).toList();
                    }

                    case RELATIONSHIP -> {
                        return relationshipTypeRestClient.list().stream().
                                map(item -> Pair.of(item.getKey(), item.getKey())).
                                sorted().toList();
                    }

                    case CUSTOM -> {
                        return customizer.properties().stream().
                                map(item -> Pair.of(item, item)).
                                collect(Collectors.toList());
                    }

                    default -> {
                        return List.of();
                    }
                }
            }
        };
    }

    public void enableSearch(final IEventSink resultContainer) {
        this.resultContainer = resultContainer;
        this.searchButton.setEnabled(true);
        this.searchButton.setVisible(true);

        field.add(PREVENT_DEFAULT_RETURN);
        field.add(new AjaxEventBehavior(Constants.ON_KEYDOWN) {

            private static final long serialVersionUID = -7133385027739964990L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (resultContainer == null) {
                    send(SearchClausePanel.this, Broadcast.BUBBLE, new SearchEvent(target));
                } else {
                    send(resultContainer, Broadcast.EXACT, new SearchEvent(target));
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AJAX_SUBMIT_ON_RETURN.accept(attributes);
            }
        });
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
    public FieldPanel<SearchClause> settingsDependingComponents() {
        SearchClause searchClause = this.clause.getObject();

        WebMarkupContainer operatorContainer = new WebMarkupContainer("operatorContainer");
        operatorContainer.setOutputMarkupId(true);
        field.add(operatorContainer);

        BootstrapToggleConfig config = new BootstrapToggleConfig().
                withStyle("mt-1").
                withOnStyle(BootstrapToggleConfig.Style.info).
                withOffStyle(BootstrapToggleConfig.Style.warning).
                withSize(BootstrapToggleConfig.Size.small);

        operatorFragment.add(new BootstrapToggle("operator", new Model<>() {

            private static final long serialVersionUID = -7157802546272668001L;

            @Override
            public Boolean getObject() {
                return searchClause.getOperator() == Operator.AND;
            }

            @Override
            public void setObject(final Boolean object) {
                searchClause.setOperator(object ? Operator.AND : Operator.OR);
            }
        }, config) {

            private static final long serialVersionUID = 2969634208049189343L;

            @Override
            protected IModel<String> getOffLabel() {
                return Model.of("OR");
            }

            @Override
            protected IModel<String> getOnLabel() {
                return Model.of("AND");
            }

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = 18266219802290L;

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

        AjaxTextFieldPanel property = new AjaxTextFieldPanel(
                "property", "property", new PropertyModel<>(searchClause, "property")) {

            private static final long serialVersionUID = -7157802546272668001L;

            @Override
            protected Optional<IConverter<String>> getConverter() {
                return Optional.of(new IConverter<String>() {

                    private static final long serialVersionUID = -2754107934642211828L;

                    @Override
                    public String convertToObject(final String label, final Locale locale) throws ConversionException {
                        return properties.getObject().stream().
                                filter(property -> property.getValue().equalsIgnoreCase(label)).
                                findFirst().
                                map(Pair::getKey).
                                orElse(label);
                    }

                    @Override
                    public String convertToString(final String key, final Locale locale) {
                        return properties.getObject().stream().
                                filter(property -> property.getKey().equalsIgnoreCase(key)).
                                findFirst().
                                map(Pair::getValue).
                                orElse(key);
                    }
                });
            }
        };
        property.setChoices(properties.getObject().stream().map(Pair::getValue).toList());
        field.add(property.hideLabel().setOutputMarkupId(true).setEnabled(true));

        property.getField().add(PREVENT_DEFAULT_RETURN);
        property.getField().add(new IndicatorAjaxEventBehavior(Constants.ON_KEYUP) {

            private static final long serialVersionUID = -957948639666058749L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (field.getModel().getObject() != null
                        && field.getModel().getObject().getType() == Type.GROUP_MEMBERSHIP) {

                    String[] inputAsArray = property.getField().getInputAsArray();
                    if (ArrayUtils.isEmpty(inputAsArray)) {
                        property.setChoices(properties.getObject().stream().map(Pair::getKey).toList());
                    } else if (groupInfo.getRight().getObject() > Constants.MAX_GROUP_LIST_SIZE) {
                        String inputValue = inputAsArray.length > 1 && inputAsArray[1] != null
                                ? inputAsArray[1]
                                : property.getField().getInput();
                        if (!inputValue.startsWith("*")) {
                            inputValue = "*" + inputValue;
                        }
                        if (!inputValue.endsWith("*")) {
                            inputValue += "*";
                        }
                        property.setChoices(groupRestClient.search(
                                SyncopeConstants.ROOT_REALM,
                                SyncopeClient.getGroupSearchConditionBuilder().
                                        is(Constants.NAME_FIELD_NAME).equalToIgnoreCase(inputValue).
                                        query(),
                                1,
                                Constants.MAX_GROUP_LIST_SIZE,
                                new SortParam<>(Constants.NAME_FIELD_NAME, true),
                                null).stream().map(GroupTO::getName).toList());
                    }
                }
            }
        });
        property.getField().add(new IndicatorAjaxEventBehavior(Constants.ON_KEYDOWN) {

            private static final long serialVersionUID = -7133385027739964990L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.focusComponent(null);
                property.getField().inputChanged();
                property.getField().validate();
                if (property.getField().isValid()) {
                    property.getField().valid();
                    property.getField().updateModel();
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AJAX_SUBMIT_ON_RETURN.accept(attributes);
            }
        });

        AjaxDropDownChoicePanel<SearchClause.Comparator> comparator = new AjaxDropDownChoicePanel<>(
                "comparator", "comparator", new PropertyModel<>(searchClause, "comparator"));
        comparator.setChoices(comparators);
        comparator.setNullValid(false).hideLabel().setOutputMarkupId(true);
        comparator.setRequired(required);
        comparator.setChoiceRenderer(new ComparatorRenderer());
        field.add(comparator);

        value = buildValue(searchClause, property);
        field.addOrReplace(value);

        adjust(searchClause.getType(), property, comparator);

        property.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                value = buildValue(searchClause, property);
                field.addOrReplace(value);
                target.add(value);

                adjust(searchClause.getType(), property, comparator);
            }
        });

        AjaxDropDownChoicePanel<SearchClause.Type> type = new AjaxDropDownChoicePanel<>(
                "type", "type", new PropertyModel<>(searchClause, "type"));

        type.setChoices(types).setChoiceRenderer(customizer.typeRenderer()).
                hideLabel().setRequired(required).setOutputMarkupId(true);
        type.setNullValid(false);
        type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                final SearchClause searchClause = new SearchClause();
                if (StringUtils.isNotEmpty(type.getDefaultModelObjectAsString())) {
                    searchClause.setType(Type.valueOf(type.getDefaultModelObjectAsString()));
                }
                SearchClausePanel.this.clause.setObject(searchClause);

                adjust(searchClause.getType(), property, comparator);

                // reset property value in case and just in case of change of type
                property.setModelObject(StringUtils.EMPTY);
                target.add(property);

                target.add(comparator);
                target.add(value);
            }
        });
        field.add(type);

        comparator.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (type.getModelObject() == SearchClause.Type.ATTRIBUTE
                        || type.getModelObject() == SearchClause.Type.RELATIONSHIP) {

                    if (comparator.getModelObject() == SearchClause.Comparator.IS_NULL
                            || comparator.getModelObject() == SearchClause.Comparator.IS_NOT_NULL) {

                        value.setModelObject(null);
                        value.setEnabled(false);
                    } else {
                        value.setEnabled(true);
                    }
                    target.add(value);
                }

                if (type.getModelObject() == SearchClause.Type.RELATIONSHIP) {
                    property.setEnabled(true);

                    SearchClause searchClause = new SearchClause();
                    searchClause.setType(Type.valueOf(type.getDefaultModelObjectAsString()));
                    searchClause.setComparator(comparator.getModelObject());
                    SearchClausePanel.this.clause.setObject(searchClause);

                    target.add(property);
                }
            }
        });

        return this;
    }

    @SuppressWarnings("unchecked")
    protected void adjust(final Type type, final AjaxTextFieldPanel property, final FieldPanel<Comparator> comparator) {
        if (type == null) {
            return;
        }

        property.setEnabled(true);
        comparator.setEnabled(true);
        value.setEnabled(true);

        switch (type) {
            case ATTRIBUTE -> {
                if (!comparator.isEnabled()) {
                    comparator.setEnabled(true);
                    comparator.setRequired(true);
                }

                if (comparator.getModelObject() == SearchClause.Comparator.IS_NULL
                        || comparator.getModelObject() == SearchClause.Comparator.IS_NOT_NULL) {
                    value.setEnabled(false);
                    value.setModelObject(null);
                }

                // reload properties list
                properties.detach();
                property.setChoices(properties.getObject().stream().map(Pair::getValue).toList());
            }

            case ROLE_MEMBERSHIP -> {
                value.setEnabled(false);
                value.setModelObject(null);

                // reload properties list
                properties.detach();
                property.setChoices(properties.getObject().stream().map(Pair::getKey).toList());
            }

            case GROUP_MEMBERSHIP -> {
                value.setEnabled(false);
                value.setModelObject(null);

                // reload properties list
                properties.detach();
                property.setChoices(properties.getObject().stream().map(Pair::getKey).toList());
            }

            case GROUP_MEMBER -> {
                value.setEnabled(true);
                property.setEnabled(false);
                property.setModelObject(StringUtils.EMPTY);
            }

            case AUX_CLASS, RESOURCE -> {
                value.setEnabled(false);
                value.setModelObject(null);

                // reload properties list
                properties.detach();
                property.setChoices(properties.getObject().stream().map(Pair::getKey).toList());
            }

            case RELATIONSHIP -> {
                value.setEnabled(true);
                value.setModelObject(null);
                property.setEnabled(true);

                // reload properties list
                properties.detach();
                property.setChoices(properties.getObject().stream().map(Pair::getKey).toList());
            }

            case CUSTOM ->
                customizer.adjust(type, property, comparator, value, properties);

            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected FieldPanel<?> buildValue(final SearchClause searchClause, final AjaxTextFieldPanel property) {
        PlainSchemaTO plainSchema = Optional.ofNullable(anames.getObject().get(property.getModelObject())).
                orElseGet(() -> {
                    PlainSchemaTO defaultPlainTO = new PlainSchemaTO();
                    defaultPlainTO.setType(AttrSchemaType.String);
                    return Optional.ofNullable(property.getModelObject()).
                            map(k -> dnames.getObject().getOrDefault(k, defaultPlainTO)).
                            orElse(defaultPlainTO);
                });

        FieldPanel<?> result;
        switch (plainSchema.getType()) {
            case Boolean:
                result = new AjaxTextFieldPanel(
                        "value",
                        "value",
                        new PropertyModel<>(searchClause, "value"),
                        true);
                ((AjaxTextFieldPanel) result).setChoices(List.of("true", "false"));

                break;

            case Date:
                FastDateFormat formatter = StringUtils.isBlank(plainSchema.getConversionPattern())
                        ? DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT
                        : FastDateFormat.getInstance(plainSchema.getConversionPattern());

                PropertyModel<Date> dateModel = new PropertyModel<>(searchClause, "value") {

                    private static final long serialVersionUID = -3743432456095828573L;

                    @Override
                    public Date getObject() {
                        try {
                            return StringUtils.isBlank(searchClause.getValue())
                                    ? null
                                    : formatter.parse(searchClause.getValue());
                        } catch (ParseException e) {
                            LOG.error("Unparsable date: {}", searchClause.getValue(), e);
                            return null;
                        }
                    }

                    @Override
                    public void setObject(final Date object) {
                        Optional.ofNullable(object).ifPresent(date -> searchClause.setValue(formatter.format(date)));
                    }
                };

                if (plainSchema.getConversionPattern() == null
                        || StringUtils.containsIgnoreCase(plainSchema.getConversionPattern(), "H")) {

                    result = new AjaxDateTimeFieldPanel(
                            "value",
                            "value",
                            dateModel,
                            formatter);
                } else {
                    result = new AjaxDateFieldPanel(
                            "value",
                            "value",
                            dateModel,
                            formatter);
                }
                break;

            case Enum:
                result = new AjaxDropDownChoicePanel<>(
                        "value",
                        "value",
                        new PropertyModel<>(searchClause, "value"),
                        true);
                ((AjaxDropDownChoicePanel<String>) result).setChoices(
                        plainSchema.getEnumValues().keySet().stream().sorted().toList());

                if (!plainSchema.getEnumValues().isEmpty()) {
                    Map<String, String> valueMap = plainSchema.getEnumValues();
                    ((AjaxDropDownChoicePanel) result).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        @Override
                        public String getDisplayValue(final String value) {
                            return valueMap.get(value) == null ? value : valueMap.get(value);
                        }

                        @Override
                        public String getIdValue(final String value, final int i) {
                            return value;
                        }

                        @Override
                        public String getObject(
                                final String id, final IModel<? extends List<? extends String>> choices) {
                            return id;
                        }
                    });
                }
                break;

            case Long:
                result = new AjaxNumberFieldPanel.Builder<Long>().enableOnChange().build(
                        "value",
                        "Value",
                        Long.class,
                        new PropertyModel<>(searchClause, "value"));

                result.add(new AttributeModifier("class", "field value search-spinner"));
                break;

            case Double:
                result = new AjaxNumberFieldPanel.Builder<Double>().enableOnChange().step(0.1).build(
                        "value",
                        "value",
                        Double.class,
                        new PropertyModel<>(searchClause, "value"));
                result.add(new AttributeModifier("class", "field value search-spinner"));
                break;

            default:
                result = new AjaxTextFieldPanel(
                        "value", "value", new PropertyModel<>(searchClause, "value"), true);
                break;
        }

        result.hideLabel().setOutputMarkupId(true);
        result.getField().add(PREVENT_DEFAULT_RETURN);
        result.getField().add(new IndicatorAjaxEventBehavior(Constants.ON_KEYDOWN) {

            private static final long serialVersionUID = -7133385027739964990L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.focusComponent(null);
                result.getField().inputChanged();
                result.getField().validate();
                if (result.getField().isValid()) {
                    result.getField().valid();
                    result.getField().updateModel();
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AJAX_SUBMIT_ON_RETURN.accept(attributes);
            }
        });

        return result;
    }

    @Override
    public FieldPanel<SearchClause> clone() {
        SearchClausePanel panel = new SearchClausePanel(
                getId(), name, null, required, types, customizer, anames, dnames, groupInfo,
                roleNames, auxClassNames, resourceNames);
        panel.setReadOnly(this.isReadOnly());
        panel.setRequired(this.isRequired());
        if (searchButton.isEnabled()) {
            panel.enableSearch(resultContainer);
        }
        return panel;
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
