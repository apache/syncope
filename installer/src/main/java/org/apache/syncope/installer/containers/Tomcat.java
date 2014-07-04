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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.syncope.installer.utilities.HttpUtils;

public class Tomcat {

    private static final String UNIX_DEPLOY_SYNCOPE_CORE_QUERY
            = "/manager/text/deploy?path=/syncope&war=file:%s/%s/core/target/syncope.war";

    private static final String WIN_DEPLOY_SYNCOPE_CORE_QUERY
            = "/manager/text/deploy?path=/syncope&war=file:%s\\%s\\core\\target\\syncope.war";

    private static final String UNIX_DEPLOY_SYNCOPE_CONSOLE_QUERY
            = "/manager/text/deploy?path=/syncope-console&war=file:%s/%s/console/target/syncope-console.war";

    private static final String WIN_DEPLOY_SYNCOPE_CONSOLE_QUERY
            = "/manager/text/deploy?path=/syncope-console&war=file:%s\\%s\\console\\target\\syncope-console.war";

    private final String installPath;

    private final String artifactId;

    private final HttpUtils httpUtils;

    private final boolean isWin;

    public Tomcat(final boolean tomcatSsl, final String tomcatHost, final String tomcatPort,
            final String installPath, final String artifactId, final String tomcatUser, final String tomcatPassword) {
        this.installPath = installPath;
        this.artifactId = artifactId;
        isWin = System.getProperty("os.name").toLowerCase().contains("win");
        httpUtils = new HttpUtils(tomcatSsl, tomcatHost, tomcatPort, tomcatUser, tomcatPassword);
    }

    public boolean deployCore() {
        int status;
        if (isWin) {
            status = httpUtils.getWithBasicAuth(pathEncoded(WIN_DEPLOY_SYNCOPE_CORE_QUERY));
        } else {
            status = httpUtils.getWithBasicAuth(path(UNIX_DEPLOY_SYNCOPE_CORE_QUERY));
        }

        return status == 200;
    }

    public boolean deployConsole() {
        int status;
        if (isWin) {
            status = httpUtils.getWithBasicAuth(pathEncoded(WIN_DEPLOY_SYNCOPE_CONSOLE_QUERY));
        } else {
            status = httpUtils.getWithBasicAuth(path(UNIX_DEPLOY_SYNCOPE_CONSOLE_QUERY));
        }

        return status == 200;
    }

    public String pathEncoded(final String what) {
        String path = "";
        try {
            path = URLEncoder.encode(String.format(what, installPath, artifactId), "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        return path;
    }

    public String path(final String what) {
        return String.format(what, installPath, artifactId);
    }
}
