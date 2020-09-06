package gitlet;

import java.io.File;
import java.util.HashMap;

/** A branch representation which contains
 * all created branch and the Head pointer.
 * @author Heming Wu
 */
public class Branch {
    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** Location of .gitlet directory. Assume it already exists.*/
    static final File GITLET = Utils.join(CWD, ".gitlet");
    /** Location of the Head file. */
    static final File HEAD = Utils.join(GITLET, "HEAD");
    /** Location of the file containing all branches. */
    static final File BRANCHES = Utils.join(GITLET, "Branches");

    /** Make a new branch.
     * @param bName Name of the new branch
     * @param cName The sha1 name of the commit the branch is
     * going to point to.
     */
    public static void makeBranch(String bName, String cName) {
        if (bName.equals("master")) {
            _allBranches.put("master", cName);
            persistence();
            return;
        }
        _allBranches = getAllBranches();
        _allBranches.put(bName, cName);
        persistence();
    }

    /** Move head to a commit or a branch. Write content to the HEAD file.
     * @param name Name of the branch (usually) or the commit the
     * Head is going to point to.
     */
    public static void moveHead(String name) {
        _head = name;
        Utils.writeContents(HEAD, _head);
    }

    /** Serialize the container of all branches.
     * (In git it's in the /ref directory).
     */
    public static void persistence() {
        Utils.writeObject(BRANCHES, _allBranches);
    }

    /** Get back the container of all branches from it's serialized file (the
     *  `branches` file).
     * @return The container (a map) of all branches.
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getAllBranches() {
        HashMap<String, String> result
                = Utils.readObject(BRANCHES, HashMap.class);
        return result;
    }

    /** Get the commit the Head is associated with, no matter if
     * it's pointing at a branch or directly at a commit.
     * @return The commit object Head is associated with.
     */
    public static Commit getCurrentCommit() {
        String name = getHead();
        HashMap<String, String> allB = getAllBranches();
        if (allB.keySet().contains(name)) {
            String commitSha = allB.get(name);
            return Commit.getCommitObject(commitSha);
        }
        return Commit.getCommitObject(name);
    }



    /** Advance the branch.
     * If the head is pointing to the branch then it's considered
     * moved together.
     * @param bName Name of the new branch.
     * @param cName The sha1 name of the commit the branch is
     * going to point to.
     */
    public static void advanceBranch(String bName, String cName) {
        _allBranches = getAllBranches();
        _allBranches.put(bName, cName);
        persistence();
    }



    /** Get the Head pointer from persisted file in .gitlet repo.
     * @return Whatever the Head pointer is pointing to.
     */
    public static String getHead() {
        return Utils.readContentsAsString(HEAD);
    }

    /** Remove branch named b. Assume it exists. */
    static void removeBranch(String b) {
        _allBranches = getAllBranches();
        _allBranches.remove(b);
        persistence();
    }

    /** Get the Commit ID the Head is pointing to. Assume it's not
     * currently pointing to a commit (So it's now pointing to a branch).
     * @return The sha1 ID of the commit.
     */
    public static String getHeadID() {
        String name = getHead();
        HashMap<String, String> allB = getAllBranches();
        return allB.get(name);
    }


    /** The head pointer of the current gitlet repository.
     *  Most of the time it's pointing to a branch name.
     *  If point to a commit it's called "detached head state".
     */
    private static String _head;

    /** All the branch names that are currently in use.
     *  Key is the branch's name.
     *  Value is the commit the branch is pointing to.
     * */
    private static HashMap<String, String> _allBranches = new HashMap<>();


}

