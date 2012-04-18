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
package org.apache.syncope.client.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.types.ConnConfProperty;

/**
 * Utility class for manipulating connector properties.
 *
 */
public final class ConnConfPropUtils {

    private ConnConfPropUtils() {
    }

    public static Set<ConnConfProperty> joinConnInstanceProperties(
            final Map<String, ConnConfProperty> connectorProp, final Map<String, ConnConfProperty> resourceProp) {

        Set<ConnConfProperty> result = new HashSet<ConnConfProperty>();
        result.addAll(connectorProp.values());
        result.addAll(resourceProp.values());

        return result;
    }

    public static Map<String, ConnConfProperty> getConnConfPropertyMap(final Set<ConnConfProperty> properties) {
        Map<String, ConnConfProperty> result;
        if (properties == null) {
            result = Collections.EMPTY_MAP;
        } else {
            result = new HashMap<String, ConnConfProperty>();
            for (Iterator<ConnConfProperty> item = properties.iterator(); item.hasNext();) {
                ConnConfProperty property = item.next();
                result.put(property.getSchema().getName(), property);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }
}
