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

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.syncope.installer.utilities.HttpUtils;

public class Tomcat extends AbstractContainer {

    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    private static final String DEPLOY_SYNCOPE_CORE_QUERY = "/manager/text/deploy?path=/syncope&war=file:";

    private static final String DEPLOY_SYNCOPE_CONSOLE_QUERY = "/manager/text/deploy?path=/syncope-console&war=file:";

    private static final String DEPLOY_SYNCOPE_ENDUSER_QUERY = "/manager/text/deploy?path=/syncope-enduser&war=file:";

    private final String installPath;

    private final String artifactId;

    private final HttpUtils httpUtils;

    public Tomcat(final boolean tomcatSsl, final String tomcatHost, final String tomcatPort,
            final String installPath, final String artifactId, final String tomcatUser, final String tomcatPassword,
            final AbstractUIProcessHandler handler) {
        this.installPath = installPath;
        this.artifactId = artifactId;
        httpUtils = new HttpUtils(tomcatSsl, tomcatHost, tomcatPort, tomcatUser, tomcatPassword, handler);
    }

    public boolean deployCore() {
        int status;
        if (IS_WIN) {
            status = httpUtils.getWithBasicAuth(DEPLOY_SYNCOPE_CORE_QUERY
                    + pathEncoded(String.format(WIN_CORE_RELATIVE_PATH, installPath, artifactId)));
        } else {
            status = httpUtils.getWithBasicAuth(path(DEPLOY_SYNCOPE_CORE_QUERY + UNIX_CORE_RELATIVE_PATH));
        }

        return status == 200;
    }

    public boolean deployConsole() {
        int status;
        if (IS_WIN) {
            status = httpUtils.getWithBasicAuth(DEPLOY_SYNCOPE_CONSOLE_QUERY
                    + pathEncoded(String.format(WIN_CONSOLE_RELATIVE_PATH, installPath, artifactId)));
        } else {
            status = httpUtils.getWithBasicAuth(path(DEPLOY_SYNCOPE_CONSOLE_QUERY + UNIX_CONSOLE_RELATIVE_PATH));
        }

        return status == 200;
    }

    public boolean deployEnduser() {
        int status;
        if (IS_WIN) {
            status = httpUtils.getWithBasicAuth(DEPLOY_SYNCOPE_ENDUSER_QUERY
                    + pathEncoded(String.format(WIN_ENDUSER_RELATIVE_PATH, installPath, artifactId)));
        } else {
            status = httpUtils.getWithBasicAuth(path(DEPLOY_SYNCOPE_ENDUSER_QUERY + UNIX_ENDUSER_RELATIVE_PATH));
        }

        return status == 200;
    }

    private String pathEncoded(final String what) {
        String path = "";
        try {
            path = URLEncoder.encode(what, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            // ignore
        }

        return path;
    }

    private String path(final String what) {
        return String.format(what, installPath, artifactId);
    }
}
