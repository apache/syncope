/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.enduser;

import org.apache.syncope.client.enduser.pages.HomePage;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.tester.WicketTester;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SyncopeEnduserApplicationTest {

    private WicketTester tester;

    static class SyncopeEnduserMockSession extends WebSession {

        private static final long serialVersionUID = -2500230416352618497L;

        SyncopeEnduserMockSession(final Request request) {
            super(request);
        }
    }

    @Before
    public void setUp() {
        tester = new WicketTester(new SyncopeEnduserApplication() {

            private static final long serialVersionUID = 1445165406200746511L;

            @Override
            protected void init() {
                // just skip over actual init
            }

            @Override
            public Session newSession(final Request request, final Response response) {
                return new SyncopeEnduserMockSession(request);
            }

        });
    }

    @Test
    public void testRedirectToIndex() {
        tester.setFollowRedirects(false);
        tester.startPage(HomePage.class);
        tester.assertNoErrorMessage();
        MockHttpServletResponse response = tester.getLastResponse();
        Assert.assertThat(response.getRedirectLocation(), CoreMatchers.equalTo("app/"));
    }

}
