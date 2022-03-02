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
package org.apache.syncope.core.provisioning.api.jexl;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.LogFactory;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;

class SandboxUberspect extends Uberspect {

    private static final Set<String> COLLECTION_METHODS =
            Set.of("contains", "containsAll", "isEmpty", "size", "iterator", "toString");

    private static final Set<String> LIST_METHODS =
            Set.of("get", "indexOf", "lastIndexOf", "toString");

    private static final Set<String> MAP_METHODS =
            Set.of("get", "getOrDefault", "containsKey", "containsValue", "toString");

    SandboxUberspect() {
        super(LogFactory.getLog(JexlEngine.class), JexlUberspect.JEXL_STRATEGY);
    }

    @Override
    public JexlMethod getConstructor(final Object ctorHandle, final Object... args) {
        return null;
    }

    @Override
    public JexlMethod getMethod(final Object obj, final String method, final Object... args) {
        if (obj instanceof AnyTO || obj instanceof Any
                || obj instanceof PlainAttr || obj instanceof Attr
                || obj instanceof MembershipTO || obj instanceof Membership
                || obj instanceof Realm || obj instanceof RealmTO) {

            return super.getMethod(obj, method, args);
        } else if (obj instanceof SyncopeJexlFunctions) {
            return super.getMethod(obj, method, args);
        } else if (obj instanceof Optional) {
            return super.getMethod(obj, method, args);
        } else if (obj.getClass().isArray()) {
            return super.getMethod(obj, method, args);
        } else if (obj instanceof String) {
            return super.getMethod(obj, method, args);
        } else if (obj instanceof Date || obj instanceof Instant || obj instanceof TemporalAccessor) {
            return super.getMethod(obj, method, args);
        } else if (obj instanceof Map && MAP_METHODS.contains(method)) {
            return super.getMethod(obj, method, args);
        } else if (obj instanceof List && (LIST_METHODS.contains(method) || COLLECTION_METHODS.contains(method))) {
            return super.getMethod(obj, method, args);
        } else if (obj instanceof Collection && COLLECTION_METHODS.contains(method)) {
            return super.getMethod(obj, method, args);
        }
        return null;
    }

    @Override
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, final Object arg) {
        return null;
    }

    @Override
    public JexlPropertySet getPropertySet(
            final List<PropertyResolver> resolvers, final Object obj, final Object identifier, final Object arg) {

        return null;
    }
}
