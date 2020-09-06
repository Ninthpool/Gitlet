package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/** Represent a commit object.
 * @author Heming Wu
 * */
public class Commit implements Serializable {

    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** Location of .gitlet directory. Assume it already exists.*/
    static final  File GITLET = Utils.join(CWD, ".gitlet");
    /** Location of `COMMIT` directory. */
    static final File COMMIT = Utils.join(GITLET, "Commit");
    /** Location of Object directory, containing blobs. */
    static final File OBJECT = Utils.join(GITLET, "Object");


    /** Construct a commit, which consist of a log message, timestamp,
     * a mapping of file names to blob references, a parent reference,
     * and (for merges) a second parent reference.
     * @param message is the commit message passed in.
     * @param parent is the sha1 name of parent commit.
     */
    public Commit(String message, String parent) throws IOException {
        COMMIT.mkdir();
        _message = message;
        _parent = parent;
        if (parent == null) {
            _timestamp = new Date(0);
            saveCommit();
            Branch.makeBranch("master", _sha);
            Branch.moveHead("master");
            return;
        } else {
            _timestamp = new Date();
            Commit pCommit = getCommitObject(parent);
            copyContent(pCommit);
            loadStage();
            rmUnstaged();
            saveCommit();
            String currentBranch = Branch.getHead();
            Branch.advanceBranch(currentBranch, _sha);
        }
    }

    /** Prints out the ids of all commits that have the given MESSAGE.
     * @param message Message that I want to find.
     */
    public static void findMessage(String message) {
        boolean indicator = false;
        for (File cSubDir : COMMIT.listFiles()) {
            for (File cFile : cSubDir.listFiles()) {
                String shaName = cFile.getName();
                Commit thisCommit = getCommitObject(shaName);
                if (thisCommit.getMessage().equals(message)) {
                    System.out.println(shaName);
                    indicator = true;
                }
            }
        }
        if (!indicator) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }

    }

    /** Remove files that are to be unstaged from _content. */
    private void rmUnstaged() {
        for (String s : StagingArea.getUnStaged().keySet()) {
            _content.remove(s);
        }
    }

    /** Handle initial commit. */
    private void makeInitial() {
        return;
    }


    /** Copy all tracked files from the commit c.
     * @param c the commit I'm copying from.
     * */
    private void copyContent(Commit c) {
        if (c._content != null) {
            for (Map.Entry<String, String> s
                    : c._content.entrySet()) {
                _content.put(s.getKey(), s.getValue());
            }
        }
    }

    /** Update content with files in The staging area.
     *  If the staging area has a file that's the updated version
     *  of a file in previous commit, remove older version from
     *  _content and add the newer version.
     * */
    private void loadStage() throws IOException {
        for (Map.Entry<String, String> s
                : StagingArea.getStagedFile().entrySet()) {
            String fileOriName = s.getKey();
            String fileShaName = s.getValue();
            if (_content.keySet().contains(fileOriName)) {
                _content.remove(fileOriName);
            }
            _content.put(fileOriName, fileShaName);
        }
    }

    /** Write content in this commit to the working directory. */
    public void writeToCWD() {
        for (Map.Entry<String, String> each
                : _content.entrySet()) {
            String fileName = each.getKey();
            String fromBlob = each.getValue();
            writeFileToCWD(fileName);
        }
    }

    /** Write the content of the file named FILENAME (which assumed to be
     * in current commit) to the working directory.
     * @param filename Original name of the file.
     */
    public void writeFileToCWD(String filename) {
        File toFile = Utils.join(CWD, filename);
        String blobID = _content.get(filename);
        String fileContent = StagingArea.getContentFromSha(blobID);
        Utils.writeContents(toFile, fileContent);
    }

    /** Remove file named FILENAME from the working directory. */
    public static void rmFileCWD(String filename) {
        File targetFile = Utils.join(CWD, filename);
        targetFile.delete();
    }



    /** Save serialized Commit object in `Commit` folder and
     * get it's sha1 name. Used the first two character as the name
     * of the Commit file's wrapping folder (for hashing purpose), then
     * save the Commit file with it's sha1 name as file's name.
     */
    private void saveCommit() throws IOException {
        byte[] serializedCommit = Utils.serialize(this);
        _sha = Utils.sha1(serializedCommit);
        String shortSha = _sha.substring(0, 2);
        File commitSubDir = Utils.join(COMMIT, shortSha);
        commitSubDir.mkdir();
        File newCommit = Utils.join(commitSubDir, _sha);
        Utils.writeContents(newCommit, serializedCommit);
    }

    /** Check if there's previous commit. Return true if there is.(Assume
     * gitlet init command is run, so that there is an initial commit named
     * "initial". */
    static boolean existPreviousCommit() {
        return COMMIT.listFiles().length != 1;
    }

    /** Get back commit from it's serialized FILE. *./
     * @param sha1 Ths sha1 ID of the commit, either short or long.
     * @return the Commit object.
     */
    public static Commit getCommitObject(String sha1) {
        String shortSha1 = sha1.substring(0, 2);
        File folder = Utils.join(COMMIT, shortSha1);
        File target = Utils.join(folder, sha1);
        if (!target.exists()) {
            System.out.println("No commit with that id exists");
            System.exit(0);
        }
        Commit result = Utils.readObject(target, Commit.class);
        return result;
    }

    /** Help print out all the commit dated back from Commit c.
     * (Help with log command).
     * @param shaID The shaID of current commit Head is associated with.
     * @param c Interested commit. Usually should be the current commit.
     * */
    static void printAll(String shaID, Commit c) {
        if (shaID == null) {
            return;
        }
        System.out.println("===");
        System.out.format("commit %s\n", shaID);
        SimpleDateFormat formatter
                = new SimpleDateFormat("EEE MMM d hh:mm:ss yyyy ZZZZ ");
        String date = formatter.format(c.getTimestamp());
        System.out.format("Date: %s\n", date);
        System.out.println(c.getMessage());
        System.out.println();
        String pName = c.getParent();
        if (pName != null) {
            Commit pCommit = Commit.getCommitObject(pName);
            printAll(pName, pCommit);
        } else {
            printAll(pName, null);
        }
    }

    /** Print out all the commit that has ever been made.
     * Help with global-log command.
     */
    static void printGlobal() {
        for (File cSubDir : COMMIT.listFiles()) {
            for (File cFile : cSubDir.listFiles()) {
                String shaName = cFile.getName();
                Commit thisCommit = getCommitObject(shaName);
                System.out.println("===");
                System.out.format("commit %s\n", shaName);
                SimpleDateFormat formatter
                        = new SimpleDateFormat("EEE MMM d h"
                        + "h:mm:ss yyyy ZZZZ ");
                String date = formatter.format(thisCommit.getTimestamp());
                System.out.format("Date: %s\n", date);
                System.out.println(thisCommit.getMessage());
                System.out.println();
            }
        }
    }


    /** Get content of the file in the commit. Return as readable String
     * @param filename Name of the file
     * @return content of the file.
     */
    String getFileContent(String filename) throws Exception {
        if (!getContent().keySet().contains(filename)) {
            throw new Exception("No such file in the commit");
        }
        String shaName = getContent().get(filename);
        return StagingArea.getContentFromSha(shaName);
    }


    /** Get parent of this commit.
     * @return sha1 name of the parent commit.
     * */
    public String getParent() {
        return _parent;
    }

    /** Get the content of the current commit.
     * @return The content of current commit.
     */
    public HashMap<String, String> getContent() {
        return _content;
    }

    /** Get the sha1 name of the current commit.
     * @return Sha1 name.
     */
    public String getSha() {
        return _sha;
    }

    /** Get commit message.
     * @return commit message
     * */
    public String getMessage() {
        return _message;
    }

    /** Get time stamp.
     * @return Time stamp.
     */
    public Date getTimestamp() {
        return _timestamp;
    }


    /** Parent of the current commit. */
    private String _parent;

    /** Commit message of current commit. */
    private String _message;
    /** Timestamp of current commit. */
    private Date _timestamp;

    /** Sha-1 of current commit. */
    private String _sha;


    /** Blob content of the commit.
     * Key is file name. Value is corresponding blob's sha1 name. */
    private HashMap<String, String> _content = new HashMap<>();
}


