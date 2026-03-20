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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class AjaxPalettePanelITCase extends AbstractConsoleITCase {

    @Test
    public void isRendered() {
        TestPage<String, AjaxPalettePanel<String>> testPage =
                new TestPage.Builder<String, AjaxPalettePanel<String>>().build(
                        new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build(
                                TestPage.FIELD,
                                new ListModel<>(List.of("A", "D")),
                                new ListModel<>(List.of("A", "B", "C", "D"))));
        TESTER.startPage(testPage);

        FormTester formTester = TESTER.newFormTester(testPage.getForm().getId());
        formTester.submit();

        @SuppressWarnings("unchecked")
        Palette<String> palette = (Palette<String>) ReflectionTestUtils.getField(testPage.getFieldPanel(), "palette");
        Collection<String> list = palette.getModelCollection();
        assertEquals(2, list.size());
        Iterator<String> iterator = list.iterator();
        assertEquals("A", iterator.next());
        assertEquals("D", iterator.next());
    }
}
