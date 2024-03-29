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
package org.apache.syncope.client.enduser.pages;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class BaseNoSidebarPage extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    public BaseNoSidebarPage(final PageParameters parameters, final String selfRegistration) {
        super(parameters, selfRegistration);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript(
                "Wicket.Event.add(window, \"load\", function(event) {$(document.getElementsByClassName"
                        + "(\"sidebar-overlay\")).hide();})"));
    }

}
