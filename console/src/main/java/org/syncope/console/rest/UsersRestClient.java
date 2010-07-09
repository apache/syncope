/*
 * 
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

import java.util.Set;
import org.syncope.client.to.UserTO;

/**
 * Console client for invoking rest users services.
 */
public class UsersRestClient

{    
     RestClient restClient;

     public Set<UserTO> getUserList() {
        Set<UserTO> listUsers =
                restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "user/list.json", Set.class);
        return listUsers;
    }

    public UserTO getUser() {
        UserTO userTO =
                restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "user/read/{userId}.json",
                UserTO.class, "11");
        return userTO;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
    
}
