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
package org.apache.syncope.client.console.reports;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.common.lib.report.Schema;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ClassUtils;
import org.apache.syncope.common.lib.report.SearchCondition;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.wicket.event.IEventSink;

public class ReportletWizardBuilder extends AjaxWizardBuilder<ReportletDirectoryPanel.ReportletWrapper> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final ReportRestClient restClient = new ReportRestClient();

    private final String report;

    public ReportletWizardBuilder(
            final String report,
            final ReportletDirectoryPanel.ReportletWrapper reportlet,
            final PageReference pageRef) {
        super(reportlet, pageRef);
        this.report = report;
    }

    public ReportletWizardBuilder setEventSink(final IEventSink eventSink) {
        this.eventSink = eventSink;
        return this;
    }

    @Override
    protected Serializable onApplyInternal(final ReportletDirectoryPanel.ReportletWrapper modelObject) {
        modelObject.getConf().setName(modelObject.getName());

        final ReportTO reportTO = restClient.read(report);

        if (modelObject.isNew()) {
            reportTO.getReportletConfs().add(modelObject.getConf());
        } else {
            CollectionUtils.filter(
                    reportTO.getReportletConfs(), new Predicate<AbstractReportletConf>() {

                @Override
                public boolean evaluate(final AbstractReportletConf object) {
                    return !object.getName().equals(modelObject.getOldName());
                }
            });
            reportTO.getReportletConfs().add(modelObject.getConf());
        }

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(modelObject.getConf());
        for (Map.Entry<String, Pair<AbstractFiqlSearchConditionBuilder, List<SearchClause>>> entry
                : modelObject.getSCondWrapper().entrySet()) {
            wrapper.setPropertyValue(entry.getKey(),
                    SearchUtils.buildFIQL(entry.getValue().getRight(), entry.getValue().getLeft()));
        }

        restClient.update(reportTO);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(
            final ReportletDirectoryPanel.ReportletWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Configuration(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        public Profile(final ReportletDirectoryPanel.ReportletWrapper reportlet) {

            final AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    "name", "reportlet", new PropertyModel<String>(reportlet, "name"), false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            final AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>(
                    "configuration", getString("configuration"), new PropertyModel<String>(reportlet, "conf") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return reportlet.getConf() == null ? null : reportlet.getConf().getClass().getName();
                }

                @Override
                public void setObject(final String object) {
                    AbstractReportletConf conf = null;

                    try {
                        conf = AbstractReportletConf.class.cast(Class.forName(object).newInstance());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        LOG.warn("Error retrieving reportlet configuration instance", e);
                    }

                    reportlet.setConf(conf);
                }
            });

            conf.setChoices(new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getReportletConfs()));

            conf.addRequiredLabel();
            add(conf);
        }
    }

    public class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        private final String[] excluded = new String[] { "serialVersionUID", "class", "name", "reportletClassName" };

        private final ReportletDirectoryPanel.ReportletWrapper reportlet;

        public Configuration(final ReportletDirectoryPanel.ReportletWrapper reportlet) {
            this.reportlet = reportlet;

            LoadableDetachableModel<List<String>> propViewModel = new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID = 5275935387613157437L;

                @Override
                protected List<String> load() {
                    List<String> result = new ArrayList<String>();
                    if (Configuration.this.reportlet.getConf() != null) {
                        for (Field field : Configuration.this.reportlet.getConf().getClass().getDeclaredFields()) {
                            if (!ArrayUtils.contains(excluded, field.getName())) {
                                result.add(field.getName());
                            }
                        }
                    }

                    return result;
                }
            };

            add(new ListView<String>("propView", propViewModel) {

                private static final long serialVersionUID = 9101744072914090143L;

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                protected void populateItem(final ListItem<String> item) {
                    final String fieldName = item.getModelObject();

                    item.add(new Label("fieldName", new ResourceModel(fieldName, fieldName)));

                    Field field = null;
                    try {
                        field = reportlet.getConf().getClass().getDeclaredField(fieldName);
                    } catch (NoSuchFieldException | SecurityException e) {
                        LOG.error("Could not find field {} in class {}", fieldName, reportlet.getConf().getClass(), e);
                    }

                    if (field == null) {
                        return;
                    }

                    final SearchCondition scondAnnot = field.getAnnotation(SearchCondition.class);
                    final Schema schemaAnnot = field.getAnnotation(Schema.class);

                    BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(reportlet.getConf());

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
                                        new ListModel<SearchClause>(clauses)).required(false).build("value");
                                builder = SyncopeClient.getUserSearchConditionBuilder();
                                break;
                            case "GROUP":
                                panel = new GroupSearchPanel.Builder(
                                        new ListModel<SearchClause>(clauses)).required(false).build("value");
                                builder = SyncopeClient.getGroupSearchConditionBuilder();
                                break;
                            default:
                                panel = new AnyObjectSearchPanel.Builder(
                                        scondAnnot.type(),
                                        new ListModel<SearchClause>(clauses)).required(false).build("value");
                                builder = SyncopeClient.getAnyObjectSearchConditionBuilder(null);
                        }

                        reportlet.getSCondWrapper().put(fieldName, Pair.of(builder, clauses));
                    } else if (List.class.equals(field.getType())) {
                        Class<?> listItemType = String.class;
                        if (field.getGenericType() instanceof ParameterizedType) {
                            listItemType = (Class<?>) ((ParameterizedType) field.getGenericType()).
                                    getActualTypeArguments()[0];
                        }

                        if (listItemType.equals(String.class) && schemaAnnot != null) {
                            SchemaRestClient schemaRestClient = new SchemaRestClient();

                            List<AbstractSchemaTO> choices;
                            switch (schemaAnnot.schema()) {
                                case UserPlainSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.PLAIN, AnyTypeKind.USER);
                                    break;

                                case UserDerivedSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.DERIVED, AnyTypeKind.USER);
                                    break;

                                case UserVirtualSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.VIRTUAL, AnyTypeKind.USER);
                                    break;

                                case GroupPlainSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.PLAIN, AnyTypeKind.GROUP);
                                    break;

                                case GroupDerivedSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.DERIVED, AnyTypeKind.GROUP);
                                    break;

                                case GroupVirtualSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.VIRTUAL, AnyTypeKind.GROUP);
                                    break;

                                case AnyObjectPlainSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.PLAIN, AnyTypeKind.ANY_OBJECT);
                                    break;

                                case AnyObjectDerivedSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.DERIVED, AnyTypeKind.ANY_OBJECT);
                                    break;

                                case AnyObjectVirtualSchema:
                                    choices = schemaRestClient.getSchemas(SchemaType.VIRTUAL, AnyTypeKind.ANY_OBJECT);
                                    break;

                                default:
                                    choices = Collections.emptyList();
                            }

                            panel = new AjaxPalettePanel.Builder<String>().setName(fieldName).build(
                                    "value",
                                    new PropertyModel<List<String>>(reportlet.getConf(), fieldName),
                                    new ListModel<String>(
                                            CollectionUtils.collect(
                                                    choices, new Transformer<AbstractSchemaTO, String>() {

                                                @Override
                                                public String transform(final AbstractSchemaTO input) {
                                                    return input.getKey();
                                                }
                                            }, new ArrayList<String>()))).hideLabel();
                        } else if (listItemType.isEnum()) {
                            panel = new AjaxPalettePanel.Builder<String>().setName(fieldName).build(
                                    "value",
                                    new PropertyModel<List<String>>(reportlet.getConf(), fieldName),
                                    new ListModel(Arrays.asList(listItemType.getEnumConstants()))).hideLabel();
                        } else {
                            if (((List) wrapper.getPropertyValue(fieldName)).isEmpty()) {
                                ((List) wrapper.getPropertyValue(fieldName)).add(null);
                            }

                            panel = new MultiFieldPanel.Builder<String>(
                                    new PropertyModel<List<String>>(reportlet.getConf(), fieldName)).build(
                                    "value",
                                    fieldName,
                                    buildSinglePanel(field.getType(), fieldName, "panel")).hideLabel();
                        }
                    } else {
                        panel = buildSinglePanel(field.getType(), fieldName, "value").hideLabel();
                    }

                    item.add(panel.setRenderBodyOnly(true));
                }
            });
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private FieldPanel buildSinglePanel(final Class<?> type, final String fieldName, final String id) {
            FieldPanel result = null;
            PropertyModel model = new PropertyModel(reportlet.getConf(), fieldName);
            if (ClassUtils.isAssignable(Boolean.class, type)) {
                result = new AjaxCheckBoxPanel(id, fieldName, model);
            } else if (ClassUtils.isAssignable(Number.class, type)) {
                result = new AjaxSpinnerFieldPanel.Builder<Number>().build(
                        id, fieldName, (Class<Number>) ClassUtils.resolvePrimitiveIfNecessary(type), model);
            } else if (Date.class.equals(type)) {
                result = new DateTimeFieldPanel(id, fieldName, model, SyncopeConstants.DEFAULT_DATE_PATTERN);
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
}
