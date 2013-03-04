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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.UserAttrColumn;

public class SortableUserProviderComparator extends SortableDataProviderComparator<UserTO> {

    private static final Set<String> INLINE_PROPS = new HashSet<String>(Arrays.asList(
            new String[]{"id", "status", "token", "username"}));

    public SortableUserProviderComparator(final SortableDataProvider<UserTO> provider) {
        super(provider);
    }

    @Override
    public int compare(final UserTO user1, final UserTO user2) {
        if (INLINE_PROPS.contains(provider.getSort().getProperty())) {
            return super.compare(user1, user2);
        }

        return super.compare(new AttrModel(user1), new AttrModel(user2));
    }

    @SuppressWarnings("rawtypes")
    private class AttrModel extends AbstractReadOnlyModel<Comparable> {

        private static final long serialVersionUID = -7856686374020091808L;

        private final Map<String, AttributeTO> attrs;

        private final Map<String, AttributeTO> derAttrs;

        private final Map<String, AttributeTO> virAttrs;

        public AttrModel(final UserTO userTO) {
            super();

            this.attrs = userTO.getAttributeMap();
            this.derAttrs = userTO.getDerivedAttributeMap();
            this.virAttrs = userTO.getVirtualAttributeMap();
        }

        /**
         * @see UserAttrColumn constructor
         */
        @Override
        public Comparable getObject() {
            int hashPos = provider.getSort().getProperty().indexOf('#');

            UserAttrColumn.SchemaType schemaType = null;
            final String schema;
            if (hashPos == -1) {
                schema = provider.getSort().getProperty();
            } else {
                String[] splitted = provider.getSort().getProperty().split("#");
                try {
                    schemaType = UserAttrColumn.SchemaType.valueOf(splitted[0]);
                } catch (IllegalArgumentException e) {
                    // this should never happen
                }
                schema = provider.getSort().getProperty().substring(hashPos + 1);
            }


            final AttributeTO attr;
            if (schemaType == null) {
                attr = this.attrs.get(schema);
            } else {
                switch (schemaType) {
                    case schema:
                    default:
                        attr = this.attrs.get(schema);
                        break;

                    case derivedSchema:
                        attr = this.derAttrs.get(schema);
                        break;

                    case virtualSchema:
                        attr = this.virAttrs.get(schema);
                        break;
                }
            }

            Comparable result = null;

            List<String> values = attr == null ? null : attr.getValues();
            if (values != null && !values.isEmpty()) {
                result = values.iterator().next();
            }

            return result;
        }
    }
}
