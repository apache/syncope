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
package org.apache.syncope.installer.utilities;

public class Commands {

    public static String createArchetype(final String mavenDir, final String syncopeVersion, final String groupId,
            final String artifactId, final String secretKey, final String anonymousKey) {
        final StringBuilder commandBuilder = new StringBuilder();
        if (IS_WIN) {
            commandBuilder.append("cmd /c ").append(String.format(WIN_MAVEN, mavenDir));
        } else {
            commandBuilder.append(String.format(UNIX_MAVEN, mavenDir));
        }
        return commandBuilder.append(String.format(
                CREATE_ARCHETYPE_COMMAND, syncopeVersion, groupId, artifactId, secretKey, anonymousKey))
                .toString();

    }

    private static final String UNIX_MAVEN = "%s/bin/mvn ";

    private static final String WIN_MAVEN = "%s\\bin\\mvn ";

    private static final String CREATE_ARCHETYPE_COMMAND = "archetype:generate "
            + "-DarchetypeGroupId=org.apache.syncope "
            + "-DarchetypeArtifactId=syncope-archetype "
            + "-DarchetypeRepository=http://repository.apache.org/content/repositories/snapshots "
            + "-DarchetypeVersion=%s "
            + "-DgroupId=%s -DartifactId=%s -DsecretKey=%s -DanonymousKey=%s -DinteractiveMode=false";

    public static String compileArchetype(
            final String mavenDir, final String logsDirectory, final String bundlesDirectory) {
        final StringBuilder commandBuilder = new StringBuilder();
        if (IS_WIN) {
            commandBuilder.append("cmd /c ").append(String.format(WIN_MAVEN, mavenDir));;
        } else {
            commandBuilder.append(String.format(UNIX_MAVEN, mavenDir));;
        }
        return commandBuilder.append(String.format(
                COMPILE_ARCHETYPE, logsDirectory, bundlesDirectory))
                .toString();
    }

    private static final String COMPILE_ARCHETYPE
            = "clean package -Dlog.directory=%s -Dbundles.directory=%s ";

    public static String createDirectory(final String directory) {
        return String.format(CREATE_DIRECTORY, directory);
    }

    private static final String CREATE_DIRECTORY = "mkdir -p %s";

    public static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

}
