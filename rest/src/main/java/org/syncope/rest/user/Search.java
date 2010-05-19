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
package org.syncope.rest.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.syncope.rest.user.jaxb.SearchParameters;
import org.syncope.rest.user.jaxb.SearchResults;

@Path("/user/search")
@Component
@Scope("request")
public class Search {

    private static final Logger log = LoggerFactory.getLogger(Search.class);

    public static SearchResults getTestValue() {
        SearchResults searchResults = new SearchResults();
        searchResults.addResult("user1");
        searchResults.addResult("user2");
        searchResults.addResult("user3");

        return searchResults;
    }

    /**
     * TODO: call syncope-core
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResults searchUser(SearchParameters searchParameters,
            @DefaultValue("FALSE") @QueryParam("test") boolean test) {

        log.info("searchUser() called with parameters " + searchParameters);

        if (test) {
            return getTestValue();
        }

        SearchResults searchResults = new SearchResults();
        searchResults.addResult("user1");
        searchResults.addResult("user2");
        searchResults.addResult("user3");

        return searchResults;
    }
}
