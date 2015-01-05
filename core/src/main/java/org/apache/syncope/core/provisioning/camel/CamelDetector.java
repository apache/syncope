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
package org.apache.syncope.core.provisioning.camel;

import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.dao.impl.ContentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author giacomolm
 */
public class CamelDetector {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelDetector.class);

    private static final String PROVISIONING_PROPERTIES = "/provisioning.properties";
     
    private static String userPMClassName;

    private static String rolePMClassName;
    
     /**
     * Read classpath:/provisioning.properties in order to determine the configured provisioning adapter class name.
     */
    static {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = ContentLoader.class.getResourceAsStream(PROVISIONING_PROPERTIES);
            props.load(propStream);
            userPMClassName = props.getProperty("userProvisioningManager");
            rolePMClassName = props.getProperty("roleProvisioningManager");
        } catch (Exception e) {
            LOG.error("Could not load workflow.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
    }
    
    /**
     * Check if the configured user provisioning manager is Camel's.
     *
     * @return whether Activiti is configured for user workflow or not
     */
    public static boolean isCamelEnabledForUsers() {
        return userPMClassName != null && userPMClassName.equals(CamelUserProvisioningManager.class.getName());
    }

    /**
     * Check if the configured role provisioning manager is Camel's.
     *
     * @return whether Activiti is configured for role workflow or not
     */
    public static boolean isCamelEnabledForRoles() {
        return rolePMClassName != null && rolePMClassName.equals(CamelRoleProvisioningManager.class.getName());
    }
}
