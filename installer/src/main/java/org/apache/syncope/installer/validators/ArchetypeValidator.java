package org.apache.syncope.installer.validators;

import com.izforge.izpack.api.data.InstallData;

public class ArchetypeValidator extends AbstractValidator {

    private StringBuilder error;

    @Override
    public Status validateData(final InstallData installData) {

        final String mavenGroupId = installData.getVariable("mvn.groupid");
        final String mavenArtifactId = installData.getVariable("mvn.artifactid");
        final String mavenSecretKey = installData.getVariable("mvn.secretkey");
        final String mavenAnonymousKey = installData.getVariable("mvn.anonymous.key");
        final String mavenLogDirectory = installData.getVariable("mvn.log.directory");
        final String mavenBundleDirectory = installData.getVariable("mvn.bundle.directory");

        boolean verified = true;
        error = new StringBuilder("Required fields:\n");
        if (isEmpty(mavenGroupId)) {
            error.append("GroupID\n");
            verified = false;
        }
        if (isEmpty(mavenArtifactId)) {
            error.append("ArtifactID\n");
            verified = false;
        }
        if (isEmpty(mavenSecretKey)) {
            error.append("SecretKey\n");
            verified = false;
        }
        if (isEmpty(mavenAnonymousKey)) {
            error.append("AnonymousKey\n");
            verified = false;
        }
        if (isEmpty(mavenLogDirectory)) {
            error.append("Logs directory\n");
            verified = false;
        }
        if (isEmpty(mavenBundleDirectory)) {
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
