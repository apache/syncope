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

import java.util.Collection;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.model.IModel;

public class ActionTableCheckGroup<T> extends CheckGroup<T> {

    private static final long serialVersionUID = 1288270558573401394L;

    public ActionTableCheckGroup(final String id, final Collection<T> collection) {
        super(id, collection);
    }

    public ActionTableCheckGroup(final String id, final IModel<Collection<T>> model) {
        super(id, model);
    }

    public boolean isCheckable(final T element) {
        return true;
    }
}
