import javax.crypto.SealedObject;
import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;

public class Vf {
    private ServerSocket listen;
    private ServerUtil util;
    private KeyPair keys;
    private ArrayList<Voter> voters;
    private HashMap<String,Integer> result;

    private Vf(int portNumber) {
        try {
            this.listen = new ServerSocket(portNumber);
            this.listen.setSoTimeout(300000); // Set server to timeout if no input in 5 minutes
            this.util = new ServerUtil();
            this.keys = this.util.getKeyPair();
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

                serverOut.writeObject(this.keys.getPublic());
                SealedObject encryptedVoterinfo = (SealedObject) serverIn.readObject();
                String voterinfo = (String) this.util.decrypt(this.keys.getPrivate(), encryptedVoterinfo);

                Voter current = this.voters.get(0);
                boolean matches = false;
                for (Voter voter : this.voters) {
                    if (voter.getVoterinfo().equals(voterinfo)) {
                        current = voter;
                        matches = true;
                        break;
                    }
                }

                if (matches) {
                    serverOut.writeShort(1);
                    short action = serverIn.readShort();
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
            } catch (InvalidClassException ex) {
                ServerUtil.handleException(ex, "Something is wrong with class used by serialization");
            } catch (ClassNotFoundException ex) {
                ServerUtil.handleException(ex, "Class of a serialized object cannot be found");
            } catch (IOException ex) {
                ServerUtil.handleException(ex, "I/O error occurred when opening, closing, or using connection socket");
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
