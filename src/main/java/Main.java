import java.io.*;  // For file handling and input-output operations
import java.nio.charset.StandardCharsets; // For making API requests
import java.nio.file.Files; // For URL handling
import java.nio.file.Path; // Encoding support
import java.nio.file.Paths; // File reading/writing
import java.security.MessageDigest; // Handling file paths
import java.security.NoSuchAlgorithmException; // Path manipulations
import java.time.Instant; // For hashing (like Git)
import java.util.*;
import java.util.zip.DeflaterOutputStream; // Handling hashing errors
import java.util.zip.InflaterInputStream; // Handling timestamps

public class Main {
    public static void main(String[] args) throws IOException { 
        if (args.length == 0) {
            System.out.println("Usage: java Main <command> [options]");
            return;
        }

        final String command = args[0];

        switch (command) {
            case "init" -> initializeGitDefault();

            case "cat-file" -> catFileHandler(args);

            case "hash-object" -> {
                if (args.length < 2) {
                    throw new IllegalArgumentException("Usage: java Main hash-object [-w] <file>");
                }
                boolean write = args[1].equals("-w");
                String fileName = write ? args[2] : args[1];
                System.out.println(createBlobObject(fileName, write));
            }

            case "ls-tree" -> lsTreeHandler(args);

            case "write-tree" -> {
                writeTreeHandler();
                break;
            }

            // case "commit-tree" -> {
            //     if (args.length < 6) {
            //         throw new IllegalArgumentException(
            //             "Usage: java Main commit-tree <tree> -p <parent> -m <message>");
            //     }
            //     String treeHash = args[1];
            //     String parentHash = args[3];
            //     String message = args[5];
            //     String commitHash = commitTreeHandler(treeHash, parentHash, message);
            //     System.out.println(commitHash);
            //     break;
            // }

            case "commit-tree" -> {
                if (args.length < 3) {
                    throw new IllegalArgumentException(
                        "Usage: java Main commit-tree <tree> [-p <parent>] -m <message>");
                }

                String treeHash = args[1];
                String parentHash = null;
                String message = null;

                for (int i = 2; i < args.length; i++) {
                    switch (args[i]) {
                        case "-p" -> parentHash = args[++i];
                        case "-m" -> message = args[++i];
                    }
                }

                if (message == null) {
                    throw new IllegalArgumentException("Commit message is required with -m");
                }

                String commitHash = commitTreeHandler(treeHash, parentHash, message);
                System.out.println(commitHash);
                break;
            }

            
            case "clone" -> {
                if (args.length != 3) {
                throw new IllegalArgumentException(
                    "Usage: java Main clone <repository-url> <target-directory>");
                }
                cloneRepository(args);
                break;
          }
            case "add" ->{
                if (args.length < 2) {
                    System.out.println("Usage: java Main add <file>");
                } else {
                    addToIndex(args[1]);
                }
            }

            default -> System.out.println("Unknown command: " + command);
        }
    }
    
public static void initializeGitDefault() throws IOException {
    File root = new File(".git");
    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();
    File head = new File(root, "HEAD");
    try {
        head.createNewFile();
        Files.write(head.toPath(),
                    "ref: refs/heads/main\n".getBytes(StandardCharsets.UTF_8));
        System.out.println("Initialized git directory");
    } catch (IOException e) {
        throw new IOException("Error initializing git repository", e);
    }
}

public static void initializeGit(String targetDir) throws IOException {
    File root = new File(targetDir, ".git");
    new File(root, "objects").mkdirs();
    new File(root, "refs").mkdirs();
    File head = new File(root, "HEAD");
    try {
        head.createNewFile();
        Files.write(head.toPath(),
                    "ref: refs/heads/main\n".getBytes(StandardCharsets.UTF_8));
        System.out.println("Initialized git directory at: " + targetDir);
    } catch (IOException e) {
        throw new IOException("Error initializing git repository", e);
    }
}

private static void catFileHandler(String[] args) {
    if (args.length < 3 || !args[1].equals("-p")) {
        System.out.println("Usage: java Main cat-file -p <hash>");
        return;
    }

    String hash = args[2];
    if (hash.length() < 6) {  // Ensure valid hash length
        System.out.println("Invalid hash.");
        return;
    }

    String dirHash = hash.substring(0, 2);
    String fileHash = hash.substring(2);
    File blobFile = new File(".git/objects/" + dirHash + "/" + fileHash);

    if (!blobFile.exists()) {
        System.out.println("Error: Object not found.");
        return;
    }

    try (FileInputStream fis = new FileInputStream(blobFile);
         InflaterInputStream inflater = new InflaterInputStream(fis);
         Scanner scanner = new Scanner(inflater)) {

        StringBuilder content = new StringBuilder();
        while (scanner.hasNextLine()) {
            content.append(scanner.nextLine()).append("\n");
        }

        String decompressed = content.toString();
        String fileContent = decompressed.substring(decompressed.indexOf("\0") + 1);
        System.out.print(fileContent);

    } catch (IOException e) {
        System.err.println("Error reading object: " + e.getMessage());
    }
}

public static String createBlobObject(String fileName, boolean write) throws IOException {
    try {
        byte[] fileContents = Files.readAllBytes(Paths.get(fileName));
        String header = "blob " + fileContents.length + "\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] fullContent = new byte[headerBytes.length + fileContents.length];

        System.arraycopy(headerBytes, 0, fullContent, 0, headerBytes.length);
        System.arraycopy(fileContents, 0, fullContent, headerBytes.length, fileContents.length);

        String sha1Hash = sha1Hex(fullContent);

        if (write) {
            String blobPath = shaToPath(sha1Hash);
            File blobFile = new File(blobPath);
            blobFile.getParentFile().mkdirs();

            try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(blobFile))) {
                out.write(fullContent);
            }
        }

        return sha1Hash;
    } catch (IOException e) {
        throw new IOException("Error processing file: " + fileName, e);
    }
}

public static void lsTreeHandler(String[] args) throws IOException {
    if (args.length < 3) {
        throw new IllegalArgumentException(
            "Usage: java Main ls-tree --name-only <tree-ish>");
    }
    boolean nameOnly = args[1].equals("--name-only");
    String treeIsh = args[2];
    List<String> entries = readTreeObject(treeIsh);
    if (nameOnly) {
        List<String> names = new ArrayList<>();
        for (String entry : entries) {
            names.add(entry.split("\t")[1]);
        }
        Collections.sort(names);
        for (String name : names) {
            System.out.println(name);
        }
    } else {
        for (String entry : entries) {
            System.out.println(entry);
        }
    }
}

private static List<String> readTreeObject(String hash) throws IOException {
    String objectPath = shaToPath(hash);
    List<String> entries = new ArrayList<>();
    try (InflaterInputStream inflaterStream =
             new InflaterInputStream(new FileInputStream(objectPath));
         DataInputStream dataIn = new DataInputStream(inflaterStream)) {
        String header = readNullTerminatedString(dataIn);
        if (!header.startsWith("tree ")) {
            throw new IOException("Invalid tree object header");
        }
        while (dataIn.available() > 0) {
            String mode = readUntilSpace(dataIn);
            String name = readNullTerminatedString(dataIn);
            byte[] sha = new byte[20];
            dataIn.readFully(sha);
            String shaHex = bytesToHex(sha);
            entries.add(String.format("%s %s %s\t%s", mode,
                                      mode.startsWith("100") ? "blob" : "tree",
                                      shaHex, name));
        }
    }
    Collections.sort(entries);
    return entries;
}

private static String readNullTerminatedString(DataInputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();
    int ch;
    while ((ch = in.read()) != 0) {
        sb.append((char) ch);
    }
    return sb.toString();
}

private static String readUntilSpace(DataInputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();
    int ch;
    while ((ch = in.read()) != ' ') {
        sb.append((char) ch);
    }
    return sb.toString();
}

public static void writeTreeHandler() throws IOException {
    String treeHash = writeTree(Paths.get("."));
    System.out.print(treeHash);
}

private static String writeTree(Path dir) throws IOException {
    ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
    Files.list(dir).sorted().forEach(path -> {
        try {
            String relativePath = dir.relativize(path).toString();
            if (Files.isDirectory(path)) {
                if (!relativePath.equals(".git")) {
                    String subTreeHash = writeTree(path);
                    writeTreeEntry(treeContent, "40000", relativePath, subTreeHash);
                }
            } else {
                String blobHash = createBlobObject(path.toString(), true);
                String mode = Files.isExecutable(path) ? "100755" : "100644";
                writeTreeEntry(treeContent, mode, relativePath, blobHash);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    });

    byte[] content = treeContent.toByteArray();
    String header = "tree " + content.length + "\0";
    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
    byte[] fullContent = new byte[headerBytes.length + content.length];
    System.arraycopy(headerBytes, 0, fullContent, 0, headerBytes.length);
    System.arraycopy(content, 0, fullContent, headerBytes.length, content.length);
    
    return sha1Hex(fullContent);
}

private static void writeTreeEntry(ByteArrayOutputStream out, String mode, String name, String hash) throws IOException {
    out.write(String.format("%s %s\0", mode, name).getBytes(StandardCharsets.UTF_8));
    out.write(hexToBytes(hash));
}

private static byte[] hexToBytes(String hex) {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
        bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return bytes;
}

public static String commitTreeHandler(String treeHash, String parentHash, String message) throws IOException {
    String timestamp = Instant.now().toString();
    String author = "Avani Mathur <mathur_avani@gmail.com>";
    String committer = "Avani Mathur <mathur_avani@gmail.com>";
    
    StringBuilder commitContent = new StringBuilder();
    commitContent.append("tree ").append(treeHash).append("\n");
    
    if (parentHash != null && !parentHash.isEmpty()) {
        commitContent.append("parent ").append(parentHash).append("\n");
    }
    
    commitContent.append("author ")
                 .append(author)
                 .append(" ")
                 .append(timestamp)
                 .append("\n");
                 
    commitContent.append("committer ")
                 .append(committer)
                 .append(" ")
                 .append(timestamp)
                 .append("\n");
                 
    commitContent.append("\n").append(message).append("\n");
    
    byte[] content = commitContent.toString().getBytes(StandardCharsets.UTF_8);
    String header = "commit " + content.length + "\0";
    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
    byte[] fullContent = new byte[headerBytes.length + content.length];
    
    System.arraycopy(headerBytes, 0, fullContent, 0, headerBytes.length);
    System.arraycopy(content, 0, fullContent, headerBytes.length, content.length);
    
    String commitHash = sha1Hex(fullContent);
    String commitPath = shaToPath(commitHash);
    
    File commitFile = new File(commitPath);
    commitFile.getParentFile().mkdirs();
    
    try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(commitFile))) {
        out.write(fullContent);
    }
    
    return commitHash;
}

private static String shaToPath(String sha) {
    return String.format(".git/objects/%s/%s", sha.substring(0, 2), sha.substring(2));
}

public static String sha1Hex(byte[] input) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return bytesToHex(md.digest(input));
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-1 algorithm not found", e);
    }
}

private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

public static void cloneRepository(String[] args) throws IOException {
    String repoLink = args[1];
    String dirName = args[2];
    File repoDir = new File(dirName);
    // Create the directory if it doesn't exist
    if (!repoDir.exists()) {
      repoDir.mkdirs();
    }
    try {
      // Execute the git clone command
      ProcessBuilder processBuilder =
          new ProcessBuilder("git", "clone", repoLink, dirName);
      processBuilder.inheritIO(); // To inherit IO of the current process
      Process process = processBuilder.start(); // Start the process
      int exitCode = process.waitFor(); // Wait for the process to complete
      if (exitCode != 0) {
        throw new RuntimeException("Failed to clone repository. Exit code: " +
                                   exitCode);
      } else {
        System.out.println("Repository cloned successfully into: " + dirName);
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "An I/O error occurred while trying to clone the repository.", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // Restore interrupted state
      throw new RuntimeException("Cloning process was interrupted.", e);
    }
  }

  public static void addToIndex(String filePath) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) {
        System.out.println("File not found: " + filePath);
        return;
    }

    // Step 1: Read file content
    byte[] fileContents = Files.readAllBytes(file.toPath());
    String header = "blob " + fileContents.length + "\0";
    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
    byte[] fullContent = new byte[headerBytes.length + fileContents.length];
    System.arraycopy(headerBytes, 0, fullContent, 0, headerBytes.length);
    System.arraycopy(fileContents, 0, fullContent, headerBytes.length, fileContents.length);

    // Step 2: Hash content and save blob
    String sha1 = sha1Hex(fullContent); // Already defined in your file
    String dir = sha1.substring(0, 2);
    String fileSha = sha1.substring(2);
    File blobFile = new File(".git/objects/" + dir + "/" + fileSha);
    blobFile.getParentFile().mkdirs();

    try (DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(blobFile))) {
        out.write(fullContent);
    }

    // Step 3: Update .git/index
    File indexFile = new File(".git/index");
    indexFile.getParentFile().mkdirs();
    List<String> index = new ArrayList<>();

    if (indexFile.exists()) {
        index = Files.readAllLines(indexFile.toPath());
        index = index.stream()
                .filter(line -> !line.startsWith(filePath + " "))
                .collect(Collectors.toList());
    }

    index.add(filePath + " " + sha1);
    Files.write(indexFile.toPath(), index);
    System.out.println("Staged: " + filePath);
}


}