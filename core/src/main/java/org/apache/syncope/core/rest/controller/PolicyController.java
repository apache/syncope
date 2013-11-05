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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.rest.data.PolicyDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/policy")
public class PolicyController extends AbstractTransactionalController<PolicyTO> {

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyDataBinder binder;

    @RequestMapping(method = RequestMethod.POST, value = "/password/create")
    public PasswordPolicyTO create(final HttpServletResponse response, @RequestBody final PasswordPolicyTO policyTO) {
        return createInternal(policyTO);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/account/create")
    public AccountPolicyTO create(final HttpServletResponse response, @RequestBody final AccountPolicyTO policyTO) {
        return createInternal(policyTO);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/sync/create")
    public SyncPolicyTO create(final HttpServletResponse response, @RequestBody final SyncPolicyTO policyTO) {
        return createInternal(policyTO);
    }

    @PreAuthorize("hasRole('POLICY_CREATE')")
    public <T extends PolicyTO> T createInternal(final T policyTO) {
        LOG.debug("Creating policy " + policyTO);
        return binder.getPolicyTO(policyDAO.save(binder.getPolicy(null, policyTO)));
    }

    private <T extends PolicyTO, K extends Policy> T update(final T policyTO, final K policy) {
        LOG.debug("Updating policy " + policyTO);
        binder.getPolicy(policy, policyTO);
        K savedPolicy = policyDAO.save(policy);
        return binder.getPolicyTO(savedPolicy);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/password/update")
    public PasswordPolicyTO update(@RequestBody final PasswordPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getId());
        if (!(policy instanceof PasswordPolicy)) {
            throw new NotFoundException("PasswordPolicy with id " + policyTO.getId());
        }

        return update(policyTO, policy);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/account/update")
    public AccountPolicyTO update(@RequestBody final AccountPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getId());
        if (!(policy instanceof AccountPolicy)) {
            throw new NotFoundException("AccountPolicy with id " + policyTO.getId());
        }

        return update(policyTO, policy);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/sync/update")
    public SyncPolicyTO update(@RequestBody final SyncPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getId());
        if (!(policy instanceof SyncPolicy)) {
            throw new NotFoundException("SyncPolicy with id " + policyTO.getId());
        }

        return update(policyTO, policy);
    }

    @PreAuthorize("hasRole('POLICY_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<PolicyTO> list(@PathVariable("kind") final String kind) {
        LOG.debug("Listing policies");
        List<? extends Policy> policies = policyDAO.find(PolicyType.valueOf(kind.toUpperCase(Locale.ENGLISH)));

        final List<PolicyTO> policyTOs = new ArrayList<PolicyTO>();
        for (Policy policy : policies) {
            policyTOs.add(binder.getPolicyTO(policy));
        }

        return policyTOs;
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/password/global/read")
    public PasswordPolicyTO getGlobalPasswordPolicy() {
        LOG.debug("Reading global password policy");

        PasswordPolicy policy = policyDAO.getGlobalPasswordPolicy();
        if (policy == null) {
            throw new NotFoundException("No password policy found");
        }

        return (PasswordPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/account/global/read")
    public AccountPolicyTO getGlobalAccountPolicy() {
        LOG.debug("Reading global account policy");

        AccountPolicy policy = policyDAO.getGlobalAccountPolicy();
        if (policy == null) {
            throw new NotFoundException("No account policy found");
        }

        return (AccountPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/sync/global/read")
    public SyncPolicyTO getGlobalSyncPolicy() {
        LOG.debug("Reading global sync policy");

        SyncPolicy policy = policyDAO.getGlobalSyncPolicy();
        if (policy == null) {
            throw new NotFoundException("No sync policy found");
        }

        return (SyncPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{id}")
    public <T extends PolicyTO> T read(@PathVariable("id") final Long id) {
        LOG.debug("Reading policy with id {}", id);

        Policy policy = policyDAO.find(id);
        if (policy == null) {
            throw new NotFoundException("Policy " + id + " not found");
        }

        return binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{id}")
    public PolicyTO delete(@PathVariable("id") final Long id) {
        Policy policy = policyDAO.find(id);
        if (policy == null) {
            throw new NotFoundException("Policy " + id + " not found");
        }

        PolicyTO policyToDelete = binder.getPolicyTO(policy);
        policyDAO.delete(id);

        return policyToDelete;
    }

    @PreAuthorize("hasRole('POLICY_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/syncCorrelationRuleClasses")
    public ModelAndView getSyncCorrelationRuleClasses() {
        return new ModelAndView().addObject(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_CORRELATION_RULES));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PolicyTO resolveReference(final Method method, final Object... args) throws
            UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof PolicyTO) {
                    id = ((PolicyTO) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getPolicyTO(policyDAO.find(id));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
