package org.apache.syncope.installer.enums;

public enum Containers {

    TOMCAT("tomcat"),
    JBOSS("jboss"),
    GLASSFISH("glassfish");

    private Containers(final String name) {
        this.name = name;
    }

    private final String name;

    public String getName() {
        return name;
    }

    public static Containers fromContainerName(final String containerName) {
        Containers container = null;
        if (TOMCAT.getName().equalsIgnoreCase(containerName)) {
            container = TOMCAT;
        } else if (JBOSS.getName().equalsIgnoreCase(containerName)) {
            container = JBOSS;
        } else if (GLASSFISH.getName().equalsIgnoreCase(containerName)) {
            container = GLASSFISH;
        }
        return container;
    }

}
