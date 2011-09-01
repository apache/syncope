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
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
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
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.AccountPolicy;
import org.syncope.types.EntityViolationType;
import org.syncope.types.PasswordPolicy;
import org.syncope.types.PolicyType;
import org.syncope.types.SyncPolicy;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/policy")
public class PolicyController extends AbstractController {

    @Autowired
    private PolicyDAO policyDAO;

    @PreAuthorize("hasRole('POLICY_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/password/create")
    public PasswordPolicyTO create(final HttpServletResponse response,
            final @RequestBody PasswordPolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        Policy policy = new Policy();
        policy.setType(policyTO.getType());
        policy.setSpecification(policyTO.getSpecification());

        return (PasswordPolicyTO) create(policy, policyTO);
    }

    @PreAuthorize("hasRole('POLICY_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/account/create")
    public AccountPolicyTO create(final HttpServletResponse response,
            final @RequestBody AccountPolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        Policy policy = new Policy();
        policy.setType(policyTO.getType());
        policy.setSpecification(policyTO.getSpecification());

        return (AccountPolicyTO) create(policy, policyTO);
    }

    @PreAuthorize("hasRole('POLICY_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/sync/create")
    public SyncPolicyTO create(final HttpServletResponse response,
            final @RequestBody SyncPolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        Policy policy = new Policy();
        policy.setType(policyTO.getType());
        policy.setSpecification(policyTO.getSpecification());

        return (SyncPolicyTO) create(policy, policyTO);

    }

    private PolicyTO create(final Policy policy, final PolicyTO policyTO)
            throws SyncopeClientCompositeErrorException {

        LOG.debug("Creating policy " + policyTO);

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        try {

            Policy actual = policyDAO.save(policy);
            policyTO.setId(actual.getId());

            return policyTO;

        } catch (InvalidEntityException e) {
            LOG.error("Policy {} cannot be crated", policy);

            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidPolicy);

            for (Map.Entry<Class, Set<EntityViolationType>> violation :
                    e.getViolations().entrySet()) {

                for (EntityViolationType violationType : violation.getValue()) {
                    sce.addElement(violation.getClass().getSimpleName() + ": "
                            + violationType);
                }
            }

            scce.addException(sce);
            throw scce;
        }
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/password/update")
    public PasswordPolicyTO update(final HttpServletResponse response,
            final @RequestBody PasswordPolicyMod policyMod)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        LOG.debug("Updating policy " + policyMod);

        Policy policy = new Policy();
        policy.setId(policyMod.getId());
        policy.setType(policyMod.getType());
        policy.setSpecification(policyMod.getSpecification());

        Policy actual = update(policy);

        PasswordPolicyTO policyTO = new PasswordPolicyTO();
        policyTO.setId(actual.getId());
        policyTO.setType(actual.getType());
        policyTO.setSpecification((PasswordPolicy) actual.getSpecification());

        return policyTO;
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/account/update")
    public AccountPolicyTO update(final HttpServletResponse response,
            final @RequestBody AccountPolicyMod policyMod)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        LOG.debug("Updating policy " + policyMod);

        Policy policy = new Policy();
        policy.setId(policyMod.getId());
        policy.setType(policyMod.getType());
        policy.setSpecification(policyMod.getSpecification());

        Policy actual = update(policy);

        AccountPolicyTO policyTO = new AccountPolicyTO();
        policyTO.setId(actual.getId());
        policyTO.setType(actual.getType());
        policyTO.setSpecification((AccountPolicy) actual.getSpecification());

        return policyTO;
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/sync/update")
    public SyncPolicyTO update(final HttpServletResponse response,
            final @RequestBody SyncPolicyMod policyMod)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        LOG.debug("Updating policy " + policyMod);

        Policy policy = new Policy();
        policy.setId(policyMod.getId());
        policy.setType(policyMod.getType());
        policy.setSpecification(policyMod.getSpecification());

        Policy actual = update(policy);

        SyncPolicyTO policyTO = new SyncPolicyTO();
        policyTO.setId(actual.getId());
        policyTO.setType(actual.getType());
        policyTO.setSpecification((SyncPolicy) actual.getSpecification());

        return policyTO;
    }

    private Policy update(final Policy policy)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        LOG.debug("Updating policy " + policy.getId());

        if (policy.getId() == null) {
            throw new NotFoundException("Policy with null id");
        }

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        try {

            return policyDAO.save(policy);

        } catch (InvalidEntityException e) {
            LOG.error("Policy {} cannot be crated", policy);

            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidPolicy);

            for (Map.Entry<Class, Set<EntityViolationType>> violation :
                    e.getViolations().entrySet()) {

                for (EntityViolationType violationType : violation.getValue()) {
                    sce.addElement(violation.getClass().getSimpleName() + ": "
                            + violationType);
                }
            }

            scce.addException(sce);
            throw scce;
        }
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
        PolicyTO policyTO;

        for (Policy policy : policies) {
            switch (policy.getType()) {
                case PASSWORD:
                    policyTO = new PasswordPolicyTO();
                    ((PasswordPolicyTO) policyTO).setSpecification(
                            (PasswordPolicy) policy.getSpecification());
                    break;
                case ACCOUNT:
                    policyTO = new AccountPolicyTO();
                    ((AccountPolicyTO) policyTO).setSpecification(
                            (AccountPolicy) policy.getSpecification());
                    break;
                default:
                    policyTO = new SyncPolicyTO();
                    ((SyncPolicyTO) policyTO).setSpecification(
                            (SyncPolicy) policy.getSpecification());
            }

            policyTO.setId(policy.getId());
            policyTO.setType(policy.getType());
            policyTOs.add(policyTO);
        }

        return policyTOs;
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/password/read")
    public PasswordPolicyTO getPasswordPolicy(
            final HttpServletResponse response) throws NotFoundException {

        LOG.debug("Reading password policy");
        Policy policy = policyDAO.getPasswordPolicy();

        if (policy == null) {
            throw new NotFoundException("No password policy found");
        }

        PasswordPolicyTO policyTO = new PasswordPolicyTO();
        policyTO.setId(policy.getId());
        policyTO.setSpecification((PasswordPolicy) policy.getSpecification());
        policyTO.setType(policy.getType());

        return policyTO;
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/account/read")
    public AccountPolicyTO getAccountPolicy(
            final HttpServletResponse response) throws NotFoundException {

        LOG.debug("Reading account policy");
        Policy policy = policyDAO.getAccountPolicy();

        if (policy == null) {
            throw new NotFoundException("No account policy found");
        }

        AccountPolicyTO policyTO = new AccountPolicyTO();
        policyTO.setId(policy.getId());
        policyTO.setSpecification((AccountPolicy) policy.getSpecification());
        policyTO.setType(policy.getType());

        return policyTO;
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

        final PolicyTO policyTO;

        switch (policy.getType()) {
            case PASSWORD:
                policyTO = new PasswordPolicyTO();
                ((PasswordPolicyTO) policyTO).setSpecification(
                        (PasswordPolicy) policy.getSpecification());
                break;
            case ACCOUNT:
                policyTO = new AccountPolicyTO();
                ((AccountPolicyTO) policyTO).setSpecification(
                        (AccountPolicy) policy.getSpecification());
                break;
            default:
                policyTO = new SyncPolicyTO();
                ((SyncPolicyTO) policyTO).setSpecification(
                        (SyncPolicy) policy.getSpecification());

        }

        policyTO.setId(policy.getId());
        policyTO.setType(policy.getType());

        return policyTO;
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
