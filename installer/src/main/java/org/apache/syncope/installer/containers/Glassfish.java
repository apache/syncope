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
package org.apache.syncope.installer.containers;

public class Glassfish extends AbstractContainer {

    private final String installPath;

    private final String artifactId;

    public Glassfish(final String installPath, final String artifactId) {
        this.installPath = installPath;
        this.artifactId = artifactId;
    }

    public String deployCore() {
        return deploy(UNIX_CORE_RELATIVE_PATH);
    }

    public String deployConsole() {
        return deploy(UNIX_CONSOLE_RELATIVE_PATH);
    }

    public String deployEnduser() {
        return deploy(UNIX_CONSOLE_RELATIVE_PATH);
    }

    public String deploy(final String what) {
        return String.format(what, installPath, artifactId);
    }

    public static final String CREATE_JAVA_OPT_COMMAND = "/bin/asadmin create-jvm-options"
            + "-Dcom.sun.enterprise.overrideablejavaxpackages=javax.ws.rs,javax.ws.rs.core,javax.ws.rs.ext";

    public static final String DEPLOY_COMMAND = "/bin/asadmin deploy ";

}
