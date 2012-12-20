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
package org.apache.syncope.console.commons;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class SortableAttributableProviderComparator extends SortableDataProviderComparator<AbstractAttributableTO> {

    private static final long serialVersionUID = 1775967163571699258L;

    private static final Set<String> inlineProps;

    static {
        inlineProps = new HashSet<String>();
        inlineProps.add("id");
        inlineProps.add("status");
        inlineProps.add("token");
    }

    public SortableAttributableProviderComparator(final SortableDataProvider<AbstractAttributableTO, String> provider) {
        super(provider);
    }

    @Override
    public int compare(final AbstractAttributableTO o1, final AbstractAttributableTO o2) {
        if (inlineProps.contains(provider.getSort().getProperty())) {
            return super.compare(o1, o2);
        }

        return super.compare(new AttrModel(o1.getAttributeMap()), new AttrModel(o2.getAttributeMap()));
    }

    private class AttrModel extends AbstractReadOnlyModel<Comparable> {

        private static final long serialVersionUID = 7201800923472498270L;

        private final Map<String, AttributeTO> attrMap;

        public AttrModel(final Map<String, AttributeTO> attrMap) {
            super();

            this.attrMap = attrMap;
        }

        @Override
        public Comparable getObject() {
            Comparable result = null;

            List<String> values = attrMap.get(provider.getSort().getProperty()).getValues();
            if (values != null && !values.isEmpty()) {
                result = values.iterator().next();
            }

            return result;
        }
    }
}
