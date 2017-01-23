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
package org.apache.syncope.client.console.approvals;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.console.commons.MapChoiceRenderer;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Approval extends Panel {

    private static final long serialVersionUID = -8847854414429745216L;

    protected static final Logger LOG = LoggerFactory.getLogger(Approval.class);

    public Approval(final PageReference pageRef, final WorkflowFormTO formTO) {
        super(MultilevelPanel.FIRST_LEVEL_ID);

        IModel<List<WorkflowFormPropertyTO>> formProps = new LoadableDetachableModel<List<WorkflowFormPropertyTO>>() {

            private static final long serialVersionUID = 3169142472626817508L;

            @Override
            protected List<WorkflowFormPropertyTO> load() {
                return formTO.getProperties();
            }
        };

        final ListView<WorkflowFormPropertyTO> propView = new ListView<WorkflowFormPropertyTO>("propView", formProps) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void populateItem(final ListItem<WorkflowFormPropertyTO> item) {
                final WorkflowFormPropertyTO prop = item.getModelObject();

                String label = StringUtils.isBlank(prop.getName()) ? prop.getId() : prop.getName();

                FieldPanel field;
                switch (prop.getType()) {
                    case Boolean:
                        field = new AjaxDropDownChoicePanel("value", label, new PropertyModel<String>(prop, "value") {

                            private static final long serialVersionUID = -3743432456095828573L;

                            @Override
                            public String getObject() {
                                return StringUtils.isBlank(prop.getValue())
                                        ? null
                                        : prop.getValue().equals("true") ? "Yes" : "No";
                            }

                            @Override
                            public void setObject(final String object) {
                                prop.setValue(String.valueOf(object.equalsIgnoreCase("yes")));
                            }

                        }, false).setChoices(Arrays.asList(new String[] { "Yes", "No" }));
                        break;

                    case Date:
                        final FastDateFormat formatter = FastDateFormat.getInstance(prop.getDatePattern());
                        field = new AjaxDateTimeFieldPanel("value", label, new PropertyModel<Date>(prop, "value") {

                            private static final long serialVersionUID = -3743432456095828573L;

                            @Override
                            public Date getObject() {
                                try {
                                    if (StringUtils.isBlank(prop.getValue())) {
                                        return null;
                                    } else {
                                        return formatter.parse(prop.getValue());
                                    }
                                } catch (ParseException e) {
                                    LOG.error("Unparsable date: {}", prop.getValue(), e);
                                    return null;
                                }
                            }

                            @Override
                            public void setObject(final Date object) {
                                prop.setValue(formatter.format(object));
                            }

                        }, prop.getDatePattern());
                        break;

                    case Enum:
                        MapChoiceRenderer<String, String> enumCR = new MapChoiceRenderer<>(prop.getEnumValues());

                        field = new AjaxDropDownChoicePanel(
                                "value", label, new PropertyModel<String>(prop, "value"), false).
                                setChoiceRenderer(enumCR).setChoices(new Model<ArrayList<String>>() {

                            private static final long serialVersionUID = -858521070366432018L;

                            @Override
                            public ArrayList<String> getObject() {
                                return new ArrayList<>(prop.getEnumValues().keySet());
                            }
                        });
                        break;

                    case Long:
                        field = new AjaxSpinnerFieldPanel.Builder<Long>().build(
                                "value",
                                label,
                                Long.class,
                                new PropertyModel<Long>(prop, "value") {

                            private static final long serialVersionUID = -7688359318035249200L;

                            @Override
                            public Long getObject() {
                                return StringUtils.isBlank(prop.getValue())
                                        ? null
                                        : NumberUtils.toLong(prop.getValue());
                            }

                            @Override
                            public void setObject(final Long object) {
                                prop.setValue(String.valueOf(object));
                            }
                        });
                        break;

                    case String:
                    default:
                        field = new AjaxTextFieldPanel("value", label, new PropertyModel<String>(prop, "value"), false);
                        break;
                }

                field.setReadOnly(!prop.isWritable());
                if (prop.isRequired()) {
                    field.addRequiredLabel();
                }

                item.add(field);
            }
        };

        final AjaxLink<String> userDetails = new AjaxLink<String>("userDetails") {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                viewDetails(formTO, target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(userDetails, ENABLE, StandardEntitlement.USER_READ);

        add(propView);
        add(userDetails);
    }

    protected abstract void viewDetails(final WorkflowFormTO formTO, final AjaxRequestTarget target);
}
