/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.panels.any;

import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractResources;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;

public class Resources extends AbstractResources {

    private static final long serialVersionUID = 702900610508752856L;

    public <T extends AnyTO> Resources(final AnyWrapper<T> modelObject) {
        super(modelObject);
    }

    @Override
    public boolean evaluate() {
        available.setObject(SyncopeEnduserSession.get().getService(SyncopeService.class).platform().getResources());
        return !available.getObject().isEmpty();
    }
}
