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
package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.wicket.PageReference;

public abstract class TypesDirectoryPanel<
        T extends EntityTO, DP extends DirectoryDataProvider<T>, E extends RestClient>
        extends DirectoryPanel<T, T, DP, E> {

    private static final long serialVersionUID = 7890071604330629259L;

    public TypesDirectoryPanel(
            final String id,
            final E restClient,
            final boolean withWizard,
            final PageReference pageRef) {

        super(id, restClient, pageRef);

        if (!withWizard) {
            setFooterVisibility(true);
            modal.addSubmitButton();
        }
        modal.size(Modal.Size.Large);
    }
}
