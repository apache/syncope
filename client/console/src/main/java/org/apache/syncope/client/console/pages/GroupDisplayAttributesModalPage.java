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

import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

/**
 * Modal window with Display group attributes form.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GroupDisplayAttributesModalPage extends DisplayAttributesModalPage {

    private static final long serialVersionUID = 5194630813773543054L;

    public static final String[] GROUP_DEFAULT_SELECTION = { "key", "name" };

    public GroupDisplayAttributesModalPage(final PageReference pageRef, final ModalWindow window,
            final List<String> schemaNames, final List<String> dSchemaNames) {
        super(pageRef, window, schemaNames, dSchemaNames);
    }

    @Override
    public String getPrefDetailView() {
        return Constants.PREF_GROUP_DETAILS_VIEW;
    }

    @Override
    public String getPrefAttributeView() {
        return Constants.PREF_GROUP_ATTRIBUTES_VIEW;
    }

    @Override
    public String getPrefDerivedAttributeView() {
        return Constants.PREF_GROUP_DERIVED_ATTRIBUTES_VIEW;
    }

    @Override
    public Class getTOClass() {
        return GroupTO.class;
    }
}
