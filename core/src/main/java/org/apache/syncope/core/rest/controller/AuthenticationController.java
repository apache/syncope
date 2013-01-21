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
package org.apache.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.types.AuditElements.AuthenticationSubCategory;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/auth")
public class AuthenticationController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @RequestMapping(method = RequestMethod.GET, value = "/allentitlements")
    public List<String> listEntitlements() {
        List<Entitlement> entitlements = entitlementDAO.findAll();
        List<String> result = new ArrayList<String>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            result.add(entitlement.getName());
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/entitlements")
    public Set<String> getEntitlements() {
        Set<String> result = EntitlementUtil.getOwnedEntitlementNames();

        auditManager.audit(Category.authentication, AuthenticationSubCategory.getEntitlements, Result.success,
                "Owned entitlements: " + result.toString());

        return result;
    }
}
