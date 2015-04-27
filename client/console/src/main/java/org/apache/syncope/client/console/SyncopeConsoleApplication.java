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
package org.apache.syncope.client.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

public class SyncopeConsoleApplication extends AuthenticatedWebApplication {

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            new Locale[] {
                Locale.ENGLISH, Locale.ITALIAN, new Locale("pt", "BR")
            }));

    @Override
    protected void init() {
        super.init();

        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
        getJavaScriptLibrarySettings().setJQueryReference(new DynamicJQueryResourceReference());

        getSecuritySettings().setAuthorizationStrategy(new MetaDataRoleAuthorizationStrategy(this));
        MetaDataRoleAuthorizationStrategy.authorize(getHomePage(), SyncopeConsoleSession.AUTHENTICATED);

        mountPage("/login", getSignInPageClass());
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return SyncopeConsoleSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return Dashboard.class;
    }

}
