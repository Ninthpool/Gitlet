package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/** The staging area of current working directory.
 * @author Heming Wu
 * */
public class StagingArea {
    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** Location of .gitlet directory. Assume it already exists.*/
    static final File GITLET = Utils.join(CWD, ".gitlet");
    /** Location of Object directory, containing blobs. */
    static final File OBJECT = Utils.join(GITLET, "Object");
    /** Location of the StagingArea file (In git it's called INDEX). */
    static final File STAGE = Utils.join(GITLET, "StagingArea");
    /** Location of the Unstaged Area.*/
    static final File UNSTAGE = Utils.join(GITLET, "UnstagedArea");

    /** Serialize and save file of FILENAME in .gitlet/object folder.
     *  Hash it's content to get a SHA1 code used as
     *  the file's name.
     * */
    public static void saveFile(String filename) throws IOException {
        if (getUnStaged().containsKey(filename)) {
            File oriPath = Utils.join(CWD, filename);
            String blobID = getUnStaged().get(filename);
            String content = getContentFromSha(blobID);
            Utils.writeObject(oriPath, content);
        }
        Blob b = new Blob(filename);
        OBJECT.mkdir();
        String shaName = Utils.sha1(b.getContent());
        File newFile = Utils.join(OBJECT, shaName);
        newFile.createNewFile();
        Utils.writeContents(newFile, b.getSerialized());
        if (!STAGE.exists()) {
            _stagedFile.put(filename, shaName);
            _trackedFile.put(filename, shaName);
            persistence();
        } else {
            _stagedFile = getStagedFile();
            _stagedFile.put(filename, shaName);
            _trackedFile.put(filename, shaName);
            persistence();
        }
    }

    /** Check if content in file f is the same as in the latest commit. */
    static boolean checkUnchangedContent(File f) throws Exception {
        if (!Commit.existPreviousCommit()) {
            return false;
        }
        String fContent = Utils.readContentsAsString(f);
        Commit cCommit = Branch.getCurrentCommit();
        if (cCommit.getContent().keySet().contains(f.getName())) {
            String oldContent = cCommit.getFileContent(f.getName());

            if (oldContent.equals(fContent)) {
                return true;
            }
        }
        return false;
    }

    /** Get staged files by fetching from the serialized file.
     * @return The staged file, which is previously persisted.
     * */
    @SuppressWarnings("unchecked")
    public static  HashMap<String, String> getStagedFile() throws IOException {
        if (!STAGE.exists()) {
            return null;
        }
        _stagedFile = Utils.readObject(STAGE, HashMap.class);
        return _stagedFile;
    }

    /** Put the file named FILENAME to the unstaged area,
     * so that the next commit knows what files to remove.
     * @param filename The file that's to be unstaged.
     * @param blobPath Sha1 Name of the file.
     */
    @SuppressWarnings("unchecked")
    public static void unstage(String filename, String blobPath) {
        _unStagedFile.put(filename, blobPath);
        persistence();
    }

    /** Serialize _stageFile.
     */
    public static void persistence() {
        Utils.writeObject(STAGE, _stagedFile);
        Utils.writeObject(UNSTAGE, _unStagedFile);
    }

    /** Clear staging area. */
    public static void clear() {
        _stagedFile.clear();
        _unStagedFile.clear();
        persistence();
    }

    /** Check if there's file in staging area.
     * @return boolean value.
     */
    public static boolean hasStagedFile() throws IOException {
        if (!STAGE.exists() && !UNSTAGE.exists()) {
            return false;
        }
        return getStagedFile().size() != 0;
    }

    /** Get back the content from serialized file with name SHA.
     * @return original content of the file with name SHA
     */
    public static String getContentFromSha(String sha) {
        File target = Utils.join(OBJECT, sha);
        String result = Utils.readObject(target, String.class);
        return result;
    }

    /** Get the set of unstaged files. Return null if area not initialized.
     * @return The set of unstaged files.
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getUnStaged() {
        if (!UNSTAGE.exists()) {
            return null;
        }
        _unStagedFile = Utils.readObject(UNSTAGE, HashMap.class);
        return _unStagedFile;
    }

    /** Get all tracked files.
     * @return ALl tracked files.
     * */
    public static HashMap<String, String> getAllTrackedFile() {
        return _trackedFile;
    }

    /** Files that just got staged in the staging area.
     *  keys are file names, values are sha1 name. */
    private static HashMap<String, String> _stagedFile = new HashMap<>();

    /** Unstaged files. Contains only the original name of the files. */
    private static HashMap _unStagedFile = new HashMap<>();

    /** All files that are tracked, using sha1 name.
     *  keys are file names, values are sha1 name. */
    private static  HashMap<String, String> _trackedFile = new HashMap<>();



}
