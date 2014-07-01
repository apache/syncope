package org.apache.syncope.installer.containers.jboss;

import org.apache.syncope.installer.utilities.HttpUtils;
import org.apache.syncope.installer.utilities.JsonUtils;

public class JBoss {

    private final String core = "%s/%s/core/target/syncope.war";

    private final String console = "%s/%s/console/target/syncope-console.war";

    private final String addContentUrl = "http://%s:%s/management/add-content";

    private final String enableUrl = "http://%s:%s/management/";

    private final String jbossHost;

    private final String jbossPort;

    private final String installPath;

    private final String artifactId;

    private final HttpUtils httpUtils;

    public JBoss(final String jbossHost, final String jbossPort,
            final String jbossAdminUsername, final String jbossAdminPassword,
            final String installPath, final String artifactId) {
        this.jbossHost = jbossHost;
        this.jbossPort = jbossPort;
        this.installPath = installPath;
        this.artifactId = artifactId;
        httpUtils = new HttpUtils(jbossAdminUsername, jbossAdminPassword);

    }

    public boolean deployCore() {
        return deploy(core, "syncope.war");
    }

    public boolean deployConsole() {
        return deploy(console, "syncope-console.war");
    }

    public boolean deploy(final String whatDeploy, final String warName) {
        final String responseBodyAsString = httpUtils.postWithDigestAuth(
                String.format(addContentUrl, jbossHost, jbossPort),
                String.format(whatDeploy, installPath, artifactId));

        final JBossAddResponse jBossAddResponse = JsonUtils.jBossAddResponse(responseBodyAsString);

        final JBossDeployRequestContent jBossDeployRequestContent = new JBossDeployRequestContent(
                jBossAddResponse.getResult().getBYTES_VALUE(), warName);

        int status = httpUtils.postWithStringEntity(String.format(enableUrl, jbossHost, jbossPort),
                JsonUtils.jBossDeployRequestContent(jBossDeployRequestContent));
        return status == 200;
    }
}
