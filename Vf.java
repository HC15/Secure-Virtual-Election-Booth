import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.crypto.SealedObject;

public class Vf {
    private ServerSocket listen;
    private ServerUtil util;
    private KeyPair serverKeys;
    private ArrayList<Voter> voters;
    private HashMap<String,Integer> result;

    private Vf(int portNumber) {
        try {
            this.listen = new ServerSocket(portNumber, 10);
            this.listen.setSoTimeout(300000); // Set server to timeout if no input in 5 minutes
            this.util = new ServerUtil();
            this.serverKeys = this.util.getServerKeys();
            this.voters = this.util.getVoters();
            this.result = this.util.getResult();
        } catch (SocketException ex) {
            ServerUtil.handleException(ex, "Error in underlying protocol");
        } catch (IOException ex) {
            ServerUtil.handleException(ex, "I/O error occurred when opening the socket");
        } catch (IllegalArgumentException ex) {
            ServerUtil.handleException(ex, "Port number is outside specified range of 0 - 65535");
        }
    }

    private void run() {
        while (true) {
            try {
                Socket connect = this.listen.accept();
                ObjectOutputStream serverOut = new ObjectOutputStream(connect.getOutputStream());
                ObjectInputStream serverIn = new ObjectInputStream(connect.getInputStream());
                PublicKey clientKey = this.util.getClientKey();

                SealedObject nameEncrypted = (SealedObject) serverIn.readObject();
                String name = (String) this.util.decrypt(this.serverKeys.getPrivate(), nameEncrypted);

                byte[] nameSigBytes = new byte[256];
                serverIn.readFully(nameSigBytes);
                Signature nameSig = Signature.getInstance("SHA1withRSA");
                nameSig.initVerify(clientKey);
                nameSig.update(name.getBytes());

                SealedObject vnumberEncrypted = (SealedObject) serverIn.readObject();
                String vnumber = (String) this.util.decrypt(this.serverKeys.getPrivate(), vnumberEncrypted);

                Voter current = this.voters.get(0);
                boolean matches = false;
                if (nameSig.verify(nameSigBytes)) {
                    for (Voter voter : this.voters) {
                        if (voter.getName().equals(name) && voter.getVnumber().equals(vnumber)) {
                            current = voter;
                            matches = true;
                            break;
                        }
                    }
                } else {
                    System.out.println("Digital Signature didn't verify correctly");
                }

                if (matches) {
                    serverOut.writeShort(1);
/*
                    short action = -1;
                    do {
                        action = serverIn.readShort();
                        if (action == 1) {

                        } else if (action == 2) {

                        } else if (action == 3) {

                        } else if (action == 4) {

                        }
                    } while (action != 4);
*/
                } else {
                    serverOut.writeShort(0);
                }

                this.util.updateResult(this.result);
                serverOut.close();
                serverIn.close();
                connect.close();
            } catch (SocketTimeoutException ex) {
                System.out.println("No input in 5 minutes, server will now close");
                break;
            } catch (NullPointerException ex) {
                ServerUtil.handleException(ex, "Input or output stream can't be null");
            } catch (IOException ex) {
                ServerUtil.handleException(ex, "I/O error occurred when opening, closing, or using connection socket");
            } catch (ClassNotFoundException ex) {
                ServerUtil.handleException(ex, "Class of a serialized object cannot be found");
            } catch (NoSuchAlgorithmException ex) {
                ClientUtil.handleException(ex, "No such signature algorithm");
            } catch (InvalidKeyException ex) {
                ServerUtil.handleException(ex, "Invalid key for signature verification");
            } catch (SignatureException ex) {
                ClientUtil.handleException(ex, "Signature object not initialized properly");
            }
        }
    }

    private void close() {
        try {
            this.listen.close();
        } catch (IOException ex) {
            ServerUtil.handleException(ex, "I/O error occurred when closing the socket");
        }
    }

    public static void main(String[] args) {
        int portNumber = -1;
        try {
            portNumber = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            ServerUtil.handleException(ex, "Invalid port number, not a number");
        }
        Vf votingFacility = new Vf(portNumber);
        votingFacility.run();
        votingFacility.close();
    }
}
