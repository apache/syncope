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
import org.springframework.web.client.HttpServerErrorException;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.PaginatedResult;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;

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
     */
    public void createUser(UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        // Create user
        restTemplate.postForObject(baseURL
                + "user/create", userTO, UserTO.class);
    }

    /**
     * Update existing user.
     * @param userTO
     * @return true is the opertion ends succesfully, false otherwise
     */
    public boolean updateUser(UserMod userModTO)
            throws SyncopeClientCompositeErrorException {

        UserTO newUserTO = null;

        newUserTO = restTemplate.postForObject(
                baseURL
                + "user/update", userModTO, UserTO.class);

        return userModTO.getId() == newUserTO.getId();
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
     * Create a new configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean createConfigurationAttributes(
            ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO =
                restTemplate.postForObject(
                baseURL + "configuration/create",
                configurationTO, ConfigurationTO.class);

        return configurationTO.equals(newConfigurationTO);
    }

    /**
     * Update an existent configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean updateConfigurationAttributes(
            ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO = restTemplate.postForObject(
                baseURL + "configuration/update",
                configurationTO, ConfigurationTO.class);

        return configurationTO.equals(newConfigurationTO);
    }

    /**
     * Load an existent configuration.
     * @return ConfigurationTO object if the configuration exists,
     * null otherwise
     */
    public ConfigurationTO readConfigurationDisplayAttributes() {

        ConfigurationTO configurationTO;
        try {
            configurationTO = restTemplate.getForObject(
                    baseURL + "configuration/read/{confKey}",
                    ConfigurationTO.class,
                    Constants.CONF_USERS_ATTRIBUTES_VIEW);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a conf key", e);
            return null;
        }

        return configurationTO;
    }

    /**
     * Search an user by its schema values.
     * @param userTO
     * @return UserTOs
     */
    public List<UserTO> searchUsers(NodeCond nodeSearchCondition)
            throws HttpServerErrorException {
        List<UserTO> matchedUsers = null;

        matchedUsers = Arrays.asList(restTemplate.postForObject(
                baseURL + "user/search",
                nodeSearchCondition, UserTO[].class));

        return matchedUsers;
    }

    /**
     * Search an user by its schema values.
     * @param userTO
     * @return UserTOs
     */
    public List<UserTO> paginatedSearchUsers(NodeCond nodeSearchCondition,
            int page, int size)
            throws HttpServerErrorException {
        List<UserTO> matchedUsers = null;

        final PaginatedResult paginatedResult =
                restTemplate.postForObject(
                baseURL + "user/paginatedSearch/{page}/{size}",
                nodeSearchCondition, PaginatedResult.class, page, size);

        matchedUsers = paginatedResult.getRecords();

        return matchedUsers;
    }

    public PaginatedResult paginatedSearchUser(NodeCond nodeSearchCondition,
            int page, int size)
            throws HttpServerErrorException,
            SyncopeClientCompositeErrorException {

        PaginatedResult paginatedResult =
                restTemplate.postForObject(
                baseURL + "user/paginatedSearch/{page}/{size}",
                nodeSearchCondition, PaginatedResult.class, page, size);

        return paginatedResult;
    }

    public PaginatedResult getPaginatedUser(int page, int size)
            throws HttpServerErrorException {

        PaginatedResult paginatedResult =
                restTemplate.getForObject(
                baseURL + "user/paginatedList/{page}/{size}",
                PaginatedResult.class, page, size);

        return paginatedResult;
    }
}
