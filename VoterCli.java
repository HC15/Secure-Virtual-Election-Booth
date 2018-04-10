import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

public class VoterCli {
    private Socket client;
    private ClientUtil util;

    private VoterCli(String serverDomain, int portNumber) {
        try {
            this.client = new Socket(serverDomain, portNumber);
            this.util = new ClientUtil();
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
            PublicKey vfPublicKey = (PublicKey) clientIn.readObject();

            String name = this.util.inputName();
            String vnumber = this.util.inputNumber();
            String voterinfo = name + ' ' + vnumber;
            clientOut.writeObject(util.encrypt(vfPublicKey, voterinfo));

            short response = clientIn.readShort();
            if (response == 1) {
                String action;
                do {
                    action = this.util.menu(name);
                    clientOut.writeShort(Short.parseShort(action));
                    if (action.equals("1")) {

                    } else if (action.equals("2")) {

                    } else if (action.equals("3")) {

                    } else if (action.equals("4")) {
                        System.out.println("Voter client will now terminate");
                    }
                } while (!action.equals("4"));

            } else {
                System.out.println("Invalid name or registration number");
            }
        } catch (IOException ex) {
            ClientUtil.handleException(ex, "I/O error occurred while client was running");
        } catch (ClassNotFoundException ex) {
            System.exit(1);
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
