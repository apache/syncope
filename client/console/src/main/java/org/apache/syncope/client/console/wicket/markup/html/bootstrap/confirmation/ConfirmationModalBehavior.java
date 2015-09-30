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
package org.apache.syncope.client.console.wicket.markup.html.bootstrap.confirmation;

import static de.agilecoders.wicket.jquery.JQuery.$;

import de.agilecoders.wicket.jquery.function.JavaScriptInlineFunction;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.ResourceModel;

/**
 * A behavior that shows a modal with OK/Cancel buttons to confirm an action.
 *
 */
public class ConfirmationModalBehavior extends Behavior {

    private static final long serialVersionUID = 1741536820040325586L;

    private final String message;

    public ConfirmationModalBehavior() {
        this("confirmDelete");
    }

    public ConfirmationModalBehavior(final String msg) {
        message = new ResourceModel(msg, "Are you sure?").getObject();
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);

        response.render(JavaScriptHeaderItem.forScript("var confirm = false;", null));
        response.render($(component).on("click",
                new JavaScriptInlineFunction(""
                        + "var element = $(this);"
                        + "evt.preventDefault();"
                        + "if(confirm == false){"
                        + "evt.stopImmediatePropagation();"
                        + "bootbox.confirm(\"" + message + "\", function(result){"
                        + "if(result == true){"
                        + "confirm = true;"
                        + "element.click();"
                        + "}"
                        + "else{confirm = false;}"
                        + "return true;"
                        + "})} "
                        + "else {confirm = false;};"
                )).asDomReadyScript());
    }
}
