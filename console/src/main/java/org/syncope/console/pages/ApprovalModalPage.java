/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.WorkflowFormPropertyTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.MapChoiceRenderer;
import org.syncope.console.rest.ApprovalRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;

public class ApprovalModalPage extends BaseModalPage {

    private static final long serialVersionUID = -8847854414429745216L;

    @SpringBean
    private ApprovalRestClient restClient;

    public ApprovalModalPage(final PageReference callerPageRef,
            final ModalWindow window, final WorkflowFormTO formTO) {

        super();

        IModel<List<WorkflowFormPropertyTO>> formProps =
                new LoadableDetachableModel<List<WorkflowFormPropertyTO>>() {

                    private static final long serialVersionUID =
                            3169142472626817508L;

                    @Override
                    protected List<WorkflowFormPropertyTO> load() {
                        return formTO.getProperties();
                    }
                };

        final ListView<WorkflowFormPropertyTO> propView =
                new ListView<WorkflowFormPropertyTO>("propView", formProps) {

                    private static final long serialVersionUID =
                            9101744072914090143L;

                    @Override
                    protected void populateItem(
                            final ListItem<WorkflowFormPropertyTO> item) {

                        final WorkflowFormPropertyTO prop =
                                item.getModelObject();

                        Label label = new Label("key",
                                prop.getName() == null
                                ? prop.getId() : prop.getName());
                        item.add(label);

                        FieldPanel field = null;
                        switch (prop.getType()) {
                            case Boolean:
                                field = new AjaxDropDownChoicePanel("value",
                                        label.getDefaultModelObjectAsString(),
                                        new Model(
                                        Boolean.valueOf(prop.getValue())),
                                        true).setChoices(Arrays.asList(
                                        new String[]{"Yes", "No"}));
                                break;

                            case Date:
                                DateFormat df = StringUtils.isNotBlank(prop.
                                        getDatePattern())
                                        ? new SimpleDateFormat(prop.
                                        getDatePattern())
                                        : new SimpleDateFormat();
                                Date parsedDate = null;
                                if (StringUtils.isNotBlank(prop.getValue())) {
                                    try {
                                        parsedDate = df.parse(prop.getValue());
                                    } catch (ParseException e) {
                                        LOG.error("Unparsable date: {}",
                                                prop.getValue(), e);
                                    }
                                }

                                field = new DateTimeFieldPanel("value",
                                        label.getDefaultModelObjectAsString(),
                                        new Model(parsedDate), true);
                                break;

                            case Enum:
                                MapChoiceRenderer<String, String> enumCR =
                                        new MapChoiceRenderer<String, String>(
                                        prop.getEnumValues());

                                field = new AjaxDropDownChoicePanel("value",
                                        label.getDefaultModelObjectAsString(),
                                        new Model(prop.getValue()),
                                        true).setChoiceRenderer(enumCR).
                                        setChoices(new Model() {

                                    private static final long serialVersionUID =
                                            -858521070366432018L;

                                    @Override
                                    public Serializable getObject() {
                                        return new ArrayList(
                                                prop.getEnumValues().
                                                keySet());
                                    }
                                });
                                break;

                            case Long:
                                field = new AjaxNumberFieldPanel("value",
                                        label.getDefaultModelObjectAsString(),
                                        new Model(
                                        Long.valueOf(prop.getValue())),
                                        true);
                                break;

                            case String:
                            default:
                                field = new AjaxTextFieldPanel("value",
                                        PARENT_PATH,
                                        new Model(prop.getValue()),
                                        true);
                                break;
                        }

                        field.setReadOnly(!prop.isWritable());
                        if (prop.isRequired()) {
                            field.addRequiredLabel();
                        }

                        item.add(field);
                    }
                };

        final AjaxButton submit = new IndicatingAjaxButton("apply", new Model(
                getString("submit"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                Map<String, WorkflowFormPropertyTO> props =
                        formTO.getPropertiesAsMap();

                for (int i = 0; i < propView.size(); i++) {
                    ListItem<WorkflowFormPropertyTO> item =
                            (ListItem<WorkflowFormPropertyTO>) propView.get(i);
                    String input = ((FieldPanel) item.get("value")).getField().
                            getInput();

                    if (!props.containsKey(item.getModelObject().getId())) {
                        props.put(item.getModelObject().getId(),
                                new WorkflowFormPropertyTO());
                    }

                    if (item.getModelObject().isWritable()) {
                        switch (item.getModelObject().getType()) {
                            case Boolean:
                                props.get(item.getModelObject().getId()).
                                        setValue(
                                        String.valueOf(input.equals("0")));
                                break;

                            case Date:
                            case Enum:
                            case String:
                            case Long:
                            default:
                                props.get(item.getModelObject().getId()).
                                        setValue(input);
                                break;
                        }
                    }
                }

                formTO.setProperties(props.values());
                try {
                    restClient.submitForm(formTO);

                    ((Todo) callerPageRef.getPage()).setModalResult(true);
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                    LOG.error("While submitting form {}", formTO, e);
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form<?> form) {

                target.add(feedbackPanel);
            }
        };

        Form form = new Form("form");
        form.add(propView);
        form.add(submit);

        MetaDataRoleAuthorizationStrategy.authorize(form, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Approval", "submit"));

        add(form);
    }
}
