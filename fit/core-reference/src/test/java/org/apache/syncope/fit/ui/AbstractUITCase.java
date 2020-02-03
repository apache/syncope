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
package org.apache.syncope.fit.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.feedback.ExactLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractUITCase.class);

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String ADDRESS = "http://localhost:9080/syncope/rest";

    protected static final String KEY = "key";

    protected static final String SCHEMA = "schema";

    protected static WicketTester TESTER;

    protected static SyncopeService SYNCOPE_SERVICE;

    protected static <V extends Serializable> Component findComponentByProp(
            final String property, final String searchPath, final V key) {

        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(ListItem.class, (ListItem<?> object, IVisit<Component> visit) -> {
                    try {
                        Method getter = PropertyResolver.getPropertyGetter(property, object.getModelObject());
                        if (getter != null && getter.invoke(object.getModelObject()).equals(key)) {
                            visit.stop(object);
                        }
                    } catch (Exception e) {
                        LOG.error("Error invoke method", e);
                    }
                });
    }

    protected static <V extends Serializable> Component findComponentByPropNotNull(
            final String property, final String searchPath) {

        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(ListItem.class, (ListItem<?> object, IVisit<Component> visit) -> {
                    try {
                        Method getter = PropertyResolver.getPropertyGetter(property, object.getModelObject());
                        if (getter != null && getter.invoke(object.getModelObject()) != null) {
                            visit.stop(object);
                        }
                    } catch (Exception e) {
                        LOG.error("Error invoke method", e);
                    }
                });
    }

    protected static Component findComponentById(final String searchPath, final String id) {
        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(Component.class, (final Component object, final IVisit<Component> visit) -> {
                    if (object.getId().equals(id)) {
                        visit.stop(object);
                    }
                });
    }

    protected static Component findComponentByMarkupId(final String searchPath, final String markupId) {
        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(Component.class, (final Component object, final IVisit<Component> visit) -> {
                    if (object.getMarkupId().equals(markupId)) {
                        visit.stop(object);
                    }
                });
    }

    protected static void closeCallBack(final Component modal) {
        modal.getBehaviors().stream().
                filter(behavior -> (behavior instanceof AbstractAjaxBehavior)).
                forEachOrdered(behavior -> TESTER.executeBehavior((AbstractAjaxBehavior) behavior));
    }

    protected static void assertSuccessMessage() {
        Set<Serializable> messages =
                TESTER.getFeedbackMessages(new ExactLevelFeedbackMessageFilter(FeedbackMessage.INFO)).stream().
                        map(FeedbackMessage::getMessage).collect(Collectors.toSet());
        if (messages.size() != 1) {
            fail("Expected single message but found " + messages);
        }
        assertEquals("Operation successfully executed", messages.iterator().next());
    }
}
