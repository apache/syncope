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
import org.apache.syncope.client.console.pages.Login;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.JVM)
public abstract class AbstractConsoleITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractConsoleITCase.class);

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String KEY = "key";

    protected static final String SCHEMA = "schema";

    protected static WicketTester TESTER;

    @BeforeClass
    public static void setUp() {
        TESTER = ConsoleSetup.TESTER;
    }

    protected void doLogin(final String user, final String passwd) {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        FormTester formTester = TESTER.newFormTester("login");
        formTester.setValue("username", user);
        formTester.setValue("password", passwd);
        formTester.submit("submit");
    }

    protected <V extends Serializable> Component findComponentByProp(
            final String property, final String searchPath, final V key) {

        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
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

    protected Component findComponentById(final String searchPath, final String id) {
        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(Component.class, new IVisitor<Component, Component>() {

                    @Override
                    public void component(final Component object, final IVisit<Component> visit) {
                        if (object.getId().equals(id)) {
                            visit.stop(object);
                        }
                    }
                });
    }

    protected void closeCallBack(final Component modal) {
        for (Behavior behavior : modal.getBehaviors()) {
            if (behavior instanceof AbstractAjaxBehavior) {
                TESTER.executeBehavior((AbstractAjaxBehavior) behavior);
            }
        }
    }
}
