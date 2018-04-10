import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

class ClientUtil {
    private Cipher cipher;
    private BufferedReader userInput;

    ClientUtil() {
        try {
            this.cipher = Cipher.getInstance("RSA");
            this.userInput = new BufferedReader(new InputStreamReader(System.in));
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such cipher algorithm");
        } catch (NoSuchPaddingException ex) {
            handleException(ex, "Transformation contains a padding scheme that is not available");
        }
    }

    static void handleException(Exception exception, String message) {
        System.err.println(exception.getMessage());
        System.err.println(message);
        exception.printStackTrace();
        System.exit(1);
    }

    private boolean invalidName(String name) {
        return (name.isEmpty() || name.indexOf(' ') != -1);
    }

    private boolean invalidNumber(String vnumber) {
        try {
            Integer.parseInt(vnumber);
        } catch (NumberFormatException ex) {
            return true;
        }
        return vnumber.length() != 9;
    }

    private boolean invalidAction(String action) {
        if (action.length() == 1) {
            return action.charAt(0) >= '0' && action.charAt(0) <= '4';
        } else {
            return false;
        }
    }

    String inputName() {
        String name = new String();
        do {
            System.out.print("Enter name: ");
            try {
                name = this.userInput.readLine();
            } catch (IOException ex) {
                handleException(ex, "I/O error occurred while inputting name");
            }
            if (this.invalidName(name)) {
                System.out.println("Invalid name, name can't be empty or contain spaces");
            }
        } while (this.invalidName(name));
        return name;
    }

    String inputNumber() {
        String vnumber = new String();
        try {
            do {
                System.out.print("Enter voter registration number: ");
                vnumber = this.userInput.readLine();
                if (this.invalidNumber(vnumber)) {
                    System.out.println("Invalid voter registration number");
                }
            } while (this.invalidNumber(vnumber));
        } catch (IOException ex) {
            handleException(ex, "I/O error occurred while inputting voter registration number");
        }
        return vnumber;
    }

    String menu(String name) {
        String action = new String();
        do {
            System.out.println("Welcome, " + name);
            System.out.println("    Main Menu");
            System.out.println("Please enter a number (1-4)");
            System.out.println("1. Vote");
            System.out.println("2. My vote history");
            System.out.println("3. Election result");
            System.out.println("4. Quit");
            System.out.print("Enter action here: ");
            try {
                action = this.userInput.readLine();
            } catch (IOException ex) {
                handleException(ex, "");
            }
            if (this.invalidAction(action)) {
                System.out.println("Invalid action, must be 1, 2, 3, or 4");
            }
        } while (this.invalidAction(action));
        return action;
    }

    SealedObject encrypt(Key key, String message) {
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, key);
            return new SealedObject(message, this.cipher);
        } catch (InvalidKeyException ex) {
            handleException(ex, "Invalid key for initializing the cipher");
        } catch (IllegalBlockSizeException ex) {
            handleException(ex, "Invalid cipher block size");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred during serialization");
        }
        return null;
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
}
