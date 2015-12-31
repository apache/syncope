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

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;

/**
 * Modal window with Display user attributes form.
 *
 * @param <T> anyTO
 */
public class AnyDisplayAttributesModalPage<T extends Serializable> extends DisplayAttributesModalPage<T> {

    private static final long serialVersionUID = 5194630813773543054L;

    public static final String[] ANY_DEFAULT_SELECTION = { "key" };

    private final String type;

    public AnyDisplayAttributesModalPage(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> schemaNames,
            final List<String> dSchemaNames,
            final String type) {

        super(modal, pageRef, schemaNames, dSchemaNames);
        this.type = type;
    }

    @Override
    public String getPrefDetailView() {
        return String.format(Constants.PREF_ANY_DETAILS_VIEW, type);
    }

    @Override
    public String getPrefAttributeView() {
        return String.format(Constants.PREF_ANY_ATTRIBUTES_VIEW, type);
    }

    @Override
    public String getPrefDerivedAttributeView() {
        return String.format(Constants.PREF_ANY_DERIVED_ATTRIBUTES_VIEW, type);
    }

    @Override
    public Class<? extends AnyTO> getTOClass() {
        return AnyObjectTO.class;
    }
}
