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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.console.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public class SearchView extends ListView<SearchClause> {

    private static final long serialVersionUID = -527351923968737757L;

    private final WebMarkupContainer searchFormContainer;

    private final boolean required;

    private final IModel<List<SearchClause.Type>> types;

    private final IModel<List<String>> anames;

    private final IModel<List<String>> dnames;

    private final IModel<List<String>> roleNames;

    private final IModel<List<String>> resourceNames;

    private final IModel<List<String>> entitlements;

    public SearchView(final String id, final List<? extends SearchClause> list,
            final WebMarkupContainer searchFormContainer,
            final boolean required,
            final IModel<List<SearchClause.Type>> types,
            final IModel<List<String>> anames,
            final IModel<List<String>> dnames,
            final IModel<List<String>> roleNames,
            final IModel<List<String>> resourceNames,
            final IModel<List<String>> entitlements) {

        super(id, list);

        this.searchFormContainer = searchFormContainer;
        this.required = required;
        this.types = types;
        this.anames = anames;
        this.dnames = dnames;
        this.roleNames = roleNames;
        this.resourceNames = resourceNames;
        this.entitlements = entitlements;
    }

    @Override
    protected void populateItem(final ListItem<SearchClause> item) {
        final SearchClause searchClause = item.getModelObject();

        final DropDownChoice<SearchClause.Operator> operator = new DropDownChoice<SearchClause.Operator>("operator",
                new PropertyModel<SearchClause.Operator>(searchClause, "operator"),
                Arrays.asList(SearchClause.Operator.values()));
        operator.setOutputMarkupPlaceholderTag(true);
        operator.setNullValid(false);
        operator.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(operator);
        if (item.getIndex() == 0) {
            operator.setVisible(false);
        }

        final DropDownChoice<SearchClause.Type> type = new DropDownChoice<SearchClause.Type>("type",
                new PropertyModel<SearchClause.Type>(searchClause, "type"), types);
        type.setOutputMarkupId(true);
        type.setRequired(required);
        type.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(searchFormContainer);
            }
        });
        item.add(type);

        @SuppressWarnings("unchecked")
        final DropDownChoice<String> property = new DropDownChoice<String>("property",
                new PropertyModel<String>(searchClause, "property"), (IModel) null);
        property.setOutputMarkupId(true);
        property.setRequired(required);
        property.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(property);

        final TextField<String> value = new TextField<String>("value",
                new PropertyModel<String>(searchClause, "value"));
        value.setOutputMarkupId(true);
        value.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(value);

        final DropDownChoice<SearchClause.Comparator> comparator =
                new DropDownChoice<SearchClause.Comparator>("comparator",
                        new PropertyModel<SearchClause.Comparator>(searchClause, "comparator"),
                        Collections.<SearchClause.Comparator>emptyList());
        comparator.setOutputMarkupId(true);
        comparator.setNullValid(false);
        comparator.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (type.getModelObject() == SearchClause.Type.ATTRIBUTE) {
                    if (comparator.getModelObject() == SearchClause.Comparator.IS_NULL
                            || comparator.getModelObject() == SearchClause.Comparator.IS_NOT_NULL) {

                        value.setEnabled(false);
                    } else {
                        value.setEnabled(true);
                    }
                    target.add(value);
                }
            }
        });
        comparator.setRequired(required);
        item.add(comparator);

        AjaxLink<Void> drop = new IndicatingAjaxLink<Void>("drop") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                SearchView.this.getModel().getObject().remove(item.getModelObject());
                target.add(searchFormContainer);
            }
        };
        item.add(drop);
        if (item.getIndex() == 0) {
            drop.setVisible(false);
            drop.setEnabled(false);
        } else {
            drop.setVisible(true);
            drop.setEnabled(true);
        }

        final AjaxLink<Void> add = new IndicatingAjaxLink<Void>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                SearchClause clause = new SearchClause();
                SearchView.this.getModel().getObject().add(clause);
                target.add(searchFormContainer);
            }
        };
        item.add(add);

        if (searchClause == null || searchClause.getType() == null) {
            property.setChoices(Collections.<String>emptyList());
        } else {
            switch (searchClause.getType()) {
                case ATTRIBUTE:
                    final List<String> names = new ArrayList<String>(dnames.getObject());
                    if (anames.getObject() != null && !anames.getObject().isEmpty()) {
                        names.addAll(anames.getObject());
                    }
                    Collections.sort(names);
                    property.setChoices(names);

                    comparator.setChoices(new LoadableDetachableModel<List<SearchClause.Comparator>>() {

                        private static final long serialVersionUID = 5275935387613157437L;

                        @Override
                        protected List<SearchClause.Comparator> load() {
                            return Arrays.asList(SearchClause.Comparator.values());
                        }
                    });
                    comparator.setChoiceRenderer(new IChoiceRenderer<SearchClause.Comparator>() {

                        private static final long serialVersionUID = -9086043750227867686L;

                        @Override
                        public Object getDisplayValue(final SearchClause.Comparator object) {
                            String display;

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

                            return display;
                        }

                        @Override
                        public String getIdValue(final SearchClause.Comparator object, int index) {
                            return getDisplayValue(object).toString();
                        }
                    });
                    if (!comparator.isEnabled()) {
                        comparator.setEnabled(true);
                        comparator.setRequired(true);
                    }

                    if (!value.isEnabled()) {
                        value.setEnabled(true);
                    }
                    break;

                case MEMBERSHIP:
                    property.setChoices(roleNames);
                    property.setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -4288397951948436434L;

                        @Override
                        public Object getDisplayValue(final String object) {
                            return object;
                        }

                        @Override
                        public String getIdValue(final String object, final int index) {
                            return object;
                        }
                    });

                    comparator.setChoices(new LoadableDetachableModel<List<SearchClause.Comparator>>() {

                        private static final long serialVersionUID = 5275935387613157437L;

                        @Override
                        protected List<SearchClause.Comparator> load() {
                            List<SearchClause.Comparator> comparators = new ArrayList<SearchClause.Comparator>();
                            comparators.add(SearchClause.Comparator.EQUALS);
                            comparators.add(SearchClause.Comparator.NOT_EQUALS);
                            return comparators;
                        }
                    });
                    comparator.setChoiceRenderer(new IChoiceRenderer<SearchClause.Comparator>() {

                        private static final long serialVersionUID = -9086043750227867686L;

                        @Override
                        public Object getDisplayValue(final SearchClause.Comparator object) {
                            String display;

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

                            return display;
                        }

                        @Override
                        public String getIdValue(final SearchClause.Comparator object, final int index) {
                            return getDisplayValue(object).toString();
                        }
                    });

                    value.setEnabled(false);
                    value.setModelObject("");

                    break;

                case RESOURCE:
                    property.setChoices(resourceNames);

                    comparator.setChoices(new LoadableDetachableModel<List<SearchClause.Comparator>>() {

                        private static final long serialVersionUID = 5275935387613157437L;

                        @Override
                        protected List<SearchClause.Comparator> load() {
                            List<SearchClause.Comparator> comparators = new ArrayList<SearchClause.Comparator>();
                            comparators.add(SearchClause.Comparator.EQUALS);
                            comparators.add(SearchClause.Comparator.NOT_EQUALS);
                            return comparators;
                        }
                    });
                    comparator.setChoiceRenderer(new IChoiceRenderer<SearchClause.Comparator>() {

                        private static final long serialVersionUID = -9086043750227867686L;

                        @Override
                        public Object getDisplayValue(final SearchClause.Comparator object) {
                            String display;

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

                            return display;
                        }

                        @Override
                        public String getIdValue(final SearchClause.Comparator object, final int index) {
                            return getDisplayValue(object).toString();
                        }
                    });

                    value.setEnabled(false);
                    value.setModelObject("");

                    break;

                case ENTITLEMENT:
                    property.setChoices(entitlements);

                    comparator.setChoices(new LoadableDetachableModel<List<SearchClause.Comparator>>() {

                        private static final long serialVersionUID = 5275935387613157437L;

                        @Override
                        protected List<SearchClause.Comparator> load() {
                            List<SearchClause.Comparator> comparators = new ArrayList<SearchClause.Comparator>();
                            comparators.add(SearchClause.Comparator.EQUALS);
                            comparators.add(SearchClause.Comparator.NOT_EQUALS);
                            return comparators;
                        }
                    });
                    comparator.setChoiceRenderer(new IChoiceRenderer<SearchClause.Comparator>() {

                        private static final long serialVersionUID = -9086043750227867686L;

                        @Override
                        public Object getDisplayValue(final SearchClause.Comparator object) {
                            String display;

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

                            return display;
                        }

                        @Override
                        public String getIdValue(final SearchClause.Comparator object, final int index) {
                            return getDisplayValue(object).toString();
                        }
                    });

                    value.setEnabled(false);
                    value.setModelObject("");

                    break;

                default:
                    property.setChoices(Collections.<String>emptyList());
            }
        }
    }
}
