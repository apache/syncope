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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.ui.commons.status.ConnObjectWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;

public interface StatusProvider extends Serializable {

    record Info(ConnObject onSyncope, ConnObject onResource) implements Serializable {

    }

    record InfoWithFailure(ConnObject onSyncope, ConnObjectWrapper onResource, String failure) implements Serializable {

    }

    Optional<Info> get(String anyTypeKey, String connObjectKeyValue, String resource);

    List<InfoWithFailure> get(AnyTO any, Collection<String> resources);

    <T extends Serializable> void addConnObjectLink(
            ListViewPanel.Builder<T> builder, ActionLink<T> connObjectLink);
}
