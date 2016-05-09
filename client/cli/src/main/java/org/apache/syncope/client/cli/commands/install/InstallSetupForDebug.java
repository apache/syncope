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
package org.apache.syncope.client.cli.commands.install;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.ws.rs.ProcessingException;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.util.FileSystemUtils;
import org.apache.syncope.client.cli.util.JasyptUtils;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallSetupForDebug {

    private static final Logger LOG = LoggerFactory.getLogger(InstallSetupForDebug.class);

    private final InstallResultManager installResultManager = new InstallResultManager();

    public void setup() throws FileNotFoundException, IllegalAccessException {
        installResultManager.printWelcome();

        System.out.println("Path to config files of Syncope CLI client will be: "
                + InstallConfigFileTemplate.dirPath());

        if (!FileSystemUtils.exists(InstallConfigFileTemplate.dirPath())) {
            throw new FileNotFoundException("Directory: " + InstallConfigFileTemplate.dirPath() + " does not exists!");
        }

        if (!FileSystemUtils.canWrite(InstallConfigFileTemplate.dirPath())) {
            throw new IllegalAccessException("Permission denied on " + InstallConfigFileTemplate.dirPath());
        }
        System.out.println("- File system permission checked");
        System.out.println("");

        final JasyptUtils jasyptUtils = JasyptUtils.get();
        try {

            final String contentCliPropertiesFile = InstallConfigFileTemplate.cliPropertiesFile(
                    "http",
                    "localhost",
                    "9080",
                    "/syncope/rest",
                    "admin",
                    jasyptUtils.encrypt("password"));
            FileSystemUtils.createFileWith(InstallConfigFileTemplate.configurationFilePath(), contentCliPropertiesFile);
        } catch (final IOException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            final SyncopeService syncopeService = SyncopeServices.get(SyncopeService.class);
            final String syncopeVersion = syncopeService.platform().getVersion();
            installResultManager.installationSuccessful(syncopeVersion);
        } catch (final ProcessingException ex) {
            LOG.error("Error installing CLI", ex);
            installResultManager.manageProcessingException(ex);
        } catch (final Exception e) {
            LOG.error("Error installing CLI", e);
            installResultManager.manageException(e);
        }
    }
}
