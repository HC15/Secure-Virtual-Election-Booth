import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

class ClientUtil {
    private BufferedReader userInput;

    ClientUtil() {
        this.userInput = new BufferedReader(new InputStreamReader(System.in));
    }

    static void handleException(Exception exception, String message) {
        System.err.println(exception.getMessage());
        System.err.println(message);
        exception.printStackTrace();
        System.exit(1);
    }

    private KeyPair createClientKeys() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);
            KeyPair keys = keyGenerator.generateKeyPair();

            FileOutputStream publicKeyFile = new FileOutputStream("client_public.key");
            publicKeyFile.write(keys.getPublic().getEncoded());
            publicKeyFile.close();

            FileOutputStream privateKeyFile = new FileOutputStream("client_private.key");
            privateKeyFile.write(keys.getPrivate().getEncoded());
            privateKeyFile.close();

            return keys;
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key pair generator algorithm");
        } catch (InvalidParameterException ex) {
            handleException(ex, "Invalid key size, wrong or not supported");
        } catch (FileNotFoundException ex) {
            handleException(ex, "Name exist but is directory, does not exist and can't be created, or can't be opened");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while writing or closing client key files");
        }
        return null;
    }

    KeyPair getClientKeys() {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            FileInputStream publicKeyFile = new FileInputStream("client_public.key");
            byte[] publicKeyByte = new byte[publicKeyFile.available()];
            publicKeyFile.read(publicKeyByte);
            publicKeyFile.close();
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByte);
            PublicKey publicKey = factory.generatePublic(publicKeySpec);

            FileInputStream privateKeyFile = new FileInputStream("client_private.key");
            byte[] privateKeyByte = new byte[privateKeyFile.available()];
            privateKeyFile.read(privateKeyByte);
            privateKeyFile.close();
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyByte);
            PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key generator algorithm");
        } catch (FileNotFoundException ex) {
            System.out.println("Client key files not found, generating keys and saving to files");
            return this.createClientKeys();
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while reading or closing client key files");
        } catch (InvalidKeySpecException ex) {
            handleException(ex, "Given key specification is inappropriate for this key factory");
        }
        return null;
    }

    PublicKey getServerKey() {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            FileInputStream publicKeyFile = new FileInputStream("server_public.key");
            byte[] publicKeyByte = new byte[publicKeyFile.available()];
            publicKeyFile.read(publicKeyByte);
            publicKeyFile.close();
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByte);
            return factory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such key generator algorithm");
        } catch (FileNotFoundException ex) {
            handleException(ex, "Server public key file not found, run server to generate key");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred while reading or closing server public key file");
        } catch (InvalidKeySpecException ex) {
            handleException(ex, "Given key specification is inappropriate for this key factory");
        }
        return null;
    }

    private boolean invalidName(String name) {
        return (name.isEmpty() || name.indexOf(' ') != -1);
    }

    private boolean invalidVnumber(String vnumber) {
        try {
            Integer.parseInt(vnumber);
        } catch (NumberFormatException ex) {
            return true;
        }
        return vnumber.length() != 9;
    }

    private boolean invalidAction(String action) {
        if (action.length() == 1) {
            return action.charAt(0) < '1' || action.charAt(0) > '4';
        } else {
            return true;
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

    String inputVnumber() {
        String vnumber = new String();
        do {
            System.out.print("Enter voter registration number: ");
            try {
                vnumber = this.userInput.readLine();
            } catch (IOException ex) {
                handleException(ex, "I/O error occurred while inputting voter registration number");
            }
            if (this.invalidVnumber(vnumber)) {
                System.out.println("Invalid voter registration number, must be number with 9 digits");
            }
        } while (this.invalidVnumber(vnumber));
        return vnumber;
    }

    Short menu(String name) {
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
                handleException(ex, "I/O error occurred while inputting action to menu");
            }
            if (this.invalidAction(action)) {
                System.out.println("Invalid action, must be number (1-4)\n");
            }
        } while (this.invalidAction(action));
        return Short.parseShort(action);
    }

    SealedObject encrypt(Key key, String message) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return new SealedObject(message, cipher);
        } catch (NoSuchAlgorithmException ex) {
            handleException(ex, "No such cipher algorithm");
        } catch (NoSuchPaddingException ex) {
            handleException(ex, "Transformation contains a padding scheme that is not available");
        } catch (InvalidKeyException ex) {
            handleException(ex, "Invalid key for initializing the cipher");
        } catch (IllegalBlockSizeException ex) {
            handleException(ex, "Invalid cipher block size");
        } catch (IOException ex) {
            handleException(ex, "I/O Error occurred during serialization");
        }
        return null;
    }
}
