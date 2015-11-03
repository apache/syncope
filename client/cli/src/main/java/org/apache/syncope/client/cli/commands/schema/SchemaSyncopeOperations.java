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
package org.apache.syncope.client.cli.commands.schema;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.SchemaService;

public class SchemaSyncopeOperations {

    private final SchemaService schemaService = SyncopeServices.get(SchemaService.class);

    public <T extends AbstractSchemaTO> T read(final String schemaTypeString, final String schemaName) {
        return schemaService.read(SchemaType.valueOf(schemaTypeString), schemaName);
    }

    public <T extends AbstractSchemaTO> List<T> list(final String schemaTypeString) {
        return schemaService.list(SchemaType.valueOf(schemaTypeString), null);
    }

    public <T extends AbstractSchemaTO> List<T> listVirtual() {
        return schemaService.list(SchemaType.VIRTUAL, null);
    }

    public <T extends AbstractSchemaTO> List<T> listPlain() {
        return schemaService.list(SchemaType.PLAIN, null);
    }

    public <T extends AbstractSchemaTO> List<T> listDerived() {
        return schemaService.list(SchemaType.DERIVED, null);
    }

    public void delete(final String schemaTypeString, final String schemaName) {
        schemaService.delete(SchemaType.valueOf(schemaTypeString), schemaName);
    }
}
