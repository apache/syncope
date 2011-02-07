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
package org.syncope.core.rest;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:syncopeContext.xml",
    "classpath:persistenceContext.xml",
    "classpath:workflowContext.xml",
    "classpath:restContext.xml"
})
public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractTest.class);

    protected static final String BASE_URL =
            "http://localhost:9080/syncope/rest/";

    @Autowired
    protected RestTemplate restTemplate;

    @Before
    public void setupRestTemplate() {
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "password"));
    }
}
