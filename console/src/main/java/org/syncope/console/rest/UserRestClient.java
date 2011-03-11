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
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.PaginatedUserContainer;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking rest users services.
 */
@Component
public class UserRestClient extends AbstractBaseRestClient {

    public List<UserTO> getAllUsers() {
        List<UserTO> users = null;
        try {
            users = Arrays.asList(restTemplate.getForObject(
                    baseURL + "user/list.json", UserTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While listing all users", e);
        }
        return users;
    }

    /**
     * Create a new user and start off the workflow.
     * @param userTO instance
     * @throws SyncopeClientCompositeErrorException
     */
    public void createUser(final UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        restTemplate.postForObject(baseURL
                + "user/create", userTO, UserTO.class);
    }

    /**
     * Update existing user.
     * @param userTO
     * @return true is the opertion ends succesfully, false otherwise
     */
    public void updateUser(UserMod userModTO)
            throws SyncopeClientCompositeErrorException {

        restTemplate.postForObject(baseURL + "user/update",
                userModTO, UserTO.class);
    }

    public void deleteUser(String id) {
        try {
            restTemplate.delete(baseURL
                    + "user/delete/{userId}", new Integer(id));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a user", e);
        }
    }

    public UserTO getUser(String id) {
        UserTO userTO = null;
        try {
            userTO = restTemplate.getForObject(
                    baseURL
                    + "user/read/{userId}.json",
                    UserTO.class, id);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a user", e);
        }
        return userTO;
    }

    /**
     * Search an user by its schema values.
     * @param userTO
     * @return UserTOs
     */
    public List<UserTO> searchUsers(NodeCond nodeSearchCondition)
            throws SyncopeClientCompositeErrorException {

        return Arrays.asList(restTemplate.postForObject(
                baseURL + "user/search",
                nodeSearchCondition, UserTO[].class));
    }

    public PaginatedUserContainer paginatedSearchUser(NodeCond nodeSearchCond,
            int page, int size)
            throws SyncopeClientCompositeErrorException {

        return restTemplate.postForObject(
                baseURL + "user/search/{page}/{size}",
                nodeSearchCond, PaginatedUserContainer.class, page, size);
    }

    public PaginatedUserContainer getPaginatedUser(int page, int size)
            throws SyncopeClientCompositeErrorException {

        return restTemplate.getForObject(
                baseURL + "user/list/{page}/{size}",
                PaginatedUserContainer.class, page, size);
    }
}
