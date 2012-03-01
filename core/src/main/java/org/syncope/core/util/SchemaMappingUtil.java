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
package org.syncope.core.util;

import java.util.ArrayList;
import java.util.List;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.SyncopeUser;

public class SchemaMappingUtil {

    public static String getExtAttrName(final SchemaMapping mapping) {
        final String name;

        if (mapping.isAccountid()) {
            name = Uid.NAME;
        } else if (mapping.isPassword()) {
            name = OperationalAttributes.PASSWORD_NAME;
        } else {
            name = mapping.getExtAttrName();
        }

        return name;
    }

    public static List<String> getIntValueAsStrings(
            final AbstractAttributable attributable, final SchemaMapping mapping) {
        final List<String> value;

        switch (mapping.getIntMappingType()) {
            case Username:
                value = new ArrayList<String>();
                value.add(((SyncopeUser) attributable).getUsername());
                break;
            case Password:
                value = new ArrayList<String>();
                value.add(((SyncopeUser) attributable).getPassword());
                break;
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                value = attributable.getAttribute(mapping.getIntAttrName()).getValuesAsStrings();
                break;
            case UserVirtualSchema:
            case RoleVirtualSchema:
            case MembershipVirtualSchema:
                value = attributable.getVirtualAttribute(mapping.getIntAttrName()).getValues();
                break;
            case UserDerivedSchema:
            case RoleDerivedSchema:
            case MembershipDerivedSchema:
                value = new ArrayList<String>();
                value.add(attributable.getDerivedAttribute(mapping.getIntAttrName()).getValue(
                        attributable.getAttributes()));
                break;
            default:
                value = null;
        }

        return value;
    }
}
