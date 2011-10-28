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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.syncope.client.mod.AccountPolicyMod;
import org.syncope.client.mod.PasswordPolicyMod;
import org.syncope.client.mod.SyncPolicyMod;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.rest.data.PolicyDataBinder;
import org.syncope.types.PolicyType;

@Controller
@RequestMapping("/policy")
public class PolicyController extends AbstractController {

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyDataBinder policyDataBinder;

    @PreAuthorize("hasRole('POLICY_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/password/create")
    public PasswordPolicyTO create(final HttpServletResponse response,
            final @RequestBody PasswordPolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        final PasswordPolicy policy =
                (PasswordPolicy) policyDataBinder.getPolicy(policyTO);

        return (PasswordPolicyTO) create(policy, policyTO);
    }

    @PreAuthorize("hasRole('POLICY_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/account/create")
    public AccountPolicyTO create(final HttpServletResponse response,
            final @RequestBody AccountPolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        final AccountPolicy policy =
                (AccountPolicy) policyDataBinder.getPolicy(policyTO);

        return (AccountPolicyTO) create(policy, policyTO);
    }

    @PreAuthorize("hasRole('POLICY_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/sync/create")
    public SyncPolicyTO create(final HttpServletResponse response,
            final @RequestBody SyncPolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        final SyncPolicy policy =
                (SyncPolicy) policyDataBinder.getPolicy(policyTO);

        return (SyncPolicyTO) create(policy, policyTO);
    }

    private PolicyTO create(final Policy policy, final PolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        LOG.debug("Creating policy " + policyTO);

        Policy actual = policyDAO.save(policy);
        policyTO.setId(actual.getId());


        return policyTO;
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/password/update")
    public PasswordPolicyTO update(final HttpServletResponse response,
            final @RequestBody PasswordPolicyMod policyMod)
            throws NotFoundException {

        LOG.debug("Updating policy " + policyMod);

        final PasswordPolicy policy =
                (PasswordPolicy) policyDataBinder.getPolicy(policyMod);

        final Policy actual = update(policy);
        return (PasswordPolicyTO) policyDataBinder.getPolicyTO(actual);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/account/update")
    public AccountPolicyTO update(final HttpServletResponse response,
            final @RequestBody AccountPolicyMod policyMod)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        LOG.debug("Updating policy " + policyMod);

        final AccountPolicy policy =
                (AccountPolicy) policyDataBinder.getPolicy(policyMod);

        final Policy actual = update(policy);
        return (AccountPolicyTO) policyDataBinder.getPolicyTO(actual);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/sync/update")
    public SyncPolicyTO update(final HttpServletResponse response,
            final @RequestBody SyncPolicyMod policyMod)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        LOG.debug("Updating policy " + policyMod);

        final SyncPolicy policy =
                (SyncPolicy) policyDataBinder.getPolicy(policyMod);

        final Policy actual = update(policy);
        return (SyncPolicyTO) policyDataBinder.getPolicyTO(actual);
    }

    private Policy update(final Policy policy)
            throws NotFoundException {

        LOG.debug("Updating policy " + policy.getId());

        if (policy.getId() == null) {
            throw new NotFoundException("Policy with null id");
        }

        return policyDAO.save(policy);
    }

    @PreAuthorize("hasRole('POLICY_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<PolicyTO> listByType(
            final HttpServletResponse response,
            @PathVariable("kind") final String kind) {

        LOG.debug("Listing policies");
        List<Policy> policies =
                policyDAO.find(PolicyType.valueOf(kind.toUpperCase()));

        final List<PolicyTO> policyTOs = new ArrayList<PolicyTO>();

        for (Policy policy : policies) {
            policyTOs.add(policyDataBinder.getPolicyTO(policy));
        }

        return policyTOs;
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/password/global/read")
    public PasswordPolicyTO getGlobalPasswordPolicy(
            final HttpServletResponse response)
            throws NotFoundException {

        LOG.debug("Reading password policy");
        Policy policy = policyDAO.getGlobalPasswordPolicy();

        if (policy == null) {
            throw new NotFoundException("No password policy found");
        }

        return (PasswordPolicyTO) policyDataBinder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/account/global/read")
    public AccountPolicyTO getGlobalAccountPolicy(
            final HttpServletResponse response)
            throws NotFoundException {

        LOG.debug("Reading account policy");
        Policy policy = policyDAO.getGlobalAccountPolicy();

        if (policy == null) {
            throw new NotFoundException("No account policy found");
        }

        return (AccountPolicyTO) policyDataBinder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/sync/global/read")
    public SyncPolicyTO getGlobalSyncPolicy(
            final HttpServletResponse response)
            throws NotFoundException {

        LOG.debug("Reading sync policy");
        Policy policy = policyDAO.getGlobalSyncPolicy();

        if (policy == null) {
            throw new NotFoundException("No sync policy found");
        }

        return (SyncPolicyTO) policyDataBinder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{id}")
    public PolicyTO read(
            final HttpServletResponse response,
            @PathVariable("id") final Long id)
            throws NotFoundException {

        LOG.debug("Reading policy");
        Policy policy = policyDAO.find(id);

        if (policy == null) {
            throw new NotFoundException("Policy " + id + " not found");
        }
        return policyDataBinder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{id}")
    public void delete(
            final HttpServletResponse response,
            @PathVariable("id") final Long id)
            throws NotFoundException {

        LOG.debug("Delete policy");
        policyDAO.delete(id);
    }
}
