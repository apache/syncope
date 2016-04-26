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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;

public class CamelRoutesDetailsPanel extends Panel {

    private static final long serialVersionUID = -768345003061796383L;

    public CamelRoutesDetailsPanel(final String id, final CamelRouteTO camelRoute) {
        super(id);

        Form<CamelRouteTO> routeDefForm = new Form<>("routeDefForm");

        TextArea<String> routeDefArea = new TextArea<>("route", new PropertyModel<String>(camelRoute, "content"));
        routeDefForm.add(routeDefArea);
        routeDefForm.setModel(new CompoundPropertyModel<>(camelRoute));
        add(routeDefForm);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnLoadHeaderItem.forScript(
                "CodeMirror.fromTextArea(document.getElementById('route'), {"
                + "  lineNumbers: true, "
                + "  autoCloseTags: true, "
                + "  mode: 'text/html', "
                + "  autoRefresh: true"
                + "}).on('change', updateTextArea);"));
    }

}
