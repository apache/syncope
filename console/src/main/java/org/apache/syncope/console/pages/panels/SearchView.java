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
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.console.commons.SearchCondWrapper;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class SearchView extends ListView<SearchCondWrapper> {

    private static final long serialVersionUID = -527351923968737757L;

    private final WebMarkupContainer searchFormContainer;

    private final boolean required;

    private final IModel<List<AttributeCond.Type>> attributeTypes;

    private final IModel<List<SearchCondWrapper.FilterType>> filterTypes;

    private final IModel<List<String>> anames;

    private final IModel<List<String>> dnames;

    private final IModel<List<String>> roleNames;

    private final IModel<List<String>> resourceNames;

    private final IModel<List<String>> entitlements;

    public SearchView(final String id, final List<? extends SearchCondWrapper> list,
            final WebMarkupContainer searchFormContainer,
            final boolean required,
            final IModel<List<AttributeCond.Type>> attributeTypes,
            final IModel<List<SearchCondWrapper.FilterType>> filterTypes,
            final IModel<List<String>> anames,
            final IModel<List<String>> dnames,
            final IModel<List<String>> roleNames,
            final IModel<List<String>> resourceNames,
            final IModel<List<String>> entitlements) {

        super(id, list);

        this.searchFormContainer = searchFormContainer;
        this.required = required;
        this.attributeTypes = attributeTypes;
        this.filterTypes = filterTypes;
        this.anames = anames;
        this.dnames = dnames;
        this.roleNames = roleNames;
        this.resourceNames = resourceNames;
        this.entitlements = entitlements;
    }

    @Override
    protected void populateItem(final ListItem<SearchCondWrapper> item) {
        final SearchCondWrapper searchCondition = item.getModelObject();

        if (item.getIndex() == 0) {
            item.add(new Label("operationType", ""));
        } else {
            item.add(new Label("operationType", searchCondition.getOperationType().toString()));
        }

        final CheckBox notOperator = new CheckBox("notOperator", new PropertyModel(searchCondition, "notOperator"));
        notOperator.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(notOperator);

        final DropDownChoice<AttributeCond.Type> type = new DropDownChoice<AttributeCond.Type>("type",
                new PropertyModel<AttributeCond.Type>(searchCondition, "type"), attributeTypes);
        type.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(type);

        final DropDownChoice<String> filterNameChooser = new DropDownChoice<String>("filterName",
                new PropertyModel<String>(searchCondition, "filterName"), (IModel) null);
        filterNameChooser.setOutputMarkupId(true);
        filterNameChooser.setRequired(required);
        filterNameChooser.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(filterNameChooser);

        final TextField<String> filterValue = new TextField<String>("filterValue", new PropertyModel<String>(
                searchCondition, "filterValue"));
        filterValue.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        item.add(filterValue);

        final DropDownChoice<SearchCondWrapper.FilterType> filterTypeChooser =
                new DropDownChoice<SearchCondWrapper.FilterType>("filterType",
                new PropertyModel<SearchCondWrapper.FilterType>(searchCondition, "filterType"), filterTypes);
        filterTypeChooser.setOutputMarkupId(true);
        filterTypeChooser.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(searchFormContainer);
            }
        });
        filterTypeChooser.setRequired(required);
        item.add(filterTypeChooser);

        AjaxButton addAndButton = new IndicatingAjaxButton("addAndButton", new ResourceModel("addAndButton")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                SearchCondWrapper conditionWrapper = new SearchCondWrapper();
                conditionWrapper.setOperationType(SearchCondWrapper.OperationType.AND);
                SearchView.this.getModelObject().add(conditionWrapper);
                target.add(searchFormContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchFormContainer);
            }
        };
        addAndButton.setDefaultFormProcessing(false);
        if (item.getIndex() != getModelObject().size() - 1) {
            addAndButton.setVisible(false);
        }
        item.add(addAndButton);

        AjaxButton addOrButton = new IndicatingAjaxButton("addOrButton", new ResourceModel("addOrButton")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                SearchCondWrapper conditionWrapper = new SearchCondWrapper();
                conditionWrapper.setOperationType(SearchCondWrapper.OperationType.OR);
                SearchView.this.getModelObject().add(conditionWrapper);
                target.add(searchFormContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchFormContainer);
            }
        };
        addOrButton.setDefaultFormProcessing(false);
        if (item.getIndex() != getModelObject().size() - 1) {
            addOrButton.setVisible(false);
        }
        item.add(addOrButton);

        AjaxButton dropButton = new IndicatingAjaxButton("dropButton", new ResourceModel("dropButton")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                getList().remove(Integer.valueOf(getParent().getId()).intValue());
                target.add(searchFormContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(searchFormContainer);
            }
        };
        dropButton.setDefaultFormProcessing(false);
        if (item.getIndex() == 0) {
            dropButton.setVisible(false);
        }
        item.add(dropButton);

        if (searchCondition == null || searchCondition.getFilterType() == null) {
            filterNameChooser.setChoices(Collections.<String>emptyList());
        } else {
            switch (searchCondition.getFilterType()) {
                case ATTRIBUTE:
                    final List<String> names = new ArrayList<String>(dnames.getObject());

                    if (anames.getObject() != null && !anames.getObject().isEmpty()) {
                        names.addAll(anames.getObject());
                    }
                    Collections.sort(names);

                    filterNameChooser.setChoices(names);
                    if (!type.isEnabled()) {
                        type.setEnabled(true);
                        type.setRequired(true);
                    }
                    if (!filterValue.isEnabled()) {
                        filterValue.setEnabled(true);
                    }

                    break;

                case MEMBERSHIP:
                    filterNameChooser.setChoices(roleNames);
                    filterNameChooser.setChoiceRenderer(new IChoiceRenderer<String>() {

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
                    type.setEnabled(false);
                    type.setRequired(false);
                    type.setModelObject(null);

                    filterValue.setEnabled(false);
                    filterValue.setModelObject("");

                    break;

                case RESOURCE:
                    filterNameChooser.setChoices(resourceNames);
                    type.setEnabled(false);
                    type.setRequired(false);
                    type.setModelObject(null);

                    filterValue.setEnabled(false);
                    filterValue.setModelObject("");

                    break;

                case ENTITLEMENT:
                    filterNameChooser.setChoices(entitlements);
                    type.setEnabled(false);
                    type.setRequired(false);
                    type.setModelObject(null);

                    filterValue.setEnabled(false);
                    filterValue.setModelObject("");

                    break;

                default:
                    filterNameChooser.setChoices(Collections.<String>emptyList());
            }
        }
    }
}
