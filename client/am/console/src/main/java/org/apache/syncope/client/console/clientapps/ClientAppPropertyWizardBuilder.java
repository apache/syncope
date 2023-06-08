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
package org.apache.syncope.client.console.clientapps;

import java.io.Serializable;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.wizards.AttrWizardBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;

public class ClientAppPropertyWizardBuilder extends AttrWizardBuilder {

    private static final long serialVersionUID = -91564005263775261L;

    protected final ClientAppType type;

    protected final ClientAppTO clientApp;

    protected final ClientAppRestClient clientAppRestClient;

    public ClientAppPropertyWizardBuilder(
            final ClientAppType type,
            final ClientAppTO clientApp,
            final Attr attr,
            final ClientAppRestClient clientAppRestClient,
            final PageReference pageRef) {

        super(attr, pageRef);
        this.type = type;
        this.clientApp = clientApp;
        this.clientAppRestClient = clientAppRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final Attr modelObject) {
        clientApp.getProperties().removeIf(p -> modelObject.getSchema().equals(p.getSchema()));
        clientApp.getProperties().add(modelObject);

        clientAppRestClient.update(type, clientApp);

        return null;
    }
}
