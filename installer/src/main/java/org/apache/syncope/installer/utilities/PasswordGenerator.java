package org.apache.syncope.installer.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;

public class PasswordGenerator {

    public static String password(final String password, final String digest) {
        String pwd = "";
        try {
            final MessageDigest cript = MessageDigest.getInstance("SHA-1");
            pwd = new String(Hex.encodeHex(cript.digest()));
        } catch (final NoSuchAlgorithmException ex) {
            Logger.getLogger(PasswordGenerator.class.getName()).log(Level.SEVERE, "NoSuchAlgorithmException", ex);

        }
        return pwd;
    }

}
