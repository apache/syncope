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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.html.panel.Panel;

public class SCIMConfTabPanel extends Panel implements ModalPanel {

    private static final long serialVersionUID = -4482885585790492795L;

    protected final List<String> plainSchemaNames = getPlainSchemas();

    public SCIMConfTabPanel(final String id) {
        super(id);
    }

    private static List<String> getPlainSchemas() {
        return SchemaRestClient.getSchemas(SchemaType.PLAIN, AnyTypeKind.USER).stream().
                map(SchemaTO::getKey).
                filter(name -> !"password".equals(name)).
                sorted().collect(Collectors.toList());
    }
}
