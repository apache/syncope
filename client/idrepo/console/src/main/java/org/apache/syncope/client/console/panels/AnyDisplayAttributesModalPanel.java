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

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;

/**
 * Modal window with Display attributes form.
 *
 * @param <T> can be {@link AnyTO} or {@link org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper}
 */
public abstract class AnyDisplayAttributesModalPanel<T extends Serializable> extends AbstractDisplayModalPanel<T> {

    private static final long serialVersionUID = -4274117450918385110L;

    protected final String type;

    protected AnyDisplayAttributesModalPanel(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> pSchemaNames,
            final List<String> dSchemaNames,
            final String type) {

        super(
                modal,
                pageRef,
                SearchableFields.get(getTOClass(type)).keySet().stream().toList(),
                pSchemaNames,
                dSchemaNames,
                getPrefDetailView(type),
                getPrefPlainAttributeView(type),
                getPrefDerivedAttributeView(type));
        this.type = type;
    }

    public static String getPrefDetailView(final String type) {
        return String.format(Constants.PREF_ANY_DETAILS_VIEW, type);
    }

    public static String getPrefPlainAttributeView(final String type) {
        return String.format(Constants.PREF_ANY_PLAIN_ATTRS_VIEW, type);
    }

    public static String getPrefDerivedAttributeView(final String type) {
        return String.format(Constants.PREF_ANY_DER_ATTRS_VIEW, type);
    }

    public static Class<? extends AnyTO> getTOClass(final String type) {
        if (type.equalsIgnoreCase(AnyTypeKind.USER.name())) {
            return UserTO.class;
        }
        if (type.equalsIgnoreCase(AnyTypeKind.GROUP.name())) {
            return GroupTO.class;
        }
        return AnyObjectTO.class;
    }
}
