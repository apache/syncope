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
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking rest users services.
 */
@Component
public class UserRestClient extends AbstractBaseRestClient {

    public Integer count() {
        return restTemplate.getForObject(baseURL + "user/count.json",
                Integer.class);
    }

    /**
     * Get all stored users.
     * @param page pagination element to fetch
     * @param size maximum number to fetch
     * @return list of TaskTO objects
     */
    public List<UserTO> list(final int page, final int size) {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "user/list/{page}/{size}.json",
                UserTO[].class, page, size));
    }

    /**
     * Create a new user and start off the workflow.
     * @param userTO instance
     * @throws SyncopeClientCompositeErrorException
     */
    public void create(final UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        restTemplate.postForObject(baseURL
                + "user/create", userTO, UserTO.class);
    }

    /**
     * Update existing user.
     * @param userTO
     * @return true is the opertion ends succesfully, false otherwise
     */
    public void update(UserMod userModTO)
            throws SyncopeClientCompositeErrorException {

        restTemplate.postForObject(baseURL + "user/update",
                userModTO, UserTO.class);
    }

    public void delete(Long id) {
        restTemplate.delete(baseURL + "user/delete/{userId}", id);
    }

    public UserTO read(String id) {
        UserTO userTO = null;
        try {
            userTO = restTemplate.getForObject(
                    baseURL + "user/read/{userId}.json",
                    UserTO.class, id);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a user", e);
        }
        return userTO;
    }

    public Integer searchCount(final NodeCond searchCond) {
        return restTemplate.postForObject(
                baseURL + "user/search/count.json", searchCond, Integer.class);
    }

    /**
     * Search an user by its schema values.
     * @param userTO
     * @return UserTOs
     */
    public List<UserTO> search(final NodeCond searchCond)
            throws SyncopeClientCompositeErrorException {

        return Arrays.asList(restTemplate.postForObject(
                baseURL + "user/search",
                searchCond, UserTO[].class));
    }

    public List<UserTO> search(final NodeCond searchCond,
            final int page, final int size)
            throws SyncopeClientCompositeErrorException {

        return Arrays.asList(restTemplate.postForObject(
                baseURL + "user/search/{page}/{size}",
                searchCond, UserTO[].class, page, size));
    }
}
