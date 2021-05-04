/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.init;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Take care of all initializations needed by Syncope Enduser to run up and safe.
 */
@WebListener
public class EnduserInitializer implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(EnduserInitializer.class);

    public static final String CLASSPATH_LOOKUP = "CLASSPATH_LOOKUP";

    public static final String MIMETYPES_LOADER = "MIMETYPES_LOADER";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
        lookup.load();
        sce.getServletContext().setAttribute(CLASSPATH_LOOKUP, lookup);

        MIMETypesLoader mimeTypes = new MIMETypesLoader();
        mimeTypes.load();
        sce.getServletContext().setAttribute(MIMETYPES_LOADER, mimeTypes);

        LOG.debug("Initialization completed");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        // nothing to do
    }

}
