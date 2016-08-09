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
package org.apache.syncope.client.console.commons;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class SortableAnyProviderComparator<T extends AnyTO> extends SortableDataProviderComparator<T> {

    private static final long serialVersionUID = 1775967163571699258L;

    private static final Set<String> INLINE_PROPS = new HashSet<>(Arrays.asList(
            new String[] { "key", "status", "token", "username", "name" }));

    public SortableAnyProviderComparator(final SortableDataProvider<T, String> provider) {
        super(provider);
    }

    @Override
    public int compare(final T any1, final T any2) {
        if (INLINE_PROPS.contains(provider.getSort().getProperty())) {
            return super.compare(any1, any2);
        }

        return super.compare(new AttrModel(any1), new AttrModel(any2));
    }

    @SuppressWarnings("rawtypes")
    private class AttrModel extends AbstractReadOnlyModel<Comparable> {

        private static final long serialVersionUID = -7856686374020091808L;

        private final Map<String, AttrTO> plainAttrs;

        private final Map<String, AttrTO> derAttrs;

        private final Map<String, AttrTO> virAttrs;

        AttrModel(final AnyTO anyTO) {
            super();

            this.plainAttrs = anyTO.getPlainAttrMap();
            this.derAttrs = anyTO.getDerAttrMap();
            this.virAttrs = anyTO.getVirAttrMap();
        }

        /**
         * @see UserAttrColumn constructor
         */
        @Override
        public Comparable getObject() {
            int hashPos = provider.getSort().getProperty().indexOf('#');

            SchemaType schemaType = null;
            final String schema;
            if (hashPos == -1) {
                schema = provider.getSort().getProperty();
            } else {
                String[] splitted = provider.getSort().getProperty().split("#");
                try {
                    schemaType = SchemaType.valueOf(splitted[0]);
                } catch (IllegalArgumentException e) {
                    // this should never happen
                }
                schema = provider.getSort().getProperty().substring(hashPos + 1);
            }

            final AttrTO attr;
            if (schemaType == null) {
                attr = this.plainAttrs.get(schema);
            } else {
                switch (schemaType) {
                    case PLAIN:
                    default:
                        attr = this.plainAttrs.get(schema);
                        break;

                    case DERIVED:
                        attr = this.derAttrs.get(schema);
                        break;

                    case VIRTUAL:
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
