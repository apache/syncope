package org.apache.syncope.installer.processes;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.files.Pom;
import org.apache.syncope.installer.utilities.Commands;

public class ArchetypeProcess extends AbstractProcess {

    public void run(final AbstractUIProcessHandler handler, final String[] args) {

        final String installPath = args[0];
        final String groupId = args[1];
        final String artifactId = args[2];
        final String secretKey = args[3];
        final String anonymousKey = args[4];
        final String logsDirectory = args[5];
        final String bundlesDirectory = args[6];
        final String syncopeVersion = args[7];
        final String syncopeAdminPassword = args[8];

        if (!new File(installPath).exists()) {
            exec(String.format(Commands.createDirectory, installPath), handler, null);
        }

        exec(String.format(Commands.createArchetypeCommand,
                syncopeVersion, groupId, artifactId, secretKey, anonymousKey), handler, installPath);
        writeToFile(new File(installPath + "/" + artifactId + Pom.PATH), Pom.FILE);

        exec(String.format(Commands.createDirectory, logsDirectory), handler, null);

        exec(String.format(Commands.createDirectory, bundlesDirectory), handler, null);

        exec(String.format(
                Commands.compileCommand, logsDirectory, bundlesDirectory), handler, installPath + "/" + artifactId);
    }

}
