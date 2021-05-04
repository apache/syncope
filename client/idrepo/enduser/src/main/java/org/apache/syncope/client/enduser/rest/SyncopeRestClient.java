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
package org.apache.syncope.client.enduser.rest;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;

public class SyncopeRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2211371717449597247L;

    public static List<String> listAnyTypeClasses() {
        List<String> types = Collections.emptyList();

        try {
            types = getService(SyncopeService.class).platform().getAnyTypeClasses();
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any type classes", e);
        }
        return types;
    }

    public static List<String> searchUserTypeExtensions(final String groupName) {
        List<String> types = Collections.emptyList();
        try {
            TypeExtensionTO typeExtensionTO = getService(SyncopeService.class).readUserTypeExtension(groupName);
            types = typeExtensionTO == null ? types : typeExtensionTO.getAuxClasses();
        } catch (Exception e) {
            LOG.error("While reading all any type classes for group [{}]", groupName, e);
        }
        return types;
    }

}
