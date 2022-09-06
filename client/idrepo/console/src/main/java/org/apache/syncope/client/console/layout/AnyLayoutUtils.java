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
package org.apache.syncope.client.console.layout;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AnyPanel;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.ui.commons.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.ui.commons.wizards.any.AnyForm;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.springframework.util.ClassUtils;

public final class AnyLayoutUtils {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static void setUserIfEmpty(final AnyLayout anyLayout) {
        if (anyLayout.getUser() == null) {
            anyLayout.setUser(new UserFormLayoutInfo());
        }
    }

    private static void setGroupIfEmpty(final AnyLayout anyLayout) {
        if (anyLayout.getGroup() == null) {
            anyLayout.setGroup(new GroupFormLayoutInfo());
        }
    }

    private static void setAnyObjectsIfEmpty(final AnyLayout anyLayout, final List<String> anyTypes) {
        if (anyLayout.getAnyObjects().isEmpty()) {
            anyLayout.getAnyObjects().putAll(anyTypes.stream().filter(
                    anyType -> !anyType.equals(AnyTypeKind.USER.name()) && !anyType.equals(AnyTypeKind.GROUP.name())).
                    collect(Collectors.toMap(Function.identity(), anyType -> new AnyObjectFormLayoutInfo())));
        }
    }

    private static AnyLayout empty(final List<String> anyTypes) {
        AnyLayout anyLayout = new AnyLayout();
        setUserIfEmpty(anyLayout);
        setGroupIfEmpty(anyLayout);
        setAnyObjectsIfEmpty(anyLayout, anyTypes);
        return anyLayout;
    }

    public static AnyLayout fetch(final List<String> anyTypes) {
        List<String> ownedRoles = Stream.concat(
                SyncopeConsoleSession.get().getSelfTO().getRoles().stream(),
                SyncopeConsoleSession.get().getSelfTO().getDynRoles().stream()).
                distinct().collect(Collectors.toList());
        try {
            AnyLayout anyLayout = null;
            for (int i = 0; i < ownedRoles.size() && anyLayout == null; i++) {
                String anyLayoutJSON = RoleRestClient.readAnyLayout(ownedRoles.get(i));
                if (StringUtils.isNotBlank(anyLayoutJSON)) {
                    anyLayout = MAPPER.readValue(anyLayoutJSON, AnyLayout.class);
                }
            }

            if (anyLayout == null) {
                anyLayout = empty(anyTypes);
            }
            setUserIfEmpty(anyLayout);
            setGroupIfEmpty(anyLayout);
            setAnyObjectsIfEmpty(anyLayout, anyTypes);

            return anyLayout;
        } catch (IOException e) {
            throw new IllegalArgumentException("While parsing console layout for "
                    + SyncopeConsoleSession.get().getSelfTO().getUsername(), e);
        }
    }

    public static String defaultIfEmpty(final String content, final List<String> anyTypes) {
        String result;

        if (StringUtils.isBlank(content)) {
            AnyLayout anyLayout = empty(anyTypes);

            try {
                result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(anyLayout);
            } catch (IOException e) {
                throw new IllegalArgumentException("While generating default console layout for "
                        + SyncopeConsoleSession.get().getSelfTO().getUsername(), e);
            }
        } else {
            try {
                result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(content));
            } catch (IOException e) {
                result = content;
            }
        }

        return result;
    }

    public static <A extends AnyTO, F extends AnyForm<A>, FL extends AbstractAnyFormLayout<A, F>> F newLayoutInfo(
            final A anyTO,
            final List<String> anyTypeClasses,
            final FL anyFormLayout,
            final PageReference pageRef) {

        try {
            if (anyTO instanceof UserTO) {
                return anyFormLayout.getFormClass().getConstructor(
                        anyTO.getClass(), // previous
                        anyTO.getClass(), // actual
                        List.class,
                        anyFormLayout.getClass(),
                        pageRef.getClass()).
                        newInstance(null, anyTO, anyTypeClasses, anyFormLayout, pageRef);
            } else {
                return anyFormLayout.getFormClass().getConstructor(
                        anyTO.getClass(), // actual
                        List.class,
                        anyFormLayout.getClass(),
                        pageRef.getClass()).
                        newInstance(anyTO, anyTypeClasses, anyFormLayout, pageRef);
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException("Could not instantiate " + anyFormLayout.getFormClass().getName(), e);
        }
    }

    public static <AP extends AnyPanel> AP newAnyPanel(
            final String panelClass,
            final String id,
            final AnyTypeTO anyTypeTO,
            final RealmTO realmTO,
            final AnyLayout anyLayout,
            final boolean enableSearch,
            final PageReference pageRef) {

        try {
            @SuppressWarnings("unchecked")
            Class<AP> clazz = (Class<AP>) ClassUtils.forName(panelClass, ClassUtils.getDefaultClassLoader());
            return clazz.getConstructor(
                    String.class,
                    AnyTypeTO.class,
                    RealmTO.class,
                    AnyLayout.class,
                    boolean.class,
                    PageReference.class).
                    newInstance(id, anyTypeTO, realmTO, anyLayout, enableSearch, pageRef);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not instantiate " + panelClass, e);
        }
    }

    public static <AP extends AnyPanel> AP newAnyPanel(
            final String panelClass,
            final String id,
            final AnyTypeTO anyTypeTO,
            final RealmTO realmTO,
            final AnyLayout anyLayout,
            final boolean enableSearch,
            final AnyPanel.DirectoryPanelSupplier directoryPanelSupplier,
            final PageReference pageRef) {

        try {
            @SuppressWarnings("unchecked")
            Class<AP> clazz = (Class<AP>) ClassUtils.forName(panelClass, ClassUtils.getDefaultClassLoader());
            return clazz.getConstructor(
                    String.class,
                    AnyTypeTO.class,
                    RealmTO.class,
                    AnyLayout.class,
                    boolean.class,
                    AnyPanel.DirectoryPanelSupplier.class,
                    PageReference.class).
                    newInstance(id, anyTypeTO, realmTO, anyLayout, enableSearch, directoryPanelSupplier, pageRef);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not instantiate " + panelClass, e);
        }
    }

    private AnyLayoutUtils() {
        // private constructor for static utility class
    }
}
