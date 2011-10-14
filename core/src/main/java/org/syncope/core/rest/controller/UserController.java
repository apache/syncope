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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.rest.data.UserDataBinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.workflow.UserWorkflowAdapter;
import org.syncope.core.workflow.WorkflowException;

@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSearchDAO userSearchDAO;

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private UserWorkflowAdapter wfAdapter;

    /**
     * Check if roles are allowed to be administered by the caller.
     *
     * @param roleIds roles to be administered
     * @throws UnauthorizedRoleException if permissions are not sufficient
     */
    private void checkPermissions(final Set<Long> roleIds)
            throws UnauthorizedRoleException {

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        roleIds.removeAll(adminRoleIds);
        if (!roleIds.isEmpty()) {
            throw new UnauthorizedRoleException(roleIds);
        }
    }

    private SyncopeUser getUserFromId(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        Set<Long> roleIds = new HashSet<Long>(user.getRoles().size());
        for (SyncopeRole role : user.getRoles()) {
            roleIds.add(role.getId());
        }
        checkPermissions(roleIds);

        return user;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/verifyPassword/{userId}")
    @Transactional(readOnly = true)
    public ModelAndView verifyPassword(@PathVariable("userId") Long userId,
            @RequestParam("password") final String password)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);

        SyncopeUser passwordUser = new SyncopeUser();
        passwordUser.setPassword(password, user.getCipherAlgoritm(), 0);

        return new ModelAndView().addObject(user.getPassword().
                equalsIgnoreCase(passwordUser.getPassword()));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/count")
    @Transactional(readOnly = true)
    public ModelAndView count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(userDAO.count(adminRoleIds));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search/count")
    @Transactional(readOnly = true)
    public ModelAndView searchCount(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(
                userSearchDAO.count(adminRoleIds, searchCondition));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    @Transactional(readOnly = true)
    public List<UserTO> list() {
        List<SyncopeUser> users = userDAO.findAll(EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames()));
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list/{page}/{size}")
    @Transactional(readOnly = true)
    public List<UserTO> list(
            @PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    @Transactional(readOnly = true)
    public UserTO read(@PathVariable("userId") final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        return userDataBinder.getUserTO(getUserFromId(userId));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    @Transactional(readOnly = true)
    public List<UserTO> search(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers = userSearchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.
                getOwnedEntitlementNames()), searchCondition);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search/{page}/{size}")
    @Transactional(readOnly = true)
    public List<UserTO> search(
            @RequestBody final NodeCond searchCondition,
            @PathVariable("page") final int page,
            @PathVariable("size") final int size)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final List<SyncopeUser> matchingUsers = userSearchDAO.search(
                EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, page, size);

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(final HttpServletResponse response,
            @RequestBody final UserTO userTO,
            @RequestParam(value = "mandatoryRoles",
            required = false) final Set<Long> mandatoryRoles,
            @RequestParam(value = "mandatoryResources",
            required = false) final Set<String> mandatoryResources)
            throws PropagationException, UnauthorizedRoleException,
            WorkflowException {

        LOG.debug("User create called with parameters {}\n{}\n{}",
                new Object[]{userTO, mandatoryRoles, mandatoryResources});

        Set<Long> requestRoleIds =
                new HashSet<Long>(userTO.getMemberships().size());
        for (MembershipTO membership : userTO.getMemberships()) {
            requestRoleIds.add(membership.getRoleId());
        }
        checkPermissions(requestRoleIds);

        final UserTO savedTO = userDataBinder.getUserTO(
                wfAdapter.create(userTO, mandatoryRoles, mandatoryResources));

        LOG.debug("About to return created user\n{}", savedTO);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/activate")
    public UserTO activate(@RequestBody final UserTO userTO)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException, PropagationException {

        final UserTO savedTO = userDataBinder.getUserTO(
                wfAdapter.activate(getUserFromId(userTO.getId()),
                userTO.getToken()));

        LOG.debug("About to return activated user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(@RequestBody final UserMod userMod,
            @RequestParam(value = "mandatoryRoles",
            required = false) final Set<Long> mandatoryRoles,
            @RequestParam(value = "mandatoryResources",
            required = false) final Set<String> mandatoryResources)
            throws NotFoundException, PropagationException,
            UnauthorizedRoleException, WorkflowException {

        LOG.debug("User update called with parameters {}\n{}\n{}",
                new Object[]{userMod, mandatoryRoles, mandatoryResources});

        final UserTO updatedTO = userDataBinder.getUserTO(
                wfAdapter.update(getUserFromId(userMod.getId()), userMod,
                mandatoryRoles, mandatoryResources));

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/suspend/{userId}")
    public UserTO suspend(@PathVariable("userId") final Long userId)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + userId);

        final UserTO savedTO = userDataBinder.getUserTO(
                wfAdapter.suspend(getUserFromId(userId)));

        LOG.debug("About to return suspended user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/reactivate/{userId}")
    public UserTO reactivate(final @PathVariable("userId") Long userId)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + userId);

        final UserTO savedTO = userDataBinder.getUserTO(
                wfAdapter.reactivate(getUserFromId(userId)));

        LOG.debug("About to return suspended user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(@PathVariable("userId") final Long userId,
            @RequestParam(value = "mandatoryRoles",
            required = false) final Set<Long> mandatoryRoles,
            @RequestParam(value = "mandatoryResources",
            required = false) final Set<String> mandatoryResources)
            throws NotFoundException, WorkflowException, PropagationException,
            UnauthorizedRoleException {

        LOG.debug("User delete called with parameters {}\n{}\n{}",
                new Object[]{userId, mandatoryRoles, mandatoryResources});

        wfAdapter.delete(getUserFromId(userId),
                mandatoryRoles, mandatoryResources);

        LOG.debug("User successfully deleted: {}", userId);
    }
}
