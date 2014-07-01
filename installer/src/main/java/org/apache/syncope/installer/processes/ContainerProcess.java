package org.apache.syncope.installer.processes;


import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.containers.Glassfish;
import org.apache.syncope.installer.containers.Tomcat;
import org.apache.syncope.installer.containers.jboss.JBoss;
import org.apache.syncope.installer.enums.Containers;
import org.apache.syncope.installer.files.GlassfishWebXml;
import org.apache.syncope.installer.files.JBossDeploymentStructureXml;
import org.apache.syncope.installer.files.PersistenceContextEMFactoryXml;
import org.apache.syncope.installer.files.WebXml;
import org.apache.syncope.installer.utilities.Commands;

public class ContainerProcess extends AbstractProcess {

    private String installPath;

    private String artifactId;

    private String tomcatUser;

    private String tomcatPassword;

    private String tomcatUrl;

    private String glassfishDir;

    private String logsDirectory;

    private String bundlesDirectory;

    private boolean withDataSource;

    private String jbossHost;

    private String jbossPort;

    private String jbossJdbcModuleName;

    private String jbossAdminUsername;

    private String jbossAdminPassword;

    public void run(final AbstractUIProcessHandler handler, final String[] args) {

        installPath = args[0];
        artifactId = args[1];
        final Containers selectedContainer = Containers.fromContainerName(args[2]);
        tomcatUrl = args[3];
        tomcatUser = args[4];
        tomcatPassword = args[5];
        glassfishDir = args[6];
        logsDirectory = args[7];
        bundlesDirectory = args[8];
        withDataSource = Boolean.valueOf(args[9]);
        jbossHost = args[10];
        jbossPort = args[11];
        jbossJdbcModuleName = args[12];
        jbossAdminUsername = args[13];
        jbossAdminPassword = args[14];

        if (withDataSource) {
            writeToFile(new File(installPath + "/" + artifactId + WebXml.PATH), WebXml.withDataSource());
            switch (selectedContainer) {
                case JBOSS:
                    writeToFile(new File(installPath + "/" + artifactId + WebXml.PATH), WebXml.withDataSourceForJBoss());
                    writeToFile(new File(installPath + "/" + artifactId + PersistenceContextEMFactoryXml.PATH),
                            PersistenceContextEMFactoryXml.FILE);
                    writeToFile(new File(installPath + "/" + artifactId + JBossDeploymentStructureXml.PATH),
                            String.format(JBossDeploymentStructureXml.FILE, jbossJdbcModuleName));
                    break;
                case GLASSFISH:
                    writeToFile(new File(installPath + "/" + artifactId + GlassfishWebXml.PATH),
                            GlassfishWebXml.withDataSource());
                    break;
            }
        }

        exec(String.format(
                Commands.compileCommand, logsDirectory, bundlesDirectory), handler, installPath + "/" + artifactId);

        switch (selectedContainer) {
            case TOMCAT:
                final Tomcat tomcat = new Tomcat(tomcatUrl, installPath, artifactId, tomcatUser, tomcatPassword);
                
                boolean deployCoreResult = tomcat.deployCore();
                if (deployCoreResult) {
                    handler.logOutput("Core successfully deployed ", true);
                } else {
                    handler.emitError("Deploy core on Tomcat failed", "Deploy core on Tomcat failed");
                }

                boolean deployConsoleResult = tomcat.deployConsole();
                if (deployConsoleResult) {
                    handler.logOutput("Console successfully deployed ", true);
                } else {
                    handler.emitError("Deploy console on Tomcat failed", "Deploy console on Tomcat failed");
                }
                break;
            case JBOSS:
                final JBoss jBoss = new JBoss(
                        jbossHost, jbossPort, jbossAdminUsername, jbossAdminPassword, installPath, artifactId);
                
                boolean deployCoreJboss = jBoss.deployCore();
                if (deployCoreJboss) {
                    handler.logOutput("Core successfully deployed ", true);
                } else {
                    handler.emitError("Deploy core on JBoss failed", "Deploy core on JBoss failed");
                }
                
                boolean deployConsoleJBoss = jBoss.deployConsole();
                if (deployConsoleJBoss) {
                    handler.logOutput("Console successfully deployed ", true);
                } else {
                    handler.emitError("Deploy console on JBoss failed", "Deploy console on JBoss failed");
                }
                break;
            case GLASSFISH:
                final String createJavaOptCommand = "sh " + glassfishDir + Glassfish.CREATE_JAVA_OPT_COMMAND;
                exec(createJavaOptCommand, handler, null);
                exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND
                        + String.format(Glassfish.deploySyncopeCore, installPath, artifactId), handler, null);
                exec("sh " + glassfishDir + Glassfish.DEPLOY_COMMAND
                        + String.format(Glassfish.deploySyncopeConsole, installPath, artifactId), handler, null);
                break;
        }
    }

}
