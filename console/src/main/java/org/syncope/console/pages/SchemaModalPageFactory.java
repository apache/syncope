/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.rest.SchemaRestClient;

/**
 * Modal window with Schema form.
 */
abstract public class SchemaModalPageFactory extends BaseModalPage {

    @SpringBean
    protected SchemaRestClient restClient;

    public enum Entity {

        user, role, membership
    };

    public enum SchemaType {

        NORMAL, DERIVED, VIRTUAL
    };

    public static AbstractSchemaModalPage getSchemaModalPage(
            Entity entity, SchemaType schemaType) {

        AbstractSchemaModalPage page;

        switch (schemaType) {
            case DERIVED:
                page = new DerivedSchemaModalPage(entity.toString());
                break;
            case VIRTUAL:
                page = new VirtualSchemaModalPage(entity.toString());
                break;
            default:
                page = new SchemaModalPage(entity.toString());
                break;
        }

        return page;
    }
}
