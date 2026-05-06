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
package org.apache.syncope.core.logic.wa;

import java.util.List;
import java.util.function.Predicate;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ConsentDecisionLogic extends AbstractAuthProfileLogic {

    public ConsentDecisionLogic(
            final AuthProfileDataBinder binder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        super(binder, authProfileDAO, entityFactory);
    }

    protected void removeAndSave(final AuthProfile profile, final Predicate<WAConsentDecision> criteria) {
        List<WAConsentDecision> consentDecisions = profile.getConsentDecisions();
        if (consentDecisions.removeIf(criteria)) {
            profile.setConsentDecisions(consentDecisions);
            authProfileDAO.save(profile);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner, final long id) {
        authProfileDAO.findByOwner(owner).
                ifPresent(profile -> removeAndSave(profile, consentDecision -> consentDecision.getId() == id));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(profile -> {
            profile.setConsentDecisions(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll(Pageable.unpaged()).forEach(profile -> {
            profile.setConsentDecisions(List.of());
            authProfileDAO.save(profile);
        });
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void store(final String owner, final WAConsentDecision contentDecision) {
        AuthProfile profile = authProfile(owner);

        List<WAConsentDecision> consentDecisions = profile.getConsentDecisions();
        consentDecisions.removeIf(cd -> cd.getId() == contentDecision.getId());
        consentDecisions.add(contentDecision);
        profile.setConsentDecisions(consentDecisions);
        authProfileDAO.save(profile);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAConsentDecision read(final String owner, final String service) {
        return authProfileDAO.findByOwner(owner).
                stream().
                map(AuthProfile::getConsentDecisions).
                flatMap(List::stream).
                filter(consentDecision -> consentDecision.getService().equals(service)).
                findFirst().
                orElseThrow(() -> new NotFoundException(
                "Could not find consent decision for owner " + owner + " and service " + service));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<WAConsentDecision> list() {
        return authProfileDAO.findAll(Pageable.unpaged()).stream().
                map(AuthProfile::getConsentDecisions).
                flatMap(List::stream).
                toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<WAConsentDecision> read(final String owner) {
        return authProfileDAO.findByOwner(owner).
                map(AuthProfile::getConsentDecisions).
                orElseGet(List::of);
    }
}
