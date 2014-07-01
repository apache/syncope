package org.apache.syncope.installer.utilities;

import java.io.IOException;
import org.apache.syncope.installer.containers.jboss.JBossAddResponse;
import org.apache.syncope.installer.containers.jboss.JBossDeployRequestContent;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonUtils {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static JBossAddResponse jBossAddResponse(final String responseBodyAsString) {
        JBossAddResponse jBossAddResponse = null;
        try {
            jBossAddResponse = objectMapper.readValue(responseBodyAsString, JBossAddResponse.class);
        } catch (IOException ioe) {

        }
        return jBossAddResponse;
    }

    public static String jBossDeployRequestContent(final JBossDeployRequestContent jBossDeployRequestContent) {
        String jBossDeployRequestContentString = "";
        try {
            jBossDeployRequestContentString = objectMapper.writeValueAsString(jBossDeployRequestContent);
        } catch (IOException ioe) {

        }
        return jBossDeployRequestContentString;
    }
}
