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
package org.apache.syncope.console.markup.html.list;

import java.util.List;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

public abstract class AltListView<T> extends ListView<T> {

    private static final long serialVersionUID = 251378224847354710L;

    public AltListView(final String id) {
        super(id);
    }

    public AltListView(final String id, final IModel<? extends List<? extends T>> model) {
        super(id, model);
    }

    public AltListView(final String id, final List<? extends T> list) {
        super(id, list);
    }

    @Override
    protected ListItem<T> newItem(final int index, final IModel<T> itemModel) {
        return new ListItem<T>(index, itemModel) {

            private static final long serialVersionUID = 5473483270932376694L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                if (index % 2 == 0) {
                    tag.append("class", "alt", " ");
                }

                super.onComponentTag(tag);
            }
        };
    }
}
