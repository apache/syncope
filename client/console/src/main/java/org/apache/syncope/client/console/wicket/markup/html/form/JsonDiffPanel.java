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
package org.apache.syncope.client.console.wicket.markup.html.form;

import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

public class JsonDiffPanel extends AbstractModalPanel<String> {

    private static final long serialVersionUID = -5110368813584745668L;

    private final IModel<String> first;

    private final IModel<String> second;

    public JsonDiffPanel(
            final BaseModal<String> modal,
            final IModel<String> first,
            final IModel<String> second,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.second = second;
        this.first = first;
        TextArea<String> jsonEditorInfoDefArea1 = new TextArea<>("jsonEditorInfo1", this.first);
        TextArea<String> jsonEditorInfoDefArea2 = new TextArea<>("jsonEditorInfo2", this.second);
        jsonEditorInfoDefArea1.setMarkupId("jsonEditorInfo1").setOutputMarkupPlaceholderTag(true);
        jsonEditorInfoDefArea2.setMarkupId("jsonEditorInfo2").setOutputMarkupPlaceholderTag(true);
        add(jsonEditorInfoDefArea1);
        add(jsonEditorInfoDefArea2);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnLoadHeaderItem.forScript(
                "CodeMirror.MergeView(document.getElementById('jsonDiffEditorInfoDefForm'), {"
                + "  value: document.getElementById('jsonEditorInfo1').innerHTML, "
                + "  orig: document.getElementById('jsonEditorInfo2').innerHTML, "
                + "  lineNumbers: true, "
                + "  mode: \"application/json\","
                + "  highlightDifferences: true,"
                + "  showDifferences: true,"
                + "  readOnly: true,"
                + "  revertButtons: false"
                + "});"));
    }
}
