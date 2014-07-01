package org.apache.syncope.installer.containers.jboss;

public class JBossDeployRequestContent {

    public final JBossDeployRequest[] content = new JBossDeployRequest[1];

    public final JBossDeployment[] address = new JBossDeployment[1];

    public final String operation = "add";

    public final String enabled = "true";

    public JBossDeployRequestContent(final String hash, final String address) {
        this.content[0] = new JBossDeployRequest(hash);
        this.address[0] = new JBossDeployment(address);
    }

    public class JBossDeployRequest {

        public JBossBytesValue hash;

        public JBossBytesValue getHash() {
            return hash;
        }

        public JBossDeployRequest(final String hash) {
            this.hash = new JBossBytesValue(hash);
        }
    }

    public class JBossBytesValue {

        public String BYTES_VALUE;

        public JBossBytesValue(final String BYTES_VALUE) {
            this.BYTES_VALUE = BYTES_VALUE;
        }
    }

    public class JBossDeployment {

        public String deployment;

        public JBossDeployment(String deployment) {
            this.deployment = deployment;
        }

        public String getDeployment() {
            return deployment;
        }
    }
}
