import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class VoterCli {
    private Socket client;
    private ClientUtil util;
    private KeyPair clientKeys;

    private VoterCli(String serverDomain, int portNumber) {
        try {
            this.client = new Socket(serverDomain, portNumber);
            this.util = new ClientUtil();
            this.clientKeys = this.util.getClientKeys();
        } catch (UnknownHostException ex) {
            ClientUtil.handleException(ex, "IP address of the host could not be determined");
        } catch (IOException ex) {
            ClientUtil.handleException(ex, "I/O error occurred when creating the socket");
        } catch (IllegalArgumentException ex) {
            ClientUtil.handleException(ex, "Port number is outside specified range of 0 - 65535");
        }
    }

    private void run() {
        try {
            ObjectOutputStream clientOut = new ObjectOutputStream(this.client.getOutputStream());
            ObjectInputStream clientIn = new ObjectInputStream(this.client.getInputStream());
            PublicKey serverPublicKey = this.util.getServerKey();

            String name = this.util.inputName();
            clientOut.writeObject(util.encrypt(serverPublicKey, name));

            Signature nameSig = Signature.getInstance("SHA1withRSA");
            nameSig.initSign(this.clientKeys.getPrivate());
            nameSig.update(name.getBytes());
            clientOut.write(nameSig.sign());

            String vnumber = this.util.inputVnumber();
            clientOut.writeObject(util.encrypt(serverPublicKey, vnumber));
            
            if (clientIn.readShort() == 1) {
                Short action = this.util.menu(name);
/*
                do {
                    action = this.util.menu(name);
                    clientOut.writeShort(action);
                    if (action == 1) {

                    } else if (action == 2) {

                    } else if (action == 3)

                    } else if (action == 4) {
                        System.out.println("Voter client will now terminate");
                    }
                } while (action != 4);
*/
            } else {
                System.out.println("Invalid name or registration number");
            }
        } catch (IOException ex) {
            ClientUtil.handleException(ex, "I/O error occurred while client was running");
        } catch (NoSuchAlgorithmException ex) {
            ClientUtil.handleException(ex, "No such signature algorithm");
        } catch (InvalidKeyException ex) {
            ClientUtil.handleException(ex, "Invalid key for signature signing");
        } catch (SignatureException ex) {
            ClientUtil.handleException(ex, "Signature object not initialized properly");
        }
    }

    private void close() {
        try {
            this.client.close();
        } catch (IOException ex) {
            ClientUtil.handleException(ex, "I/O error occurred when closing the socket");
        }
    }

    public static void main(String[] args) {
        String serverDomain = args[0];
        int portNumber = 0;
        try {
            portNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            ClientUtil.handleException(ex, "Invalid port number, not a number");
        }
        VoterCli voterClient = new VoterCli(serverDomain, portNumber);
        voterClient.run();
        voterClient.close();
    }
}
