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

import org.apache.syncope.installer.utilities.FileSystemUtils;
import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.enums.DBs;
import org.apache.syncope.installer.files.MasterProperties;
import org.apache.syncope.installer.files.ProvisioningProperties;
import org.apache.syncope.installer.utilities.InstallLog;

public class PersistenceProcess extends BaseProcess {

    private String installPath;

    private String artifactId;

    private DBs dbSelected;

    private String persistenceUrl;

    private String persistenceUser;

    private String persistencePassword;

    private boolean mysqlInnoDB;

    private String schema;

    @Override
    public void run(final AbstractUIProcessHandler handler, final String[] args) {
        installPath = args[0];
        artifactId = args[1];
        dbSelected = DBs.fromDbName(args[2]);
        persistenceUrl = args[3];
        persistenceUser = args[4];
        persistencePassword = args[5];
        mysqlInnoDB = Boolean.valueOf(args[6]);
        schema = args[7];

        final StringBuilder masterProperties = new StringBuilder(MasterProperties.HEADER);
        setSyncopeInstallDir(installPath, artifactId);

        final FileSystemUtils fileSystemUtils = new FileSystemUtils(handler);
        final File provisioningFile = new File(
                syncopeInstallDir + PROPERTIES.getProperty("provisioningPropertiesFile"));

        final String provisioningFileString = fileSystemUtils.readFile(provisioningFile);
        final StringBuilder provisioningProperties = new StringBuilder(
                provisioningFileString.substring(0, provisioningFileString.indexOf("quartz.jobstore")));
        handler.logOutput("Configure persistence for " + dbSelected, false);
        InstallLog.getInstance().info("Configure persistence for " + dbSelected);

        switch (dbSelected) {
            case POSTGRES:
                provisioningProperties.append(ProvisioningProperties.POSTGRES);
                masterProperties.append(String.format(
                        MasterProperties.POSTGRES, persistenceUrl, persistenceUser, persistencePassword));
                break;

            case MYSQL:
                provisioningProperties.append(ProvisioningProperties.MYSQL);
                provisioningProperties.append(mysqlInnoDB
                        ? ProvisioningProperties.MYSQL_QUARTZ_INNO_DB
                        : ProvisioningProperties.MYSQL_QUARTZ);
                masterProperties.append(String.format(
                        MasterProperties.MYSQL, persistenceUrl, persistenceUser, persistencePassword));
                break;

            case MARIADB:
                provisioningProperties.append(ProvisioningProperties.MARIADB);
                masterProperties.append(String.format(
                        MasterProperties.MARIADB, persistenceUrl, persistenceUser, persistencePassword));
                break;

            case ORACLE:
                provisioningProperties.append(ProvisioningProperties.ORACLE);
                masterProperties.append(String.format(
                        MasterProperties.ORACLE, persistenceUrl, schema, persistenceUser, persistencePassword));
                break;

            case SQLSERVER:
                provisioningProperties.append(ProvisioningProperties.SQLSERVER);
                masterProperties.append(String.format(
                        MasterProperties.SQLSERVER, persistenceUrl, schema, persistenceUser, persistencePassword));
                break;

            default:
        }

        fileSystemUtils.writeToFile(new File(
                syncopeInstallDir + PROPERTIES.getProperty("provisioningPropertiesFile")),
                provisioningProperties.toString());
        fileSystemUtils.writeToFile(new File(
                syncopeInstallDir + PROPERTIES.getProperty("masterPropertiesFile")),
                masterProperties.toString());
    }
}
