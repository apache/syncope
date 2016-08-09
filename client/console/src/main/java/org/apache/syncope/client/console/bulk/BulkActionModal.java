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
package org.apache.syncope.client.console.bulk;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.RestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

public class BulkActionModal<T extends Serializable, S> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = 4114026480146090962L;

    public BulkActionModal(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final Collection<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final RestClient bulkActionExecutor,
            final String keyFieldName) {

        super(modal, pageRef);
        add(new BulkContent<>("content", modal, items, columns, actions, bulkActionExecutor, keyFieldName).
                setRenderBodyOnly(true));
    }
}
