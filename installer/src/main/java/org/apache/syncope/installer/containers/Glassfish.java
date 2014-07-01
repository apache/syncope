package org.apache.syncope.installer.containers;

public class Glassfish {

    public static final String deploySyncopeCore
            = "%s/%s/core/target/syncope.war";

    public static final String deploySyncopeConsole
            = "%s/%s/console/target/syncope-console.war";

    public static final String CREATE_JAVA_OPT_COMMAND = "/bin/asadmin create-jvm-options"
            + "-Dcom.sun.enterprise.overrideablejavaxpackages=javax.ws.rs,javax.ws.rs.core,javax.ws.rs.ext";

    public static final String DEPLOY_COMMAND = "/bin/asadmin deploy ";

}
