import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

class ServerUtil {
    private Cipher cipher;
    private KeyPairGenerator keyGenerator;

    ServerUtil() {
        try {
            this.cipher = Cipher.getInstance("RSA");
            this.keyGenerator = KeyPairGenerator.getInstance("RSA");
            this.keyGenerator.initialize(2048);
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key pair generator or cipher algorithm");
        } catch (NoSuchPaddingException ex) {
            handleException(ex, "Transformation contains a padding scheme that is not available");
        } catch (InvalidParameterException ex) {
            handleException(ex, "Incorrect key size, wrong or not supported");
        }
    }

    static void handleException(Exception exception, String errorMessage) {
        System.err.println(exception.getMessage());
        System.err.println(errorMessage);
        exception.printStackTrace();
        System.exit(1);
    }

    KeyPair getKeyPair() {
        return this.keyGenerator.genKeyPair();
    }

    Object decrypt(Key key, SealedObject encrypted) {
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, key);
            return encrypted.getObject(this.cipher);
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

    ArrayList<Voter> getVoters() {
        ArrayList<Voter> voters = new ArrayList<Voter>();

        String line;
        try {
            BufferedReader voterinfoFile = new BufferedReader(new FileReader("voterinfo"));
            while ((line = voterinfoFile.readLine()) != null && !line.trim().isEmpty()) {
                voters.add(new Voter(line));
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
                for (Voter voter : voters) {
                    if (voter.getVoterinfo().contains(history[0])) {
                        voter.setVoted();
                        voter.setVoteTime(history[1]);
                        break;
                    }
                }
            }
            historyFile.close();
        } catch (FileNotFoundException ex) {
            this.createHistory();
        } catch (IOException ex) {
            handleException(ex, "Error when reading lines from the history file or from closing file");
        } catch (ArrayIndexOutOfBoundsException ex) {
            handleException(ex, "Invalid history file, not in correct format <registration number> <time voted>");
        }

        return voters;
    }

    ArrayList<String> getCandidates() {
        ArrayList<String> candidates = new ArrayList<String>();
        String line;
        try {
            BufferedReader candidateinfoFile = new BufferedReader(new FileReader("candidateinfo"));
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
        String line;
        String[] candidateVote;
        try {
            BufferedReader resultFile = new BufferedReader(new FileReader("result"));
            while ((line = resultFile.readLine()) != null && !line.trim().isEmpty()) {
                candidateVote = line.split(" ");
                result.put(candidateVote[0], Integer.parseInt(candidateVote[1]));
            }
            resultFile.close();
        } catch (FileNotFoundException ex) {
            ArrayList<String> candidates = this.getCandidates();
            this.createResult(candidates);
            for (String candidate : candidates) {
                result.put(candidate, 0);
            }
        } catch (IOException ex) {
            handleException(ex, "I/O Error when using result file");
        } catch (ArrayIndexOutOfBoundsException ex) {
            handleException(ex, "Invalid history file, not in correct format <registration number> <time voted>");
        }
        return result;
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
                resultFile.flush();
            }
            resultFile.close();
        } catch (IOException ex) {
            handleException(ex, "I/O Error when using result file");
        }
    }

    void updateHistory(Voter voter) {
        try {
            BufferedWriter historyFile = new BufferedWriter(new FileWriter("history", true));
            historyFile.write(voter.getVoteTime());
            historyFile.newLine();
            historyFile.flush();
            historyFile.close();
        } catch (IOException ex) {
            handleException(ex, "I/O Error when using history file");
        }
    }

    void updateResult(HashMap<String,Integer> result) {
        try {
            BufferedWriter resultFile = new BufferedWriter(new FileWriter("result"));
            for(String candidate : result.keySet()) {
                resultFile.write(candidate + Integer.toString(result.get(candidate)));
                resultFile.newLine();
                resultFile.flush();
            }
            resultFile.close();
        } catch (IOException ex) {
            handleException(ex, "I/O Error when using result file");
        }
    }
}
