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
package org.apache.syncope.console.commons;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.console.pages.AbstractSchemaModalPage;
import org.apache.syncope.console.pages.DerivedSchemaModalPage;
import org.apache.syncope.console.pages.SchemaModalPage;
import org.apache.syncope.console.pages.VirtualSchemaModalPage;

public final class SchemaModalPageFactory {

    private static final long serialVersionUID = -3533177688264693505L;

    private SchemaModalPageFactory() {
        // empty constructor for static utility class
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractSchemaModalPage> T getSchemaModalPage(
            final AttributableType entity, final SchemaType schemaType) {

        T page;

        switch (schemaType) {
            case DERIVED:
                page = (T) new DerivedSchemaModalPage(entity);
                break;

            case VIRTUAL:
                page = (T) new VirtualSchemaModalPage(entity);
                break;

            default:
                page = (T) new SchemaModalPage(entity);
                break;
        }

        return page;
    }
}
