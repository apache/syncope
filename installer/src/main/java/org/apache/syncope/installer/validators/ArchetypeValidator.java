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
package org.apache.syncope.installer.validators;

import com.izforge.izpack.api.data.InstallData;
import java.io.File;
import org.apache.commons.lang3.StringUtils;

public class ArchetypeValidator extends AbstractValidator {

    private StringBuilder error;

    @Override
    public Status validateData(final InstallData installData) {
        final String mavenDir = StringUtils.trim(installData.getVariable("mvn.directory"));
        final String mavenGroupId = StringUtils.trim(installData.getVariable("mvn.groupid"));
        final String mavenArtifactId = StringUtils.trim(installData.getVariable("mvn.artifactid"));
        final String mavenSecretKey = StringUtils.trim(installData.getVariable("mvn.secretkey"));
        final String mavenAnonymousKey = StringUtils.trim(installData.getVariable("mvn.anonymous.key"));
        final String mavenLogDirectory = StringUtils.trim(installData.getVariable("mvn.log.directory"));
        final String mavenBundleDirectory = StringUtils.trim(installData.getVariable("mvn.bundle.directory"));

        boolean verified = true;
        error = new StringBuilder("Required fields:\n");
        if (StringUtils.isBlank(mavenDir)) {
            error.append("Maven home directory\n");
            verified = false;
        } else if (!new File(mavenDir + "/bin/mvn").exists()) {
            error.append("Maven home directory not valid, check it please...\n");
            verified = false;
        }
        if (StringUtils.isBlank(mavenGroupId)) {
            error.append("GroupID\n");
            verified = false;
        }
        if (StringUtils.isBlank(mavenArtifactId)) {
            error.append("ArtifactID\n");
            verified = false;
        }
        if (StringUtils.isBlank(mavenSecretKey)) {
            error.append("SecretKey\n");
            verified = false;
        }
        if (StringUtils.isBlank(mavenAnonymousKey)) {
            error.append("AnonymousKey\n");
            verified = false;
        }
        if (StringUtils.isBlank(mavenLogDirectory)) {
            error.append("Logs directory\n");
            verified = false;
        }
        if (StringUtils.isBlank(mavenBundleDirectory)) {
            error.append("Bundles directory\n");
            verified = false;
        }

        return verified ? Status.OK : Status.ERROR;
    }

    @Override
    public String getErrorMessageId() {
        return error.toString();
    }

    @Override
    public String getWarningMessageId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getDefaultAnswer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
