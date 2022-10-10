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

import static de.agilecoders.wicket.jquery.JQuery.$;

import de.agilecoders.wicket.jquery.function.JavaScriptInlineFunction;
import java.util.ArrayList;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.ResourceModel;

public class ConfirmBehavior extends Behavior {

    private static final long serialVersionUID = 2210125898183667592L;

    private final Component parent;

    private final String msg;

    public ConfirmBehavior(final Component parent, final String msg) {
        this.parent = parent;
        this.msg = msg;
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        response.render(JavaScriptHeaderItem.forScript("proceed = false;", null));
        response.render($(parent).on("click",
                new JavaScriptInlineFunction(""
                        + "var element = $(this);"
                        + "evt.preventDefault();"
                        + "if (proceed == false) {"
                        + "  evt.stopImmediatePropagation();"
                        + "  bootbox.confirm({"
                        + "message:'" + new ResourceModel(msg).getObject() + "', "
                        + "buttons: {"
                        + "    confirm: {"
                        + "        className: 'btn-success'"
                        + "    },"
                        + "    cancel: {"
                        + "        className: 'btn-danger'"
                        + "    }"
                        + "},"
                        + "locale: '" + SyncopeConsoleSession.get().getLocale().getLanguage() + "',"
                        + "callback: function(result) {"
                        + "    if (result == true) {"
                        + "      proceed = true;"
                        + "      element.click();"
                        + "    } else {"
                        + "      proceed = false;"
                        + "    }"
                        + "  return true;"
                        + "  }})"
                        + "} else {"
                        + "  proceed = false;"
                        + "};", new ArrayList<>()
                )).asDomReadyScript());
    }
}
