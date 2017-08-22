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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;

public final class EntityTOUtils {

    public static Map<String, AttrTO> buildAttrMap(final Collection<AttrTO> attrs) {
        Map<String, AttrTO> result = new HashMap<>(attrs.size());
        attrs.forEach(attrTO -> result.put(attrTO.getSchema(), attrTO));

        return Collections.unmodifiableMap(result);
    }

    public static Map<Pair<String, String>, RelationshipTO> buildRelationshipMap(
            final Collection<RelationshipTO> relationships) {

        Map<Pair<String, String>, RelationshipTO> result = new HashMap<>(relationships.size());
        relationships.forEach(
                relationship -> result.put(Pair.of(relationship.getType(), relationship.getRightKey()), relationship));

        return Collections.unmodifiableMap(result);
    }

    public static Map<String, MembershipTO> buildMembershipMap(final Collection<MembershipTO> memberships) {
        Map<String, MembershipTO> result = new HashMap<>(memberships.size());
        memberships.forEach(membership -> result.put(membership.getRightKey(), membership));

        return Collections.unmodifiableMap(result);
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private EntityTOUtils() {
    }
}
