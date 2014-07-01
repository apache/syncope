package org.apache.syncope.installer.utilities;

public class Commands {
            
    public static final String createArchetypeCommand
            = "mvn archetype:generate "
            + "-DarchetypeGroupId=org.apache.syncope "
            + "-DarchetypeArtifactId=syncope-archetype "
            + "-DarchetypeRepository=http://repo1.maven.org/maven2 "
            + "-DarchetypeVersion=%s "
            + "-DgroupId=%s -DartifactId=%s -DsecretKey=%s -DanonymousKey=%s -DinteractiveMode=false";

    public static final String compileCommand = "mvn clean package -Dlog.directory=%s -Dbundles.directory=%s ";

    public static final String createDirectory = "mkdir -p %s";

}
