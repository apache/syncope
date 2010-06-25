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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.syncope.client.to.SearchParameters;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

/**
 * TODO: call syncope-core
 */
@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public UserTO create(HttpServletResponse response,
            @RequestBody UserTO userTO) throws IOException {

        log.info("create called with parameter " + userTO);

        return userTO;
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{userId}")
    public void delete(HttpServletResponse response,
            @PathVariable("userId") Long userId) throws IOException {

        log.info("delete called with parameter " + userId);

        if (userId == 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public Set<UserTO> list(HttpServletRequest request) throws IOException {

        WebApplicationContext webApplicationContext =
                RequestContextUtils.getWebApplicationContext(request);

        SyncopeUserDAO syncopeUserDAO =
                (SyncopeUserDAO) webApplicationContext.getBean(
                "syncopeUserDAOImpl");

        return Collections.singleton(new UserTO());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/read/{userId}")
    public ModelAndView read(@PathVariable("userId") Long userId)
            throws IOException {

        log.info("read called with parameter " + userId);

        Set<String> usernameValues = new HashSet<String>();
        usernameValues.add("chicchiricco");
        usernameValues.add("fabio.martelli");

        Set<String> surnameValues = new HashSet<String>();
        surnameValues.add("Chicchiriccò");
        surnameValues.add("Martelli");

        UserTO userTO = new UserTO();
        userTO.addAttribute("username", usernameValues);
        userTO.addAttribute("surname", surnameValues);
        //userTO.setAttributes(attributes);

        ModelAndView mav = new ModelAndView();
        mav.addObject(userTO);
        return mav;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/passwordReset/{userId}")
    public ModelAndView getPasswordResetToken(
            @PathVariable("userId") Long userId,
            @RequestParam("passwordResetFormURL") String passwordResetFormURL,
            @RequestParam("gotoURL") String gotoURL)
            throws IOException {

        log.info("passwordReset (GET) called with parameters "
                + userId + ", " + passwordResetFormURL + ", " + gotoURL);

        String passwordResetToken = "token";

        ModelAndView mav = new ModelAndView();
        mav.addObject(passwordResetToken);
        return mav;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/passwordReset/{userId}")
    public void passwordReset(
            @PathVariable("userId") Long userId,
            @RequestParam("tokenId") String tokenId,
            @RequestParam("newPassword") String newPassword)
            throws IOException {

        log.info("passwordReset (POST) called with parameters "
                + userId + ", " + tokenId + ", " + newPassword);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/search")
    public List<UserTO> search(HttpServletResponse response,
            @RequestBody SearchParameters searchParameters) throws IOException {

        log.info("search called with parameter " + searchParameters);

        List<UserTO> searchResult = new ArrayList<UserTO>();

        return searchResult;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public UserTO update(HttpServletResponse response,
            @RequestBody UserTO userTO) throws IOException {

        log.info("update called with parameter " + userTO);

        return userTO;
    }
}
