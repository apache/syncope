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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.services.AuthenticationService;
import org.apache.syncope.to.EntitlementTO;
import org.apache.syncope.types.AuditElements.AuthenticationSubCategory;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationController extends AbstractController implements AuthenticationService {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Override
    public List<EntitlementTO> getAllEntitlements() {
        List<Entitlement> entitlements = entitlementDAO.findAll();
        List<EntitlementTO> result = new ArrayList<EntitlementTO>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            result.add(new EntitlementTO(entitlement.getName()));
        }

        return result;
    }

    @Override
    public Set<EntitlementTO> getMyEntitlements() {
        Set<String> ownedEntitlements = EntitlementUtil.getOwnedEntitlementNames();
        Set<EntitlementTO> result = new HashSet<EntitlementTO>();

        for (String e : ownedEntitlements) {
            result.add(new EntitlementTO(e));
        }

        auditManager.audit(Category.authentication, AuthenticationSubCategory.getEntitlements,
                Result.success, "Owned entitlements: " + result.toString());

        return result;
    }
}
