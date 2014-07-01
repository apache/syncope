package org.apache.syncope.installer.containers;

import org.apache.syncope.installer.utilities.HttpUtils;

public class Tomcat {

    private static final String DEPLOY_SYNCOPE_CORE_QUERY
            = "%s/manager/text/deploy?path=/syncope&war=file:%s/%s/core/target/syncope.war";

    private static final String DEPLOY_SYNCOPE_CONSOLE_QUERY
            = "%s/manager/text/deploy?path=/syncope-console&war=file:%s/%s/console/target/syncope-console.war";

    private final String tomcatUrl;

    private final String installPath;

    private final String artifactId;

    private final HttpUtils httpUtils;

    public Tomcat(String tomcatUrl, String installPath, String artifactId, String tomcatUser, String tomcatPassword) {
        this.tomcatUrl = tomcatUrl;
        this.installPath = installPath;
        this.artifactId = artifactId;
        httpUtils = new HttpUtils(tomcatUser, tomcatPassword);
    }

    public boolean deployCore() {
        int status = httpUtils.getWithBasicAuth(
                String.format(DEPLOY_SYNCOPE_CORE_QUERY, tomcatUrl, installPath, artifactId));
        return status == 200;
    }

    public boolean deployConsole() {
        int status = httpUtils.getWithBasicAuth(
                String.format(DEPLOY_SYNCOPE_CONSOLE_QUERY, tomcatUrl, installPath, artifactId));
        return status == 200;
    }
}
