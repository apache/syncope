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
package org.apache.syncope.console.pages;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.syncope.annotation.FormAttributeField;
import org.apache.syncope.client.SyncopeConstants;
import org.apache.syncope.client.report.ReportletConf;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.console.pages.panels.UserSearchPanel;
import org.apache.syncope.console.rest.ReportRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.CheckBoxMultipleChoiceFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.syncope.types.AttributableType;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ClassUtils;

public class ReportletConfModalPage extends BaseModalPage {

    private static final long serialVersionUID = 3910027601200382958L;

    private static final String[] EXCLUDE_PROPERTIES = new String[]{"serialVersionUID", "class", "name",
        "reportletClassName"};

    @SpringBean
    private ReportRestClient restClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    private ReportletConf reportletConf;

    private final PageReference pageRef;

    private final AjaxTextFieldPanel name;

    private WebMarkupContainer propertiesContainer;

    private ListView<String> propView;

    public ReportletConfModalPage(final ReportletConf reportletConf, final ModalWindow window,
            final PageReference pageRef) {

        this.reportletConf = reportletConf;
        this.pageRef = pageRef;

        Form form = new Form("form");
        add(form);

        propertiesContainer = new WebMarkupContainer("container");
        propertiesContainer.setOutputMarkupId(true);
        form.add(propertiesContainer);

        name = new AjaxTextFieldPanel("name", "name", this.reportletConf == null
                ? new Model()
                : new PropertyModel<String>(this.reportletConf, "name"));
        name.setOutputMarkupId(true);
        name.addRequiredLabel();
        form.add(name);

        final AjaxDropDownChoicePanel<String> reportletClass = new AjaxDropDownChoicePanel<String>("reportletClass",
                "reportletClass", new IModel<String>() {

            private static final long serialVersionUID = -2316468110411802130L;

            @Override
            public String getObject() {
                return ReportletConfModalPage.this.reportletConf == null
                        ? null
                        : ReportletConfModalPage.this.reportletConf.getClass().getName();
            }

            @Override
            public void setObject(final String object) {
                try {
                    Class reportletClass = Class.forName(object);
                    ReportletConfModalPage.this.reportletConf = (ReportletConf) reportletClass.newInstance();
                    propertiesContainer.replace(buildPropView());
                } catch (Exception e) {
                    LOG.error("Cannot find or initialize {}", object, e);
                }
            }

            @Override
            public void detach() {
            }
        });
        reportletClass.setStyleShet("long_dynamicsize");
        reportletClass.setChoices(restClient.getReportletConfClasses());
        ((DropDownChoice) reportletClass.getField()).setNullValid(true);
        reportletClass.addRequiredLabel();
        reportletClass.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((DropDownChoice) reportletClass.getField()).setNullValid(false);
                target.add(reportletClass.getField());
                target.add(propertiesContainer);
            }
        });
        form.add(reportletClass);

        propertiesContainer.add(buildPropView());

        final AjaxButton submit = new AjaxButton("apply", new ResourceModel("apply")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final BeanWrapper wrapper = PropertyAccessorFactory
                        .forBeanPropertyAccess(ReportletConfModalPage.this.reportletConf);
                wrapper.setPropertyValue("name", name.getField().getInput());

                // Iterate over properties in order to find UserSearchPanel instances and manually update
                // this.reportletConf with select search criteria - this is needed because UserSearchPanel
                // does not comply with usual Wicket model paradigm.
                for (Iterator<Component> itor = ReportletConfModalPage.this.propView.visitChildren(); itor.hasNext();) {
                    Component component = itor.next();
                    if (component instanceof UserSearchPanel) {
                        // using component.getDefaultModelObjectAsString() to fetch field name (set above)
                        wrapper.setPropertyValue(component.getDefaultModelObjectAsString(),
                                ((UserSearchPanel) component).buildSearchCond());
                    }
                }

                ((ReportModalPage) pageRef.getPage())
                        .setModalReportletConf(ReportletConfModalPage.this.reportletConf);
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };
        form.add(submit);
    }

    private FieldPanel buildSinglePanel(final Class<?> type, final String fieldName, final String id) {
        FieldPanel result = null;
        PropertyModel model = new PropertyModel(ReportletConfModalPage.this.reportletConf, fieldName);
        if (Boolean.TYPE.equals(type) || Boolean.class.equals(type)) {
            result = new AjaxCheckBoxPanel(id, fieldName, model);
        } else if (Integer.TYPE.equals(type) || Integer.class.equals(type) || Long.TYPE.equals(type)
                || Long.class.equals(type) || Double.TYPE.equals(type) || Double.class.equals(type)) {

            result = new AjaxNumberFieldPanel(id, fieldName, model, ClassUtils.resolvePrimitiveIfNecessary(type));
        } else if (Date.class.equals(type)) {
            result = new DateTimeFieldPanel(id, fieldName, model, SyncopeConstants.DEFAULT_DATE_PATTERN);
        } else if (type.isEnum()) {
            result = new AjaxDropDownChoicePanel(id, fieldName, model).setChoices(Arrays
                    .asList(type.getEnumConstants()));
        }

        // treat as String if nothing matched above
        if (result == null) {
            result = new AjaxTextFieldPanel(id, fieldName, model);
        }

        return result;
    }

    private ListView<String> buildPropView() {
        LoadableDetachableModel<List<String>> propViewModel = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<String> result = new ArrayList<String>();
                if (ReportletConfModalPage.this.reportletConf != null) {
                    for (Field field : ReportletConfModalPage.this.reportletConf.getClass().getDeclaredFields()) {
                        if (!ArrayUtils.contains(EXCLUDE_PROPERTIES, field.getName())) {
                            result.add(field.getName());
                        }
                    }
                }

                return result;
            }
        };

        propView = new ListView<String>("propView", propViewModel) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String fieldName = item.getModelObject();

                Label label = new Label("key", fieldName);
                item.add(label);

                Field field = null;
                try {
                    field = ReportletConfModalPage.this.reportletConf.getClass().getDeclaredField(fieldName);
                } catch (Exception e) {
                    LOG.error("Could not find field {} in class {}", new Object[]{fieldName,
                                ReportletConfModalPage.this.reportletConf.getClass(), e});
                }
                if (field == null) {
                    return;
                }

                BeanWrapper wrapper = PropertyAccessorFactory
                        .forBeanPropertyAccess(ReportletConfModalPage.this.reportletConf);

                Panel panel;

                if (NodeCond.class.equals(field.getType())) {
                    panel = new UserSearchPanel("value", (NodeCond) wrapper.getPropertyValue(fieldName),
                            false, pageRef);
                    // This is needed in order to manually update this.reportletConf with search panel selections
                    panel.setDefaultModel(new Model(fieldName));
                } else if (List.class.equals(field.getType())) {
                    if (wrapper.getPropertyValue(fieldName) == null) {
                        wrapper.setPropertyValue(fieldName, new ArrayList());
                    }

                    Class<?> listItemType = String.class;
                    if (field.getGenericType() instanceof ParameterizedType) {
                        listItemType =
                                (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    }

                    FormAttributeField annotation = field.getAnnotation(FormAttributeField.class);

                    if (listItemType.equals(String.class) && annotation != null) {
                        List<String> choices;
                        switch (annotation.schema()) {
                            case UserSchema:
                                choices = schemaRestClient.getSchemaNames(AttributableType.USER);
                                break;

                            case UserDerivedSchema:
                                choices = schemaRestClient.getDerivedSchemaNames(AttributableType.USER);
                                break;

                            case UserVirtualSchema:
                                choices = schemaRestClient.getVirtualSchemaNames(AttributableType.USER);
                                break;

                            case RoleSchema:
                                choices = schemaRestClient.getSchemaNames(AttributableType.ROLE);
                                break;

                            case RoleDerivedSchema:
                                choices = schemaRestClient.getDerivedSchemaNames(AttributableType.ROLE);
                                break;

                            case RoleVirtualSchema:
                                choices = schemaRestClient.getVirtualSchemaNames(AttributableType.ROLE);
                                break;

                            case MembershipSchema:
                                choices = schemaRestClient.getSchemaNames(AttributableType.MEMBERSHIP);
                                break;

                            case MembershipDerivedSchema:
                                choices = schemaRestClient.getDerivedSchemaNames(AttributableType.MEMBERSHIP);
                                break;

                            case MembershipVirtualSchema:
                                choices = schemaRestClient.getVirtualSchemaNames(AttributableType.MEMBERSHIP);
                                break;

                            default:
                                choices = Collections.emptyList();
                        }

                        panel = new AjaxPalettePanel("value", new PropertyModel<List<String>>(
                                ReportletConfModalPage.this.reportletConf, fieldName), new ListModel<String>(choices),
                                true);
                    } else if (listItemType.isEnum()) {
                        panel = new CheckBoxMultipleChoiceFieldPanel("value", new PropertyModel(
                                ReportletConfModalPage.this.reportletConf, fieldName), new ListModel(Arrays
                                .asList(listItemType.getEnumConstants())));
                    } else {
                        if (((List) wrapper.getPropertyValue(fieldName)).isEmpty()) {
                            ((List) wrapper.getPropertyValue(fieldName)).add(null);
                        }

                        panel = new MultiValueSelectorPanel("value", new PropertyModel<List>(
                                ReportletConfModalPage.this.reportletConf, fieldName), buildSinglePanel(
                                field.getType(), fieldName, "panel"));
                    }
                } else {
                    panel = buildSinglePanel(field.getType(), fieldName, "value");
                }

                item.add(panel);
            }
        };

        return propView;
    }
}
