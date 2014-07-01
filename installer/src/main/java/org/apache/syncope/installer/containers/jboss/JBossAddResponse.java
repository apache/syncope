package org.apache.syncope.installer.containers.jboss;

public class JBossAddResponse {

    public class JBossBytesValue {

        public String BYTES_VALUE;

        public JBossBytesValue() {
        }

        public JBossBytesValue(String BYTES_VALUE) {
            this.BYTES_VALUE = BYTES_VALUE;
        }

        public String getBYTES_VALUE() {
            return BYTES_VALUE;
        }

        public void setBYTES_VALUE(String BYTES_VALUE) {
            this.BYTES_VALUE = BYTES_VALUE;
        }

    }

    public String outcome;

    public JBossBytesValue result;

    public JBossAddResponse() {
    }

    public JBossAddResponse(String outcome, JBossBytesValue result) {
        this.outcome = outcome;
        this.result = result;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public void setResult(JBossBytesValue result) {
        this.result = result;
    }

    public String getOutcome() {
        return outcome;
    }

    public JBossBytesValue getResult() {
        return result;
    }

}
