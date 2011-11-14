/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.beans.UserRequest;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.UserRequestDAO;
import org.syncope.core.rest.data.UserRequestDataBinder;

@Controller
@RequestMapping("/user/request")
public class UserRequestController {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(UserRequestController.class);

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private UserRequestDAO userRequestDAO;

    @Autowired
    private UserRequestDataBinder dataBinder;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/self")
    @Transactional(readOnly = true)
    public UserTO read()
            throws NotFoundException {

        return dataBinder.getAuthUserTO();
    }

    private Boolean isCreateAllowedByConf() {
        SyncopeConf createRequestAllowed =
                confDAO.find("createRequest.allowed", "false");

        return Boolean.valueOf(createRequestAllowed.getValue());
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/create/allowed")
    @Transactional(readOnly = true)
    public ModelAndView isCreateAllowed() {

        return new ModelAndView().addObject(
                isCreateAllowedByConf());
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserRequestTO create(@RequestBody final UserTO userTO)
            throws UnauthorizedRoleException {

        if (!isCreateAllowedByConf()) {
            LOG.error("Create requests are not allowed");

            throw new UnauthorizedRoleException(-1L);
        }

        LOG.debug("Request user create called with {}", userTO);

        try {
            dataBinder.testCreate(userTO);
        } catch (RollbackException e) {
        }

        UserRequest request = new UserRequest();
        request.setUserTO(userTO);
        request = userRequestDAO.save(request);

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserRequestTO update(@RequestBody final UserMod userMod)
            throws NotFoundException, UnauthorizedRoleException {

        LOG.debug("Request user update called with {}", userMod);

        try {
            dataBinder.testUpdate(userMod);
        } catch (RollbackException e) {
        }

        UserRequest request = new UserRequest();
        request.setUserMod(userMod);
        request = userRequestDAO.save(request);

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.POST,
    value = "/delete")
    public UserRequestTO delete(@RequestBody final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        LOG.debug("Request user delete called with {}", userId);

        try {
            dataBinder.testDelete(userId);
        } catch (RollbackException e) {
        }

        UserRequest request = new UserRequest();
        request.setUserId(userId);
        request = userRequestDAO.save(request);

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("hasRole('USER_REQUEST_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    @Transactional(readOnly = true)
    public List<UserRequestTO> list() {
        List<UserRequestTO> result = new ArrayList<UserRequestTO>();

        for (UserRequest request : userRequestDAO.findAll()) {
            result.add(dataBinder.getUserRequestTO(request));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_REQUEST_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{requestId}")
    @Transactional(readOnly = true)
    public UserRequestTO read(@PathVariable("requestId") final Long requestId)
            throws NotFoundException {

        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("hasRole('USER_REQUEST_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/deleteRequest/{requestId}")
    public void deleteRequest(@PathVariable("requestId") final Long requestId)
            throws NotFoundException {

        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        userRequestDAO.delete(requestId);
    }
}
