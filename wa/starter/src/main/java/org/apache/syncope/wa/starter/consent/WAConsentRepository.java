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
package org.apache.syncope.wa.starter.consent;

import java.util.Collection;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.common.rest.api.service.wa.ConsentDecisionService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.consent.ConsentDecision;
import org.apereo.cas.consent.ConsentReminderOptions;
import org.apereo.cas.consent.ConsentRepository;
import org.apereo.cas.services.RegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAConsentRepository implements ConsentRepository {

    private static final long serialVersionUID = -3094119228321296264L;

    protected static final Logger LOG = LoggerFactory.getLogger(WAConsentRepository.class);

    protected static WAConsentDecision toWAConsentDecision(final ConsentDecision decision) {
        return new WAConsentDecision.Builder(
                decision.getId(), decision.getPrincipal(), decision.getService(), decision.getCreatedDate()).
                options(WAConsentDecision.ReminderOptions.valueOf(decision.getOptions().name())).
                reminder(decision.getReminder()).
                reminderTimeUnit(decision.getReminderTimeUnit()).
                attributes(decision.getAttributes()).
                build();
    }

    protected static ConsentDecision toConsentDecision(final String tenant, final WAConsentDecision waConsentDecision) {
        ConsentDecision consentDecision = new ConsentDecision();
        consentDecision.setId(waConsentDecision.getId());
        consentDecision.setPrincipal(waConsentDecision.getPrincipal());
        consentDecision.setService(waConsentDecision.getService());
        consentDecision.setCreatedDate(waConsentDecision.getCreatedDate());
        consentDecision.setOptions(ConsentReminderOptions.valueOf(waConsentDecision.getOptions().getValue()));
        consentDecision.setReminder(waConsentDecision.getReminder());
        consentDecision.setReminderTimeUnit(waConsentDecision.getReminderTimeUnit());
        consentDecision.setTenant(tenant);
        consentDecision.setAttributes(waConsentDecision.getAttributes());
        return consentDecision;
    }

    protected final WARestClient waRestClient;

    public WAConsentRepository(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public ConsentDecision findConsentDecision(
            final Service service,
            final RegisteredService registeredService,
            final Authentication authentication) {

        try {
            WAConsentDecision waContentDecision = waRestClient.getService(ConsentDecisionService.class).
                    read(authentication.getPrincipal().getId(), service.getId());
            return toConsentDecision(waRestClient.getSyncopeClient().getDomain(), waContentDecision);
        } catch (Exception e) {
            LOG.error("While attempting to find ConsentDecision for principal {} and service {}",
                    authentication.getPrincipal().getId(), service.getId(), e);
            return null;
        }
    }

    @Override
    public Collection<? extends ConsentDecision> findConsentDecisions(final String principal) {
        return waRestClient.getService(ConsentDecisionService.class).read(principal).getResult().stream().
                map(wcd -> toConsentDecision(waRestClient.getSyncopeClient().getDomain(), wcd)).
                toList();
    }

    @Override
    public Collection<? extends ConsentDecision> findConsentDecisions() {
        return waRestClient.getService(ConsentDecisionService.class).list().getResult().stream().
                map(wcd -> toConsentDecision(waRestClient.getSyncopeClient().getDomain(), wcd)).
                toList();
    }

    @Override
    public ConsentDecision storeConsentDecision(final ConsentDecision decision) throws Throwable {
        waRestClient.getService(ConsentDecisionService.class).
                store(decision.getPrincipal(), toWAConsentDecision(decision));
        return decision;
    }

    @Override
    public boolean deleteConsentDecision(final long id, final String principal) throws Throwable {
        waRestClient.getService(ConsentDecisionService.class).delete(principal, id);
        return true;
    }

    @Override
    public boolean deleteConsentDecisions(final String principal) throws Throwable {
        waRestClient.getService(ConsentDecisionService.class).delete(principal);
        return true;
    }

    @Override
    public void deleteAll() throws Throwable {
        waRestClient.getService(ConsentDecisionService.class).deleteAll();
    }
}
