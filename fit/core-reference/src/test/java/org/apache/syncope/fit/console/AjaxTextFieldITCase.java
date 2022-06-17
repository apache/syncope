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
package org.apache.syncope.fit.console;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.validation.validator.StringValidator;
import org.junit.jupiter.api.Test;

public class AjaxTextFieldITCase extends AbstractConsoleITCase {

    private static final IModel<String> TEXT_MODEL = Model.of((String) null);

    @Test
    public void emptyInputConvertedToNull() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, TEXT_MODEL));
        TESTER.startPage(testPage);
        FormTester formTester = TESTER.newFormTester(testPage.getForm().getId());
        formTester.setValue("field:textField", "");
        formTester.submit();
        assertNull(testPage.getFieldPanel().getField().getDefaultModelObject());
    }

    @Test
    public void valueAttribute() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, TEXT_MODEL));
        String text = "sometext";
        TEXT_MODEL.setObject(text);
        TESTER.startPage(testPage);
        assertTrue(TESTER.getLastResponseAsString().contains(Strings.escapeMarkup(text)));
    }

    @Test
    public void nullIsNotValidated() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, TEXT_MODEL));
        testPage.getFieldPanel().getField().setRequired(false);
        testPage.getFieldPanel().getField().add(StringValidator.minimumLength(2));
        TESTER.startPage(testPage);
        FormTester formTester = TESTER.newFormTester(testPage.getForm().getId());
        formTester.setValue("field:textField", "");
        formTester.submit();
        assertNull(testPage.getFieldPanel().getDefaultModelObject());
        assertTrue(testPage.getFieldPanel().getField().isValid());
    }

    @Test
    public void requiredAttribute() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, TEXT_MODEL));
        testPage.getFieldPanel().setOutputMarkupId(true);
        testPage.getFieldPanel().getField().setRequired(true);
        TESTER.startPage(testPage);
        TESTER.assertLabel("form:field:field-label", "field");
        TESTER.assertVisible("form:field:required");
        TESTER.assertVisible("form:field:externalAction");
    }
}
