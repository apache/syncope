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
package org.apache.syncope.client.console.pages;

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

public class ModelerPopupPage extends WebPage {

    private static final long serialVersionUID = -7031206743629422898L;

    public ModelerPopupPage(final PageParameters parameters) {
        super(parameters);

        StringValue modelId = parameters.get(Constants.MODEL_ID_PARAM);

        WebMarkupContainer refresh = new WebMarkupContainer("refresh");
        // properly parameterize ?modelId=5 with SYNCOPE-1020
        refresh.add(new AttributeModifier(
                "content", "0; url=../../" + parameters.get(Constants.MODELER_CONTEXT)
                + "/index.html#/editor/" + modelId.toString()));
        add(refresh);
    }
}
