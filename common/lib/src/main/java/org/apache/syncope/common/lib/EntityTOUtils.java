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
package org.apache.syncope.common.lib;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;

public final class EntityTOUtils {

    public static Map<String, AttrTO> buildAttrMap(final Collection<AttrTO> attrs) {
        return Collections.unmodifiableMap(attrs.stream().collect(Collectors.toMap(
                AttrTO::getSchema, Function.identity(), (exist, repl) -> repl)));
    }

    public static Map<Pair<String, String>, RelationshipTO> buildRelationshipMap(
            final Collection<RelationshipTO> relationships) {

        return Collections.unmodifiableMap(relationships.stream().collect(Collectors.toMap(
                rel -> Pair.of(rel.getType(), rel.getOtherEndKey()), Function.identity(), (exist, repl) -> repl)));
    }

    public static Map<String, MembershipTO> buildMembershipMap(final Collection<MembershipTO> memberships) {
        return Collections.unmodifiableMap(memberships.stream().collect(Collectors.toMap(
                MembershipTO::getGroupKey, Function.identity(), (exist, repl) -> repl)));
    }

    public static Map<Pair<String, String>, LinkedAccountTO> buildLinkedAccountMap(
            final Collection<LinkedAccountTO> accounts) {

        return Collections.unmodifiableMap(accounts.stream().collect(Collectors.toMap(
                account -> Pair.of(account.getResource(), account.getconnObjectKeyValue()),
                Function.identity(),
                (exist, repl) -> repl)));
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private EntityTOUtils() {
    }
}
