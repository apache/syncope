/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.util.EntitlementUtil;

@Controller
@RequestMapping("/auth")
public class AuthenticationController extends AbstractController {

    @Autowired
    private EntitlementDAO entitlementDAO;

    @RequestMapping(method = RequestMethod.GET,
    value = "/allentitlements")
    public List<String> listEntitlements() {
        List<Entitlement> entitlements = entitlementDAO.findAll();
        List<String> result = new ArrayList<String>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            result.add(entitlement.getName());
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/entitlements")
    public Set<String> getEntitlements() {
        return EntitlementUtil.getOwnedEntitlementNames();
    }
}
