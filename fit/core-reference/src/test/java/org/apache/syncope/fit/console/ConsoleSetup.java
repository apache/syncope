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

import javax.servlet.ServletContext;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.wicket.util.tester.WicketTester;

public final class ConsoleSetup {

    public static final WicketTester TESTER = new WicketTester(new SyncopeConsoleApplication() {

        @Override
        protected void init() {
            ServletContext ctx = getServletContext();
            ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
            lookup.load();
            ctx.setAttribute(ConsoleInitializer.CLASSPATH_LOOKUP, lookup);

            MIMETypesLoader mimeTypes = new MIMETypesLoader();
            mimeTypes.load();
            ctx.setAttribute(ConsoleInitializer.MIMETYPES_LOADER, mimeTypes);

            super.init();
        }
    });

}
