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
package org.syncope.console.commons;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;

public class SortableUserProviderComparator
        extends SortableDataProviderComparator<UserTO> {

    private static final Set<String> inlineProps;

    static {
        inlineProps = new HashSet<String>();
        inlineProps.add("id");
        inlineProps.add("status");
        inlineProps.add("token");
    }

    public SortableUserProviderComparator(
            final SortableDataProvider<UserTO> provider) {

        super(provider);
    }

    @Override
    public int compare(final UserTO o1, final UserTO o2) {
        if (inlineProps.contains(provider.getSort().getProperty())) {
            return super.compare(o1, o2);
        }

        return super.compare(new AttrModel(o1.getAttributeMap()),
                new AttrModel(o2.getAttributeMap()));
    }

    private class AttrModel extends AbstractReadOnlyModel<Comparable> {

        private final Map<String, AttributeTO> attrMap;

        public AttrModel(final Map<String, AttributeTO> attrMap) {
            super();

            this.attrMap = attrMap;
        }

        @Override
        public Comparable getObject() {
            Comparable result = null;

            List<String> values = attrMap.get(
                    provider.getSort().getProperty()).getValues();
            if (values != null && !values.isEmpty()) {
                result = values.iterator().next();
            }

            return result;
        }
    }
}
