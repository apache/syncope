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
package org.apache.syncope.core;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static String connidSoapVersion;

    protected static String bundlesDirectory;

    @BeforeClass
    public static void setUpIdentityConnectorsBundles() throws IOException {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = AbstractTest.class.getResourceAsStream("/bundles.properties");
            props.load(propStream);
            connidSoapVersion = props.getProperty("connid.soap.version");
            bundlesDirectory = props.getProperty("bundles.directory");
        } catch (Exception e) {
            LOG.error("Could not load bundles.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
        assertNotNull(connidSoapVersion);
        assertNotNull(bundlesDirectory);
    }
}
