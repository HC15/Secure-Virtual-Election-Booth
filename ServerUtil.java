import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

class ServerUtil {

    ServerUtil() {}

    static void handleException(Exception exception, String errorMessage) {
        System.err.println(exception.getMessage());
        System.err.println(errorMessage);
        exception.printStackTrace();
        System.exit(1);
    }

    private KeyPair createServerKeys() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);
            KeyPair keys = keyGenerator.generateKeyPair();

            FileOutputStream publicKeyFile = new FileOutputStream("server_public.key");
            publicKeyFile.write(keys.getPublic().getEncoded());
            publicKeyFile.close();

            FileOutputStream privateKeyFile = new FileOutputStream("server_private.key");
            privateKeyFile.write(keys.getPrivate().getEncoded());
            privateKeyFile.close();

            return keys;
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key pair generator algorithm");
        } catch (InvalidParameterException ex) {
            handleException(ex, "Incorrect key size, wrong or not supported");
        } catch (FileNotFoundException ex) {
            handleException(ex, "Name exist but is directory, does not exist and can't be created, or can't be opened");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while writing or closing server key files");
        }
        return null;
    }

    private void createHistory() {
        File historyFile = new File("history");
        try {
            historyFile.createNewFile();
        } catch (IOException ex) {
            handleException(ex, "I/O Error when creating history file");
        }
    }

    private void createResult(ArrayList<String> candidates) {
        try {
            BufferedWriter resultFile = new BufferedWriter(new FileWriter("result"));
            for(String candidate : candidates) {
                resultFile.write(candidate + " 0");
                resultFile.newLine();
            }
            resultFile.close();
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while writing or closing result file");
        }
    }

    KeyPair getServerKeys() {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            FileInputStream publicKeyFile = new FileInputStream("server_public.key");
            byte[] publicKeyByte = new byte[publicKeyFile.available()];
            publicKeyFile.read(publicKeyByte);
            publicKeyFile.close();
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByte);
            PublicKey publicKey = factory.generatePublic(publicKeySpec);

            FileInputStream privateKeyFile = new FileInputStream("server_private.key");
            byte[] privateKeyByte = new byte[privateKeyFile.available()];
            privateKeyFile.read(privateKeyByte);
            privateKeyFile.close();
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyByte);
            PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key generator algorithm");
        } catch (FileNotFoundException ex) {
            System.out.println("Server key files not found, generating keys and saving to files");
            return this.createServerKeys();
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while reading or closing server key files");
        } catch (InvalidKeySpecException ex) {
            handleException(ex, "Given key specification is inappropriate for this key factory");
        }
        return null;
    }

    PublicKey getClientKey() {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            FileInputStream publicKeyFile = new FileInputStream("client_public.key");
            byte[] publicKeyByte = new byte[publicKeyFile.available()];
            publicKeyFile.read(publicKeyByte);
            publicKeyFile.close();
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByte);
            return factory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key generator algorithm");
        } catch (FileNotFoundException ex) {
            handleException(ex, "Client public key file not found, run client to generate key");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while reading or closing client public key file");
        } catch (InvalidKeySpecException ex) {
            handleException(ex, "Given key specification is inappropriate for this key factory");
        }
        return null;
    }

    ArrayList<Voter> getVoters() {
        ArrayList<Voter> voters = new ArrayList<Voter>();
        String line;

        try {
            BufferedReader voterinfoFile = new BufferedReader(new FileReader("voterinfo"));
            String[] voterinfo;
            while ((line = voterinfoFile.readLine()) != null && !line.trim().isEmpty()) {
                voterinfo = line.split(" ");
                if (voterinfo.length == 2) {
                    voters.add(new Voter(voterinfo[0], voterinfo[1]));
                }
            }
            voterinfoFile.close();
        } catch (FileNotFoundException ex) {
            handleException(ex, "No voter info since file not found, add file named 'voterinfo'");
        } catch (IOException ex) {
            handleException(ex, "Error when reading lines from the voterinfo file or from closing file");
        }

        try {
            BufferedReader historyFile = new BufferedReader(new FileReader("history"));
            String[] history;
            while ((line = historyFile.readLine()) != null && !line.trim().isEmpty()) {
                history = line.split(" ");
                if (history.length == 2) {
                    for (Voter voter : voters) {
                        if (voter.getVnumber().equals(history[0])) {
                            voter.setVoted();
                            voter.setVoteTime(history[1]);
                            break;
                        }
                    }
                }
            }
            historyFile.close();
        } catch (FileNotFoundException ex) {
            this.createHistory();
        } catch (IOException ex) {
            handleException(ex, "Error when reading lines from the history file or from closing file");
        }

        return voters;
    }

    ArrayList<String> getCandidates() {
        ArrayList<String> candidates = new ArrayList<String>();

        try {
            BufferedReader candidateinfoFile = new BufferedReader(new FileReader("candidateinfo"));
            String line;
            while ((line = candidateinfoFile.readLine()) != null && !line.trim().isEmpty()) {
                candidates.add(line);
            }
            candidateinfoFile.close();
        } catch (FileNotFoundException ex) {
            handleException(ex, "No candidate info since file not found, add file named 'candidateinfo'");
        } catch (IOException ex) {
            handleException(ex, "Error when reading lines from the candidateinfo file or from closing file");
        }

        return candidates;
    }

    HashMap<String,Integer> getResult() {
        HashMap<String,Integer> result = new HashMap<String,Integer>();

        try {
            BufferedReader resultFile = new BufferedReader(new FileReader("result"));
            String line;
            String[] candidateVote;
            while ((line = resultFile.readLine()) != null && !line.trim().isEmpty()) {
                candidateVote = line.split(" ");
                if (candidateVote.length == 2) {
                    result.put(candidateVote[0], Integer.parseInt(candidateVote[1]));
                }
            }
            resultFile.close();
        } catch (FileNotFoundException ex) {
            ArrayList<String> candidates = this.getCandidates();
            this.createResult(candidates);
            for (String candidate : candidates) {
                result.put(candidate, 0);
            }
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred when reading or closing result file");
        } catch (ArrayIndexOutOfBoundsException ex) {
            handleException(ex, "Invalid history file, not in correct format <registration number> <time voted>");
        }

        return result;
    }

    void updateHistory(Voter voter) {
        try {
            BufferedWriter historyFile = new BufferedWriter(new FileWriter("history", true));
            historyFile.write(voter.getVnumber() + " " + voter.getVoteTime());
            historyFile.newLine();
            historyFile.close();
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while writing to or closing history file");
        }
    }

    void updateResult(HashMap<String,Integer> result) {
        try {
            BufferedWriter resultFile = new BufferedWriter(new FileWriter("result"));
            for(String candidate : result.keySet()) {
                resultFile.write(candidate + " " + Integer.toString(result.get(candidate)));
                resultFile.newLine();
            }
            resultFile.close();
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while writing to or closing result file");
        }
    }

    Object decrypt(Key key, SealedObject encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return encrypted.getObject(cipher);
        } catch (NoSuchPaddingException ex) {
            handleException(ex, "Transformation contains a padding scheme that is not available");
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such cipher algorithm");
        } catch (InvalidKeyException ex) {
            handleException(ex, "Invalid key for initializing the cipher");
        } catch (IllegalBlockSizeException ex) {
            handleException(ex, "Invalid cipher block size");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred during de-serialization");
        } catch (ClassNotFoundException ex) {
            handleException(ex, "Class not found, error occurred during de-serialization");
        } catch (BadPaddingException ex) {
            handleException(ex, "Input data does not have proper expected padding bytes");
        }
        return null;
    }
}
