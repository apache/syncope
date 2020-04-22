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
import java.util.List;
import org.apache.syncope.client.console.panels.AnyDirectoryPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.WebMarkupContainer;

public interface AnyDirectoryPanelAdditionalActionsProvider extends Serializable {

    void add(AnyDirectoryPanel<?, ?> panel,
            BaseModal<?> modal,
            boolean wizardInModal,
            WebMarkupContainer container,
            String type,
            String realm,
            String fiql,
            int rows,
            List<String> pSchemaNames,
            List<String> dSchemaNames,
            PageReference pageRef);

    void hide();
}
