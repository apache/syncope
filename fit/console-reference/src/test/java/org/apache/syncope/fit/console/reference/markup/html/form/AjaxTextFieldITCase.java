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
package org.apache.syncope.fit.console.reference.markup.html.form;

import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.fit.console.reference.AbstractITCase;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.validation.validator.StringValidator;
import org.junit.Test;
import org.apache.syncope.fit.console.reference.commons.TestPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AjaxTextFieldITCase extends AbstractITCase {

    private final IModel<String> textModel = Model.of((String) null);

    @Test
    public void emptyInputConvertedToNull() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, textModel));
        wicketTester.startPage(testPage);
        FormTester formTester = wicketTester.newFormTester(testPage.getForm().getId());
        formTester.setValue("field:textField", "");
        formTester.submit();
        assertEquals(null, testPage.getFieldPanel().getField().getDefaultModelObject());
    }

    @Test
    public void valueAttribute() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, textModel));
        String text = "sometext";
        textModel.setObject(text);
        wicketTester.startPage(testPage);
        assertTrue(wicketTester.getLastResponseAsString().contains(Strings.escapeMarkup(text)));
    }

    @Test
    public void nullIsNotValidated() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, textModel));
        testPage.getFieldPanel().getField().setRequired(false);
        testPage.getFieldPanel().getField().add(StringValidator.minimumLength(2));
        wicketTester.startPage(testPage);
        FormTester formTester = wicketTester.newFormTester(testPage.getForm().getId());
        formTester.setValue("field:textField", "");
        formTester.submit();
        assertEquals(null, testPage.getFieldPanel().getDefaultModelObject());
        assertTrue(testPage.getFieldPanel().getField().isValid());
    }

    @Test
    public void requiredAttribute() {
        TestPage<String, AjaxTextFieldPanel> testPage =
                new TestPage.Builder<String, AjaxTextFieldPanel>().build(
                        new AjaxTextFieldPanel(TestPage.FIELD, TestPage.FIELD, textModel));
        testPage.getFieldPanel().setOutputMarkupId(true);
        testPage.getFieldPanel().getField().setRequired(true);
        wicketTester.startPage(testPage);
        wicketTester.assertLabel("form:field:field-label", "field");
        wicketTester.assertVisible("form:field:required");
        wicketTester.assertVisible("form:field:externalAction");
    }
}
