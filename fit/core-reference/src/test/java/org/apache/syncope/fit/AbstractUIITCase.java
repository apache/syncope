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
package org.apache.syncope.fit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
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
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUIITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractUIITCase.class);

    protected static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String ADDRESS = "http://localhost:9080/syncope/rest";

    protected static final String KEY = "key";

    protected static final String SCHEMA = "schema";

    protected static String ANONYMOUS_UNAME;

    protected static String ANONYMOUS_KEY;

    protected static WicketTester TESTER;

    protected static SyncopeService SYNCOPE_SERVICE;

    protected static boolean IS_FLOWABLE_ENABLED = false;

    protected static boolean IS_EXT_SEARCH_ENABLED = false;

    @BeforeAll
    public static void anonymousSetup() throws IOException {
        try (InputStream propStream = AbstractITCase.class.getResourceAsStream("/core.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            ANONYMOUS_UNAME = props.getProperty("security.anonymousUser");
            ANONYMOUS_KEY = props.getProperty("security.anonymousKey");
        } catch (Exception e) {
            LOG.error("Could not read core.properties", e);
        }

        assertNotNull(ANONYMOUS_UNAME);
        assertNotNull(ANONYMOUS_KEY);

        String beansJSON = await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return WebClient.create(StringUtils.substringBeforeLast(ADDRESS, "/") + "/actuator/beans",
                        ANONYMOUS_UNAME,
                        ANONYMOUS_KEY,
                        null).
                        accept(MediaType.APPLICATION_JSON).get().readEntity(String.class);
            } catch (Exception e) {
                return null;
            }
        }, Objects::nonNull);
        JsonNode beans = JSON_MAPPER.readTree(beansJSON);

        JsonNode uwfAdapter = beans.findValues("uwfAdapter").getFirst();
        IS_FLOWABLE_ENABLED = uwfAdapter.get("resource").asText().contains("Flowable");

        JsonNode anySearchDAO = beans.findValues("anySearchDAO").getFirst();
        IS_EXT_SEARCH_ENABLED = anySearchDAO.get("type").asText().contains("Elasticsearch")
                || anySearchDAO.get("type").asText().contains("OpenSearch");
    }

    protected static <V extends Serializable> Component findComponentByProp(
            final String property, final String path, final V key) {

        Component component = TESTER.getComponentFromLastRenderedPage(path);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(ListItem.class, (ListItem<?> object, IVisit<Component> visit) -> {
                    try {
                        Method getter = PropertyResolver.getPropertyGetter(property, object.getModelObject());
                        if (getter != null && getter.invoke(object.getModelObject()).equals(key)) {
                            visit.stop(object);
                        }
                    } catch (Exception e) {
                        LOG.debug("Error finding component by property ({},{}) on path {}", property, key, path, e);
                    }
                });
    }

    protected static <V extends Serializable> Component findComponentByPropNotNull(
            final String property, final String path) {

        Component component = TESTER.getComponentFromLastRenderedPage(path);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(ListItem.class, (ListItem<?> object, IVisit<Component> visit) -> {
                    try {
                        Method getter = PropertyResolver.getPropertyGetter(property, object.getModelObject());
                        if (getter != null && getter.invoke(object.getModelObject()) != null) {
                            visit.stop(object);
                        }
                    } catch (Exception e) {
                        LOG.debug("Error finding component by property {} not null on path {}", property, path, e);
                    }
                });
    }

    protected static Component findComponentById(final String searchPath, final String id) {
        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(Component.class, (object, visit) -> {
                    if (object.getId().equals(id)) {
                        visit.stop(object);
                    }
                });
    }

    protected static Component findComponentByMarkupId(final String searchPath, final String markupId) {
        Component component = TESTER.getComponentFromLastRenderedPage(searchPath);
        return (component instanceof MarkupContainer ? MarkupContainer.class.cast(component) : component.getPage()).
                visitChildren(Component.class, (object, visit) -> {
                    if (object.getMarkupId().equals(markupId)) {
                        visit.stop(object);
                    }
                });
    }

    protected static void closeCallBack(final Component modal) {
        modal.getBehaviors().stream().
                filter(AbstractAjaxBehavior.class::isInstance).
                forEachOrdered(behavior -> TESTER.executeBehavior((AbstractAjaxBehavior) behavior));
    }

    protected static void assertSuccessMessage() {
        Set<Serializable> messages = TESTER.getFeedbackMessages(
                new ExactLevelFeedbackMessageFilter(FeedbackMessage.SUCCESS)).stream().
                map(FeedbackMessage::getMessage).collect(Collectors.toSet());
        if (messages.size() != 1) {
            fail(() -> "Expected single message but found " + messages);
        }
        assertEquals("Operation successfully executed", messages.iterator().next());
    }
}
