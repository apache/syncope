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
package org.apache.syncope.client.console.commons;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.wizards.any.Applier;
import org.springframework.util.ClassUtils;

public final class ApplierUtils {

    public static ApplierUtils getInstance() {
        return new ApplierUtils();
    }

    private final ClassPathScanImplementationLookup classPathScanImplementationLookup;

    private ApplierUtils() {
        classPathScanImplementationLookup = (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);
    }
    
    public Applier getApplier(final String mode) {
        if (StringUtils.isBlank(mode)) {
            return null;
        }

        Class<? extends Applier> applier = classPathScanImplementationLookup.getApplyerClass(mode);
        try {
            return applier == null
                    ? null
                    : ClassUtils.getConstructorIfAvailable(applier).
                    newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
