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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.report.Schema;
import org.apache.syncope.common.lib.report.SearchCondition;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ClassUtils;

public class BeanPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 3905038169553185171L;

    protected static final Logger LOG = LoggerFactory.getLogger(BeanPanel.class);

    private final List<String> excluded;

    private final Map<String, Pair<AbstractFiqlSearchConditionBuilder, List<SearchClause>>> sCondWrapper;

    public BeanPanel(final String id, final IModel<T> bean, final String... excluded) {
        this(id, bean, null, excluded);
    }

    public BeanPanel(
            final String id,
            final IModel<T> bean,
            final Map<String, Pair<AbstractFiqlSearchConditionBuilder, List<SearchClause>>> sCondWrapper,
            final String... excluded) {
        super(id, bean);
        setOutputMarkupId(true);

        this.sCondWrapper = sCondWrapper;

        this.excluded = new ArrayList<>(Arrays.asList(excluded));
        this.excluded.add("serialVersionUID");
        this.excluded.add("class");

        final LoadableDetachableModel<List<String>> model = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<String> result = new ArrayList<>();

                if (BeanPanel.this.getDefaultModelObject() != null) {
                    for (Field field : BeanPanel.this.getDefaultModelObject().getClass().getDeclaredFields()) {
                        if (!BeanPanel.this.excluded.contains(field.getName())) {
                            result.add(field.getName());
                        }
                    }
                }

                return result;
            }
        };

        add(new ListView<String>("propView", model) {

            private static final long serialVersionUID = 9101744072914090143L;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected void populateItem(final ListItem<String> item) {
                final String fieldName = item.getModelObject();

                item.add(new Label("fieldName", new ResourceModel(fieldName, fieldName)));

                Field field = null;
                try {
                    field = bean.getObject().getClass().getDeclaredField(fieldName);
                } catch (NoSuchFieldException | SecurityException e) {
                    LOG.error("Could not find field {} in class {}", fieldName, bean.getObject().getClass(), e);
                }

                if (field == null) {
                    return;
                }

                final SearchCondition scondAnnot = field.getAnnotation(SearchCondition.class);
                final Schema schemaAnnot = field.getAnnotation(Schema.class);

                BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean.getObject());

                Panel panel;

                if (scondAnnot != null) {
                    final String fiql = (String) wrapper.getPropertyValue(fieldName);

                    final List<SearchClause> clauses;
                    if (StringUtils.isEmpty(fiql)) {
                        clauses = new ArrayList<>();
                    } else {
                        clauses = SearchUtils.getSearchClauses(fiql);
                    }

                    final AbstractFiqlSearchConditionBuilder builder;

                    switch (scondAnnot.type()) {
                        case "USER":
                            panel = new UserSearchPanel.Builder(
                                    new ListModel<>(clauses)).required(false).build("value");
                            builder = SyncopeClient.getUserSearchConditionBuilder();
                            break;
                        case "GROUP":
                            panel = new GroupSearchPanel.Builder(
                                    new ListModel<>(clauses)).required(false).build("value");
                            builder = SyncopeClient.getGroupSearchConditionBuilder();
                            break;
                        default:
                            panel = new AnyObjectSearchPanel.Builder(
                                    scondAnnot.type(),
                                    new ListModel<>(clauses)).required(false).build("value");
                            builder = SyncopeClient.getAnyObjectSearchConditionBuilder(null);
                    }

                    if (BeanPanel.this.sCondWrapper != null) {
                        BeanPanel.this.sCondWrapper.put(fieldName, Pair.of(builder, clauses));
                    }
                } else if (List.class.equals(field.getType())) {
                    Class<?> listItemType = String.class;
                    if (field.getGenericType() instanceof ParameterizedType) {
                        listItemType = (Class<?>) ((ParameterizedType) field.getGenericType()).
                                getActualTypeArguments()[0];
                    }

                    if (listItemType.equals(String.class) && schemaAnnot != null) {
                        SchemaRestClient schemaRestClient = new SchemaRestClient();

                        final List<AbstractSchemaTO> choices = new ArrayList<>();

                        for (SchemaType type : schemaAnnot.type()) {
                            switch (type) {
                                case PLAIN:
                                    choices.addAll(
                                            schemaRestClient.getSchemas(SchemaType.PLAIN, schemaAnnot.anyTypeKind()));
                                    break;

                                case DERIVED:
                                    choices.addAll(
                                            schemaRestClient.getSchemas(SchemaType.DERIVED, schemaAnnot.anyTypeKind()));
                                    break;

                                case VIRTUAL:
                                    choices.addAll(
                                            schemaRestClient.getSchemas(SchemaType.VIRTUAL, schemaAnnot.anyTypeKind()));
                                    break;

                                default:
                            }
                        }

                        panel = new AjaxPalettePanel.Builder<>().setName(fieldName).build(
                                "value",
                                new PropertyModel<>(bean.getObject(), fieldName),
                                new ListModel<>(choices.stream().map(EntityTO::getKey).collect(Collectors.toList()))).
                                hideLabel();
                    } else if (listItemType.isEnum()) {
                        panel = new AjaxPalettePanel.Builder<>().setName(fieldName).build(
                                "value",
                                new PropertyModel<>(bean.getObject(), fieldName),
                                new ListModel(Arrays.asList(listItemType.getEnumConstants()))).hideLabel();
                    } else {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(bean.getObject(), fieldName)).build(
                                "value",
                                fieldName,
                                buildSinglePanel(bean.getObject(), field.getType(), fieldName, "panel")).hideLabel();
                    }
                } else {
                    panel = buildSinglePanel(bean.getObject(), field.getType(), fieldName, "value").hideLabel();
                }

                item.add(panel.setRenderBodyOnly(true));
            }

        }.setReuseItems(true).setOutputMarkupId(true));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private FieldPanel buildSinglePanel(
            final Serializable bean, final Class<?> type, final String fieldName, final String id) {
        FieldPanel result = null;
        PropertyModel model = new PropertyModel(bean, fieldName);
        if (ClassUtils.isAssignable(Boolean.class, type)) {
            result = new AjaxCheckBoxPanel(id, fieldName, model);
        } else if (ClassUtils.isAssignable(Number.class, type)) {
            result = new AjaxSpinnerFieldPanel.Builder<>().build(
                    id, fieldName, (Class<Number>) ClassUtils.resolvePrimitiveIfNecessary(type), model);
        } else if (Date.class.equals(type)) {
            result = new AjaxDateTimeFieldPanel(id, fieldName, model, SyncopeConstants.DEFAULT_DATE_PATTERN);
        } else if (type.isEnum()) {
            result = new AjaxDropDownChoicePanel(id, fieldName, model).setChoices(
                    Arrays.asList(type.getEnumConstants()));
        }

        // treat as String if nothing matched above
        if (result == null) {
            result = new AjaxTextFieldPanel(id, fieldName, model);
        }

        result.hideLabel();
        return result;
    }
}
