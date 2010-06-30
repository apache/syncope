
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
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.SearchParameters;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.UserTOs;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.rest.data.UserDataBinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private UserDataBinder userDataBinder;

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(HttpServletResponse response,
            @RequestBody UserTO userTO) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("create called with parameter " + userTO);
        }

        SyncopeUser user = null;
        try {
            user = userDataBinder.createSyncopeUser(userTO);
        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + userTO, e);
            return throwCompositeException(e, response);
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return userDataBinder.getUserTO(user);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            throwNotFoundException(String.valueOf(userId), response);
        } else {
            syncopeUserDAO.delete(userId);
            syncopeUserDAO.getEntityManager().flush();
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/isActive/{userId}")
    public ModelAndView isActive(@PathVariable("userId") Long userId)
            throws IOException {

        // TODO: check workflow
        ModelAndView mav = new ModelAndView();

        mav.addObject(syncopeUserDAO.find(userId) != null);

        return mav;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public UserTOs list(HttpServletRequest request) throws IOException {
        List<SyncopeUser> users = syncopeUserDAO.findAll();
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());

        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        UserTOs result = new UserTOs();
        result.setUsers(userTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    public UserTO read(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {
        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            return throwNotFoundException(String.valueOf(userId), response);
        }

        return userDataBinder.getUserTO(user);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/passwordReset/{userId}")
    public ModelAndView getPasswordResetToken(
            @PathVariable("userId") Long userId,
            @RequestParam("passwordResetFormURL") String passwordResetFormURL,
            @RequestParam("gotoURL") String gotoURL)
            throws IOException {
        log.info("passwordReset (GET) called with parameters " + userId + ", "
                + passwordResetFormURL + ", " + gotoURL);

        String passwordResetToken = "token";
        ModelAndView mav = new ModelAndView();

        mav.addObject(passwordResetToken);

        return mav;
    }

    @RequestMapping(method = RequestMethod.PUT,
    value = "/passwordReset/{userId}")
    public void passwordReset(@PathVariable("userId") Long userId,
            @RequestParam("tokenId") String tokenId,
            @RequestParam("newPassword") String newPassword)
            throws IOException {
        log.info("passwordReset (POST) called with parameters " + userId + ", "
                + tokenId + ", " + newPassword);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    public UserTOs search(HttpServletResponse response,
            @RequestBody SearchParameters searchParameters)
            throws IOException {
        log.info("search called with parameter " + searchParameters);

        List<UserTO> userTOs = new ArrayList<UserTO>();
        UserTOs result = new UserTOs();

        result.setUsers(userTOs);

        return result;
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(HttpServletResponse response,
            @RequestBody UserTO userTO)
            throws IOException {

        log.info("update called with parameter " + userTO);

        return userTO;
    }
}
