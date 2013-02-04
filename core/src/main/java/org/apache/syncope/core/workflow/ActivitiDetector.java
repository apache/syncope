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
package org.apache.syncope.core.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.dao.impl.ContentLoader;
import org.apache.syncope.core.workflow.user.activiti.ActivitiUserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivitiDetector {

    private static final Logger LOG = LoggerFactory.getLogger(ActivitiDetector.class);

    private static final String XX = "/workflow.properties";

    private static String uwfAdapterClassName;

    private static String rwfAdapterClassName;

    private static String activitiVersion;

    static {
        try {
            init();
        } catch (IOException e) {
            LOG.error("Could not read from {}", XX, e);
        }
    }

    /**
     * Read classpath:/workflow.properties in order to determine the configured workflow adapter class name.
     *
     * @throws IOException if anything goes wrong
     */
    private static void init() throws IOException {
        Properties props = new java.util.Properties();
        InputStream propStream = null;
        try {
            propStream = ContentLoader.class.getResourceAsStream(XX);
            props.load(propStream);
            uwfAdapterClassName = props.getProperty("uwfAdapter");
            rwfAdapterClassName = props.getProperty("rwfAdapter");
            activitiVersion = props.getProperty("activitiVersion");
        } catch (Exception e) {
            LOG.error("Could not load workflow.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
    }

    /**
     * Check if the configured user workflow adapter is Activiti's.
     *
     * @return whether Activiti is configured for user workflow or not
     */
    public static boolean isActivitiEnabledForUsers() {
        return uwfAdapterClassName != null && uwfAdapterClassName.equals(ActivitiUserWorkflowAdapter.class.getName());
    }

    /**
     * Check if the configured role workflow adapter is Activiti's.
     *
     * @return whether Activiti is configured for role workflow or not
     */
    public static boolean isActivitiEnabledForRoles() {
        // ActivitiRoleWorkflowAdapter hasn't been developed (yet) as part of SYNCOPE-173 
        //return rwfAdapterClassName != null && rwfAdapterClassName.equals(ActivitiRoleWorkflowAdapter.class.getName());
        return false;
    }

    /**
     * @return the version of Activiti packages, as configured in Maven
     */
    public static String getActivitiVersion() {
        return activitiVersion;
    }
}
