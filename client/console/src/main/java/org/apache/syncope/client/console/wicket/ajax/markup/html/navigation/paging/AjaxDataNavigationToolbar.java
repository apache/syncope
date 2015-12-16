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
package org.apache.syncope.client.console.wicket.ajax.markup.html.navigation.paging;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;

public class AjaxDataNavigationToolbar extends AjaxNavigationToolbar {

    private static final long serialVersionUID = -225570234877133351L;

    private final WebMarkupContainer container;

    public AjaxDataNavigationToolbar(final DataTable<?, ?> table, final WebMarkupContainer container) {
        super(table);
        this.container = container;
    }

    @Override
    protected PagingNavigator newPagingNavigator(final String navigatorId, final DataTable<?, ?> table) {
        return new BootstrapAjaxPagingNavigator(navigatorId, table) {

            private static final long serialVersionUID = -5254490177324296529L;

            @Override
            protected void onAjaxEvent(final AjaxRequestTarget target) {
                if (container != null) {
                    target.add(container);
                }
            }
        };
    }
}
