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

import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.client.AbstractBaseBean;

/**
 * Modal window with Schema form.
 */
abstract public class AbstractSchemaModalPage extends BaseModalPage {

    private static final long serialVersionUID = 7369215690388444748L;

    @SpringBean
    protected SchemaRestClient restClient;

    protected String kind;

    public AbstractSchemaModalPage(String kind) {
        this.kind = kind;
    }

    abstract public void setSchemaModalPage(
            final PageReference callerPageRef,
            final ModalWindow window,
            AbstractBaseBean schema,
            final boolean createFlag);

    public String getKind() {
        return kind;
    }
}
