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
package org.apache.syncope.client.console.wizards.any;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public final class UserFormFinalizerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UserFormFinalizerUtils.class);

    private static UserFormFinalizerUtils INSTANCE;

    public static UserFormFinalizerUtils getInstance() {
        synchronized (LOG) {
            if (INSTANCE == null) {
                INSTANCE = new UserFormFinalizerUtils();
            }
        }
        return INSTANCE;
    }

    private final ClassPathScanImplementationLookup classPathScanImplementationLookup;

    private UserFormFinalizerUtils() {
        classPathScanImplementationLookup = (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);
    }

    public List<UserFormFinalizer> getFormFinalizers(final AjaxWizard.Mode mode) {
        List<UserFormFinalizer> finalizers = new ArrayList<>();

        classPathScanImplementationLookup.getUserFormFinalizerClasses(mode).forEach(applier -> {
            if (applier != null) {
                try {
                    finalizers.add(ClassUtils.getConstructorIfAvailable(applier).newInstance());
                } catch (Exception e) {
                    LOG.error("Could not instantiate {}", applier, e);
                }
            }
        });

        return finalizers;
    }
}
