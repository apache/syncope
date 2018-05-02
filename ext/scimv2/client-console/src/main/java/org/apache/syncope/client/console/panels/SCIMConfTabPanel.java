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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.html.panel.Panel;

public class SCIMConfTabPanel extends Panel implements ModalPanel {

    private static final long serialVersionUID = -4482885585790492795L;

    protected final List<String> plainSchemaNames = getPlainSchemas();

    public SCIMConfTabPanel(
            final String id,
            final SCIMConf scimConf) {
        super(id);
    }

    private static List<String> getPlainSchemas() {
        final List<String> names = new ArrayList<>(ClassPathScanImplementationLookup.USER_FIELD_NAMES);
        names.addAll(CollectionUtils.collect(new SchemaRestClient().getSchemas(SchemaType.PLAIN, AnyTypeKind.USER),
                new Transformer<SchemaTO, String>() {

            @Override
            public String transform(final SchemaTO input) {
                return input.getKey();
            }
        }, new ArrayList<String>()));
        names.remove("password");
        Collections.sort(names);

        return names;
    }

}
