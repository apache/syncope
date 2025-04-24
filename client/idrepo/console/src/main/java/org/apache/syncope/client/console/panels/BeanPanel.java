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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxGridFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.report.SearchCondition;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.core.util.lang.PropertyResolverConverter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

public class BeanPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 3905038169553185171L;

    protected static final Logger LOG = LoggerFactory.getLogger(BeanPanel.class);

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    protected final List<String> excluded;

    protected final Map<String, Pair<AbstractFiqlSearchConditionBuilder<?, ?, ?>, List<SearchClause>>> sCondWrapper;

    public BeanPanel(final String id, final IModel<T> bean, final PageReference pageRef, final String... excluded) {
        this(id, bean, null, pageRef, excluded);
    }

    public BeanPanel(
            final String id,
            final IModel<T> bean,
            final Map<String, Pair<AbstractFiqlSearchConditionBuilder<?, ?, ?>, List<SearchClause>>> sCondWrapper,
            final PageReference pageRef,
            final String... excluded) {

        super(id, bean);
        setOutputMarkupId(true);

        this.sCondWrapper = sCondWrapper;

        this.excluded = new ArrayList<>(List.of(excluded));
        this.excluded.add("serialVersionUID");
        this.excluded.add("class");

        LoadableDetachableModel<List<Field>> model = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<Field> load() {
                List<Field> result = new ArrayList<>();

                if (BeanPanel.this.getDefaultModelObject() != null) {
                    ReflectionUtils.doWithFields(
                            BeanPanel.this.getDefaultModelObject().getClass(),
                            result::add,
                            field -> !field.isSynthetic() && !BeanPanel.this.excluded.contains(field.getName()));
                }

                return result;
            }
        };

        add(new ListView<>("propView", model) {

            private static final long serialVersionUID = 9101744072914090143L;

            private void setRequired(final ListItem<Field> item, final boolean required) {
                if (required) {
                    Fragment fragment = new Fragment("required", "requiredFragment", this);
                    fragment.add(new Label("requiredLabel", "*"));
                    item.replace(fragment);
                }
            }

            private void setDescription(final ListItem<Field> item, final String description) {
                Fragment fragment = new Fragment("description", "descriptionFragment", this);
                fragment.add(new Label("descriptionLabel", Model.of()).add(new PopoverBehavior(
                        Model.of(),
                        Model.of(description),
                        new PopoverConfig().withPlacement(TooltipConfig.Placement.right)) {

                    private static final long serialVersionUID = -7867802555691605021L;

                    @Override
                    protected String createRelAttribute() {
                        return "description";
                    }
                }).setRenderBodyOnly(false));
                item.replace(fragment);
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected void populateItem(final ListItem<Field> item) {
                item.add(new Fragment("required", "emptyFragment", this));
                item.add(new Fragment("description", "emptyFragment", this));

                Field field = item.getModelObject();

                item.add(new Label("fieldName", new ResourceModel(field.getName(), field.getName())));

                Panel panel;

                SearchCondition scondAnnot = field.getAnnotation(SearchCondition.class);
                if (scondAnnot != null) {
                    BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean.getObject());
                    String fiql = (String) wrapper.getPropertyValue(field.getName());

                    List<SearchClause> clauses = Optional.ofNullable(fiql).
                            map(f -> SearchUtils.getSearchClauses(f.replaceAll(
                            SearchUtils.getTypeConditionPattern(scondAnnot.type()).pattern(), ""))).
                        orElseGet(ArrayList::new);

                    AbstractFiqlSearchConditionBuilder<?, ?, ?> builder;
                    switch (scondAnnot.type()) {
                        case "USER":
                            panel = new UserSearchPanel.Builder(
                                    new ListModel<>(clauses), pageRef).required(false).build("value");
                            builder = SyncopeClient.getUserSearchConditionBuilder();
                            break;

                        case "GROUP":
                            panel = new GroupSearchPanel.Builder(
                                    new ListModel<>(clauses), pageRef).required(false).build("value");
                            builder = SyncopeClient.getGroupSearchConditionBuilder();
                            break;

                        default:
                            panel = new AnyObjectSearchPanel.Builder(
                                    scondAnnot.type(),
                                    new ListModel<>(clauses), pageRef).required(false).build("value");
                            builder = SyncopeClient.getAnyObjectSearchConditionBuilder(scondAnnot.type());
                    }

                    Optional.ofNullable(BeanPanel.this.sCondWrapper).
                            ifPresent(scw -> scw.put(field.getName(), Pair.of(builder, clauses)));
                } else if (List.class.equals(field.getType())) {
                    Class<?> listItemType = field.getGenericType() instanceof ParameterizedType parameterizedType
                            ? (Class<?>) (parameterizedType).getActualTypeArguments()[0]
                            : String.class;

                    org.apache.syncope.common.lib.Schema schema =
                            field.getAnnotation(org.apache.syncope.common.lib.Schema.class);
                    if (listItemType.equals(String.class) && schema != null) {
                        List<SchemaTO> choices = new ArrayList<>();

                        for (SchemaType type : schema.type()) {
                            switch (type) {
                                case PLAIN:
                                    choices.addAll(
                                            schemaRestClient.getSchemas(SchemaType.PLAIN, schema.anyTypeKind()));
                                    break;

                                case DERIVED:
                                    choices.addAll(
                                            schemaRestClient.getSchemas(SchemaType.DERIVED, schema.anyTypeKind()));
                                    break;

                                case VIRTUAL:
                                    choices.addAll(
                                            schemaRestClient.getSchemas(SchemaType.VIRTUAL, schema.anyTypeKind()));
                                    break;

                                default:
                            }
                        }

                        panel = new AjaxPalettePanel.Builder<>().setName(field.getName()).build(
                                "value",
                                new PropertyModel<>(bean.getObject(), field.getName()),
                                new ListModel<>(choices.stream().map(SchemaTO::getKey).collect(Collectors.toList()))).
                                hideLabel();
                    } else if (listItemType.isEnum()) {
                        panel = new AjaxPalettePanel.Builder<>().setName(field.getName()).build(
                                "value",
                                new PropertyModel<>(bean.getObject(), field.getName()),
                                new ListModel(List.of(listItemType.getEnumConstants()))).hideLabel();
                    } else {
                        Triple<FieldPanel, Boolean, Optional<String>> single =
                                buildSinglePanel(bean.getObject(), field.getType(), field.getName(),
                                        field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class), "panel");

                        setRequired(item, single.getMiddle());
                        single.getRight().ifPresent(description -> setDescription(item, description));

                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(bean.getObject(), field.getName())).build(
                                "value",
                                field.getName(),
                                single.getLeft()).hideLabel();
                    }
                } else if (Map.class.equals(field.getType())) {
                    panel = new AjaxGridFieldPanel(
                            "value", field.getName(), new PropertyModel<>(bean, field.getName())).hideLabel();
                    Optional.ofNullable(field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class)).
                            ifPresent(annot -> setDescription(item, annot.description()));
                } else {
                    Triple<FieldPanel, Boolean, Optional<String>> single =
                            buildSinglePanel(bean.getObject(), field.getType(), field.getName(),
                                    field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class), "value");

                    setRequired(item, single.getMiddle());
                    single.getRight().ifPresent(description -> setDescription(item, description));

                    panel = single.getLeft().hideLabel();
                }

                item.add(panel.setRenderBodyOnly(true));
            }
        }.setReuseItems(false));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Triple<FieldPanel, Boolean, Optional<String>> buildSinglePanel(
            final Serializable bean, final Class<?> type, final String fieldName,
            final io.swagger.v3.oas.annotations.media.Schema schema, final String id) {

        PropertyModel model = new PropertyModel<>(bean, fieldName);

        FieldPanel panel;
        if (ClassUtils.isAssignable(Boolean.class, type)) {
            panel = new AjaxCheckBoxPanel(id, fieldName, model);
        } else if (ClassUtils.isAssignable(Number.class, type)) {
            panel = new AjaxNumberFieldPanel.Builder<>().build(
                    id, fieldName, AjaxNumberFieldPanel.cast(ClassUtils.resolvePrimitiveIfNecessary(type)), model);
        } else if (Date.class.equals(type)) {
            panel = new AjaxDateTimeFieldPanel(id, fieldName, model,
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
        } else if (OffsetDateTime.class.equals(type)) {
            panel = new AjaxDateTimeFieldPanel(id, fieldName, DateOps.WrappedDateModel.ofOffset(model),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
        } else if (ZonedDateTime.class.equals(type)) {
            panel = new AjaxDateTimeFieldPanel(id, fieldName, DateOps.WrappedDateModel.ofZoned(model),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
        } else if (LocalDateTime.class.equals(type)) {
            panel = new AjaxDateTimeFieldPanel(id, fieldName, DateOps.WrappedDateModel.ofLocal(model),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
        } else if (type.isEnum()) {
            panel = new AjaxDropDownChoicePanel(id, fieldName, model).
                    setChoices(List.of(type.getEnumConstants()));
        } else if (Duration.class.equals(type)) {
            panel = new AjaxTextFieldPanel(id, fieldName, new IModel<>() {

                private static final long serialVersionUID = 807008909842554829L;

                @Override
                public String getObject() {
                    return Optional.ofNullable(PropertyResolver.getValue(fieldName, bean)).
                            map(Object::toString).orElse(null);
                }

                @Override
                public void setObject(final String object) {
                    PropertyResolverConverter prc = new PropertyResolverConverter(
                            SyncopeWebApplication.get().getConverterLocator(),
                            SyncopeConsoleSession.get().getLocale());
                    PropertyResolver.setValue(fieldName, bean, Duration.parse(object), prc);
                }
            });
        } else {
            // treat as String if nothing matched above
            panel = new AjaxTextFieldPanel(id, fieldName, model);
        }

        boolean required = false;
        Optional<String> description = Optional.empty();

        if (schema != null) {
            panel.setReadOnly(schema.accessMode() == Schema.AccessMode.READ_ONLY);

            required = schema.requiredMode() == Schema.RequiredMode.REQUIRED;
            panel.setRequired(required);

            Optional.ofNullable(schema.example()).ifPresent(panel::setPlaceholder);

            description = Optional.ofNullable(schema.description());

            if (panel instanceof final AjaxTextFieldPanel components
                    && panel.getModelObject() == null
                    && schema.defaultValue() != null) {

                components.setModelObject(schema.defaultValue());
            }
        }

        return Triple.of(panel, required, description);
    }
}
