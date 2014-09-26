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
import java.util.Map;
import java.util.TreeMap;
import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SecurityQuestionPanel extends Panel {

    private static final long serialVersionUID = -790642213865180146L;

    private final Map<Long, String> questions = new TreeMap<Long, String>();

    @SpringBean
    private SecurityQuestionRestClient restClient;

    public SecurityQuestionPanel(final String id, final UserTO userTO) {
        super(id);
        setOutputMarkupId(true);

        for (SecurityQuestionTO secQues : restClient.list()) {
            questions.put(secQues.getId(), secQues.getContent());
        }

        final AjaxTextFieldPanel securityAnswer = new AjaxTextFieldPanel("securityAnswer", "securityAnswer",
                new PropertyModel<String>(userTO, "securityAnswer"));
        securityAnswer.getField().setOutputMarkupId(true);
        securityAnswer.setEnabled(false);
        add(securityAnswer);

        final AjaxDropDownChoicePanel<Long> securityQuestion =
                new AjaxDropDownChoicePanel<Long>("securityQuestion", "securityQuestion",
                        new PropertyModel<Long>(userTO, "securityQuestion"));
        ((DropDownChoice) securityQuestion.getField()).setNullValid(true);
        securityQuestion.setChoices(new ArrayList<Long>(questions.keySet()));
        securityQuestion.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
        securityQuestion.getField().setOutputMarkupId(true);
        securityQuestion.setChoiceRenderer(new IChoiceRenderer<Long>() {

            private static final long serialVersionUID = 2693996850376268294L;

            @Override
            public Object getDisplayValue(final Long object) {
                return questions.get(object);
            }

            @Override
            public String getIdValue(final Long object, final int index) {
                return questions.get(object);
            }
        });
        securityQuestion.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (securityQuestion.getModelObject() == null) {
                    securityAnswer.setModelObject(null);
                } else {
                    securityAnswer.setEnabled(true);
                }
                target.add(SecurityQuestionPanel.this);
            }
        });
        add(securityQuestion);
    }
}
