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
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.ResourceModel;

public abstract class IndicatingOnConfirmAjaxLink<T> extends IndicatingAjaxLink<T> {

    private static final long serialVersionUID = 2228670850922265663L;

    private final String msg;

    private final boolean enabled;

    public IndicatingOnConfirmAjaxLink(final String id, final boolean enabled) {
        this(id, "confirmDelete", enabled);
    }

    public IndicatingOnConfirmAjaxLink(final String id, final String msg, final boolean enabled) {
        super(id);
        this.msg = msg;
        this.enabled = enabled;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        if (enabled) {
            response.render(JavaScriptHeaderItem.forScript("proceed = false;", null));
            response.render($(this).on("click",
                    new JavaScriptInlineFunction(""
                            + "var element = $(this);"
                            + "evt.preventDefault();"
                            + "if (proceed == false) {"
                            + "  evt.stopImmediatePropagation();"
                            + "  bootbox.confirm('" + new ResourceModel(msg).getObject() + "', function(result) {"
                            + "    if (result == true) {"
                            + "      proceed = true;"
                            + "      element.click();"
                            + "    } else {"
                            + "      proceed = false;"
                            + "    }"
                            + "  return true;"
                            + "  })"
                            + "} else {"
                            + "  proceed = false;"
                            + "};"
                    )).asDomReadyScript());
        }
    }
}
