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
package org.apache.syncope.fit.console;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.servlet.ServletContext;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.wicket.Component;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.junit.Before;

public abstract class AbstractConsoleITCase extends AbstractITCase {

    protected static final String KEY = "key";

    protected static final String SCHEMA = "schema";

    protected WicketTester wicketTester;

    protected SyncopeConsoleApplication testApplicaton;

    @Before
    public void setUp() {
        testApplicaton = new SyncopeConsoleApplication() {

            @Override
            protected void init() {
                final ServletContext ctx = getServletContext();
                final ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
                lookup.load();
                ctx.setAttribute(ConsoleInitializer.CLASSPATH_LOOKUP, lookup);

                final MIMETypesLoader mimeTypes = new MIMETypesLoader();
                mimeTypes.load();
                ctx.setAttribute(ConsoleInitializer.MIMETYPES_LOADER, mimeTypes);

                super.init();
            }
        };

        wicketTester = new WicketTester(testApplicaton);
    }

    protected void doLogin(final String user, final String passwd) {
        wicketTester.startPage(Login.class);
        wicketTester.assertRenderedPage(Login.class);

        FormTester formTester = wicketTester.newFormTester("login");
        formTester.setValue("username", user);
        formTester.setValue("password", passwd);
        formTester.submit("submit");
    }

    protected <V extends Serializable> Component findComponentByProp(
            final String property, final String searchPath, final V key) {

        return wicketTester.getComponentFromLastRenderedPage(searchPath).getPage().
                visitChildren(ListItem.class, new IVisitor<ListItem<?>, Component>() {

                    @Override
                    public void component(final ListItem<?> object, final IVisit<Component> visit) {
                        try {
                            Method getter = PropertyResolver.getPropertyGetter(property, object.getModelObject());
                            if (getter != null && getter.invoke(object.getModelObject()).equals(key)) {
                                visit.stop(object);
                            }
                        } catch (Exception e) {
                            LOG.error("Error invoke method", e);
                        }
                    }
                });
    }
}
