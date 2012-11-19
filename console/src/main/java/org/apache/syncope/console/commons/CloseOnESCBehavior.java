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
package org.apache.syncope.console.commons;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

public class CloseOnESCBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 5826308247642534260L;

    private ModalWindow modalWindow;

    public CloseOnESCBehavior(ModalWindow modalWindow) {
        this.modalWindow = modalWindow;
    }

    private static final String PRE_JS = "$(document).ready(function() {\n"
            + "$(document).bind('keyup', function(evt) {\n"
            + "    if (evt.keyCode == 27){\n";

    private static final String POST_JS = "\n evt.preventDefault();\n"
            + "evt.stopPropagation();\n"
            + "    }\n"
            + "  });\n"
            + "});";

    @Override
    protected void respond(final AjaxRequestTarget target) {
        modalWindow.close(target);
    }

    @Override
    protected String findIndicatorId() {
        return null;
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forScript(
                new StringBuilder(PRE_JS).append(getCallbackScript()).append(POST_JS).toString(),
                "closeModalOnEsc"));
    }

    public void setModalWindow(ModalWindow modalWindow) {
        this.modalWindow = modalWindow;
    }
}
