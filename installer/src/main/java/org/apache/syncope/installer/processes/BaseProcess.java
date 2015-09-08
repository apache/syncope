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
package org.apache.syncope.installer.processes;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

public abstract class BaseProcess {

    protected static final Properties PROPERTIES = new Properties();

    protected String syncopeInstallDir;

    static {
        InputStream input = null;
        try {
            input = BaseProcess.class.getResourceAsStream("/installer.properties");
            PROPERTIES.load(input);
        } catch (IOException e) {
            // ignore
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    protected void setSyncopeInstallDir(final String installPath, final String artifactId) {
        syncopeInstallDir = new StringBuilder().
                append(installPath).
                append("/").
                append(artifactId).
                append("/").toString();
    }

    public abstract void run(AbstractUIProcessHandler handler, String[] args);

}
