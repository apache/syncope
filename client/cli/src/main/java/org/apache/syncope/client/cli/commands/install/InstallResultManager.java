package org.apache.syncope.client.cli.commands.install;

import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.ws.rs.ProcessingException;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.commands.CommonsResultManager;

public class InstallResultManager extends CommonsResultManager {

    public void installationSuccessful(final String version) {
        System.out.println("Installation parameters checked on Syncope core version: " + version);
        System.out.println("");
        System.out.println("###############################################");
        System.out.println("#                                             #");
        System.out.println("#           Installation successful           #");
        System.out.println("#     now you can use Syncope CLI client      #");
        System.out.println("#                                             #");
        System.out.println("###############################################");
        System.out.println("");
    }

    public void manageProcessingException(final ProcessingException ex) {
        if (ex.getCause() instanceof UnknownHostException) {
            final String unknownHost = ex.getCause().getMessage().split(":")[3];
            System.out.println("");
            System.out.println("Provided host:" + unknownHost);
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#            Provided unknown host!           #");
            System.out.println("#        START AGAIN the installation!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        } else if (ex.getCause() instanceof ConnectException) {
            System.out.println("");
            System.out.println("Provided address :" + SyncopeServices.getAddress());
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#       Provided address is unreachable!      #");
            System.out.println("#         Check it and if it is wrong         #");
            System.out.println("#        START AGAIN the installation!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        }
    }

    public void manageException(final Exception e) {
        if (e.getMessage().contains("not authenticated")) {
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#   Username or password provided are wrong   #");
            System.out.println("#        START AGAIN the installation!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        } else {
            System.out.println("");
            System.out.println("###############################################");
            System.out.println("#                                             #");
            System.out.println("#                Something wrong              #");
            System.out.println("#        START AGAIN the installation!        #");
            System.out.println("#                                             #");
            System.out.println("###############################################");
            System.out.println("");
        }
    }
}
