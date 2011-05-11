/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.commons.SchemaWrapper;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;
import org.syncope.types.SchemaType;

/**
 * MembershipModalPage.
 */
public class MembershipModalPage extends BaseModalPage {

    @SpringBean
    private SchemaRestClient schemaRestClient;

    private List<SchemaWrapper> schemaWrappers = new ArrayList<SchemaWrapper>();

    private WebMarkupContainer container;

    private AjaxButton submit;

    public MembershipModalPage(final Page basePage, final ModalWindow window,
            final MembershipTO membershipTO, final boolean createFlag) {

        final Form form = new Form("MembershipForm");

        form.setModel(new CompoundPropertyModel(membershipTO));

        setupSchemaWrappers(createFlag, membershipTO);

        final IModel<List<String>> derivedSchemaNames =
                new LoadableDetachableModel<List<String>>() {

                    @Override
                    protected List<String> load() {
                        return schemaRestClient.getDerivedSchemaNames("membership");
                    }
                };

        final ListView userAttributesView = new ListView("membershipSchemas",
                schemaWrappers) {

            @Override
            protected void populateItem(ListItem item) {
                final SchemaWrapper schemaWrapper = (SchemaWrapper) item.getDefaultModelObject();

                final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

                item.add(new Label("name",
                        schemaWrapper.getSchemaTO().getName()));

                item.add(new ListView("fields", schemaWrapper.getValues()) {

                    Panel panel;

                    @Override
                    protected void populateItem(final ListItem item) {
                        String mandatoryCondition =
                                schemaTO.getMandatoryCondition();

                        boolean required = false;

                        if (mandatoryCondition.equalsIgnoreCase("true")) {
                            required = true;
                        }

                        if (schemaTO.getType() == SchemaType.String) {
                            panel = new AjaxTextFieldPanel("panel",
                                    schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    item.setModelObject((String) object);
                                }
                            }, required);
                        } else if (schemaTO.getType() == SchemaType.Boolean) {
                            panel = new AjaxCheckBoxPanel("panel", schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    //return (String) item.getModelObject();
                                    return "false";
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    Boolean val = (Boolean) object;
                                    item.setModelObject(val.toString());
                                }
                            }, required);

                        } else if (schemaTO.getType() == SchemaType.Date) {
                            panel = new DateFieldPanel("panel",
                                    schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    DateFormat formatter =
                                            new SimpleDateFormat(schemaTO.getConversionPattern());
                                    Date date = new Date();
                                    try {
                                        String dateValue = (String) item.getModelObject();
                                        formatter = new SimpleDateFormat(
                                                schemaTO.getConversionPattern());

                                        if (!dateValue.equals("")) {
                                            date = formatter.parse(
                                                    dateValue);
                                        } else {
                                            date = null;
                                        }
                                    } catch (ParseException e) {
                                        LOG.error(
                                                "While parsing a date",
                                                e);
                                    }
                                    return date;
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    Date date = (Date) object;
                                    Format formatter = new SimpleDateFormat(
                                            schemaTO.getConversionPattern());
                                    String val = formatter.format(date);
                                    item.setModelObject(val);
                                }
                            }, schemaTO.getConversionPattern(),
                                    required,
                                    schemaTO.isReadonly(), form);
                        }

                        item.add(panel);
                    }
                });

                AjaxButton addButton = new IndicatingAjaxButton("add",
                        new Model(getString("add"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        schemaWrapper.getValues().add("");

                        target.addComponent(container);
                    }
                };

                AjaxButton dropButton = new AjaxButton("drop",
                        new Model(getString("drop"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        //Drop the last component added
                        schemaWrapper.getValues().remove(schemaWrapper.getValues().
                                size() - 1);

                        target.addComponent(container);
                    }
                };

                if (schemaTO.getType() == SchemaType.Boolean) {
                    addButton.setVisible(false);
                    dropButton.setVisible(false);
                }

                addButton.setDefaultFormProcessing(false);
                addButton.setVisible(schemaTO.isMultivalue());

                dropButton.setDefaultFormProcessing(false);
                dropButton.setVisible(schemaTO.isMultivalue());

                if (schemaWrapper.getValues().size() == 1) {
                    dropButton.setVisible(false);
                }

                item.add(addButton);
                item.add(dropButton);
            }
        };

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                MembershipTO membershipTO =
                        (MembershipTO) form.getDefaultModelObject();

                membershipTO.setAttributes(getMembershipAttributes());

                if (createFlag) {
                    ((UserModalPage) basePage).getMembershipTOs().add(
                            membershipTO);
                } else {
                    ((UserModalPage) basePage).getMembershipTOs().remove(
                            membershipTO);
                    ((UserModalPage) basePage).getMembershipTOs().add(
                            membershipTO);
                }
                window.close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles = null;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Users", "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Users", "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER,
                allowedRoles);

        form.add(submit);


        container = new WebMarkupContainer("container");
        container.add(userAttributesView);
        container.setOutputMarkupId(true);

        form.add(container);

        //--------------------------------
        // Derived attributes container
        //--------------------------------

        //--------------------------------

        add(form);
    }

    public List<AttributeTO> getMembershipAttributes() {

        List<AttributeTO> attributes = new ArrayList<AttributeTO>();

        AttributeTO attribute;

        for (SchemaWrapper schemaWrapper : schemaWrappers) {

            attribute = new AttributeTO();
            attribute.setSchema(schemaWrapper.getSchemaTO().getName());
            attribute.setValues(new ArrayList<String>());

            for (String value : schemaWrapper.getValues()) {
                attribute.getValues().add(value);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    public void setupSchemaWrappers(boolean create, MembershipTO membershipTO) {
        schemaWrappers = new ArrayList<SchemaWrapper>();
        SchemaWrapper schemaWrapper;

        List<SchemaTO> schemas = schemaRestClient.getSchemas("membership");

        boolean found = false;

        if (create) {
            for (SchemaTO schema : schemas) {
                schemaWrapper = new SchemaWrapper(schema);
                schemaWrappers.add(schemaWrapper);
            }
        } else {
            for (SchemaTO schema : schemas) {
                for (AttributeTO attribute : membershipTO.getAttributes()) {
                    if (schema.getName().equals(attribute.getSchema())) {
                        schemaWrapper = new SchemaWrapper(schema);
                        schemaWrapper.setValues(attribute.getValues());
                        schemaWrappers.add(schemaWrapper);
                        found = true;
                    }
                }
                if (!found) {
                    schemaWrapper = new SchemaWrapper(schema);
                    schemaWrappers.add(schemaWrapper);
                } else {
                    found = false;
                }
            }
        }
    }

    private void setDerivedAttributeContainer(
            final Form form,
            final MembershipTO membershipTO,
            final IModel<List<String>> derivedSchemaNames) {
        final WebMarkupContainer derivedAttributesContainer =
                new WebMarkupContainer("derivedAttributesContainer");
        derivedAttributesContainer.setOutputMarkupId(true);
        form.add(derivedAttributesContainer);

        AjaxButton addDerivedAttributeBtn = new IndicatingAjaxButton(
                "addDerivedAttributeBtn",
                new Model(getString("addDerivedAttributeBtn"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                membershipTO.getDerivedAttributes().add(new AttributeTO());
                target.addComponent(derivedAttributesContainer);
            }
        };
        addDerivedAttributeBtn.setDefaultFormProcessing(false);
        form.add(addDerivedAttributeBtn);

        ListView<AttributeTO> derivedAttributes = new ListView<AttributeTO>(
                "derivedAttributes", membershipTO.getDerivedAttributes()) {

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO derivedAttributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        membershipTO.getDerivedAttributes().remove(
                                derivedAttributeTO);
                        item.getParent().removeAll();
                        target.addComponent(derivedAttributesContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final DropDownChoice<String> derivedSchemaChoice =
                        new DropDownChoice<String>(
                        "schema",
                        new PropertyModel<String>(derivedAttributeTO, "schema"),
                        derivedSchemaNames);

                derivedSchemaChoice.setOutputMarkupId(true);

                if (derivedAttributeTO.getSchema() != null) {
                    item.add(derivedSchemaChoice.setEnabled(Boolean.FALSE));
                } else {
                    item.add(derivedSchemaChoice.setRequired(true));
                }

                final List<String> values = derivedAttributeTO.getValues();

                if (values == null || values.isEmpty()) {
                    item.add(new TextField(
                            "derivedAttributeValue",
                            new Model(null)).setVisible(Boolean.FALSE));
                } else {
                    item.add(new TextField(
                            "derivedAttributeValue",
                            new Model(values.get(0))).setEnabled(
                            Boolean.FALSE));
                }
            }
        };
        derivedAttributes.setReuseItems(true);
        derivedAttributesContainer.add(derivedAttributes);
    }

    private void setVirtualAttributeContainer(
            final Form form,
            final MembershipTO membershipTO,
            final IModel<List<String>> virtualSchemaNames) {
        final WebMarkupContainer virtualAttributesContainer =
                new WebMarkupContainer("virtualAttributesContainer");
        virtualAttributesContainer.setOutputMarkupId(true);
        form.add(virtualAttributesContainer);

        AjaxButton addVirtualAttributeBtn = new IndicatingAjaxButton(
                "addVirtualAttributeBtn",
                new Model(getString("addVirtualAttributeBtn"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                membershipTO.getVirtualAttributes().add(new AttributeTO());
                target.addComponent(virtualAttributesContainer);
            }
        };
        addVirtualAttributeBtn.setDefaultFormProcessing(false);
        form.add(addVirtualAttributeBtn);

        ListView<AttributeTO> virtualAttributes = new ListView<AttributeTO>(
                "virtualAttributes", membershipTO.getVirtualAttributes()) {

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO virtualAttributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        membershipTO.getVirtualAttributes().remove(
                                virtualAttributeTO);
                        item.getParent().removeAll();
                        target.addComponent(virtualAttributesContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final DropDownChoice<String> virtualSchemaChoice =
                        new DropDownChoice<String>(
                        "schema",
                        new PropertyModel<String>(virtualAttributeTO, "schema"),
                        virtualSchemaNames);

                virtualSchemaChoice.setOutputMarkupId(true);

                if (virtualAttributeTO.getSchema() != null) {
                    item.add(virtualSchemaChoice.setEnabled(Boolean.FALSE));
                } else {
                    item.add(virtualSchemaChoice.setRequired(true));
                }

                final List<String> values = virtualAttributeTO.getValues();

                if (values == null || values.isEmpty()) {
                    item.add(new TextField(
                            "virtualAttributeValue",
                            new Model(null)).setVisible(Boolean.FALSE));
                } else {
                    item.add(new TextField(
                            "virtualAttributeValue",
                            new Model(values.get(0))).setEnabled(
                            Boolean.FALSE));
                }
            }
        };
        virtualAttributes.setReuseItems(true);
        virtualAttributesContainer.add(virtualAttributes);
    }
}
