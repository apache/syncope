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

import java.io.Serializable;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;

public abstract class DirectoryDataProvider<T extends Serializable> extends SortableDataProvider<T, String> {

    private static final long serialVersionUID = 6267494272884913376L;

    protected int paginatorRows;

    public DirectoryDataProvider(final int paginatorRows) {
        super();
        this.paginatorRows = paginatorRows;

        // default sorting
        setSort(Constants.KEY_FIELD_NAME, SortOrder.ASCENDING);
    }

    public void setPaginatorRows(final int paginatorRows) {
        this.paginatorRows = paginatorRows;
    }
}
