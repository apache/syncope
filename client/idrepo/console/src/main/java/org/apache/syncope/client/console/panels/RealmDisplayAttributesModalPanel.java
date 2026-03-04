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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;

public class RealmDisplayAttributesModalPanel<T extends Serializable> extends AbstractDisplayModalPanel<T> {

    protected static final Set<String> EXCLUDED_COLUMNS = Set.of(
            "parent",
            "plainAttrs",
            "derAttrs",
            "templates",
            "accountPolicy",
            "passwordPolicy",
            "authPolicy",
            "accessPolicy",
            "attrReleasePolicy",
            "ticketExpirationPolicy");

    protected static final List<String> DEFAULT_COLUMNS = List.of(
            Constants.KEY_FIELD_NAME,
            Constants.NAME_FIELD_NAME,
            "fullPath",
            "anyTypeClasses");

    protected static final List<String> AVAILABLE_COLUMNS = Arrays.stream(RealmTO.class.getDeclaredFields()).
            filter(field -> !Modifier.isStatic(field.getModifiers())).
            map(Field::getName).
            filter(fieldName -> !EXCLUDED_COLUMNS.contains(fieldName)).
            toList();

    private static final long serialVersionUID = 4055453846383559861L;

    public RealmDisplayAttributesModalPanel(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final List<String> availableDetails,
            final List<String> availablePlainSchemas,
            final List<String> availableDerSchemas) {

        super(
                modal,
                pageRef,
                availableDetails,
                availablePlainSchemas,
                availableDerSchemas,
                IdRepoConstants.PREF_REALM_DETAILS_VIEW,
                IdRepoConstants.PREF_REALM_PLAIN_ATTRS_VIEW,
                IdRepoConstants.PREF_REALM_DER_ATTRS_VIEW);
    }
}
