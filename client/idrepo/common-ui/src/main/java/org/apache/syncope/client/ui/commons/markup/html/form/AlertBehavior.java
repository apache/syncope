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
package org.apache.syncope.client.ui.commons.markup.html.form;

import static de.agilecoders.wicket.jquery.JQuery.$;

import de.agilecoders.wicket.jquery.function.JavaScriptInlineFunction;
import java.util.ArrayList;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;

public class AlertBehavior extends Behavior {

    private static final long serialVersionUID = 2210125898183667592L;

    private final Component parent;

    private final String title;

    private final String body;

    public AlertBehavior(final Component parent, final String title, final String body) {
        this.parent = parent;
        this.title = title;
        this.body = body;
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        response.render($(parent).on("click",
                new JavaScriptInlineFunction(""
                        + "bootbox.alert({"
                        + "size:'large', "
                        + "title:'" + title + "', "
                        + "message: '" + body + "', "
                        + "buttons: {"
                        + "    ok: {"
                        + "        className: 'btn-success'"
                        + "    }"
                        + "},"
                        + "locale: '" + Session.get().getLocale().getLanguage() + "',"
                        + "callback: function() {"
                        + "  return true;"
                        + "}"
                        + "});", new ArrayList<>()
                )).asDomReadyScript());
    }
}
