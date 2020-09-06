package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


/** A central command room to facilitate Gitlet commands.
 * @author Heming Wu
 */
public class Commander {
    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));
    /** Location of .gitlet directory. Assume it already exists.*/
    static final File GITLET = Utils.join(CWD, ".gitlet");
    /** Location of Object directory. */
    static final File OBJECT = Utils.join(GITLET, "Object");
    /** Location of the StagingArea file (In git it's called INDEX). */
    static final File STAGE = Utils.join(GITLET, "StagingArea");
    /** Location of the Unstaged Area. */
    static final File UNSTAGE = Utils.join(GITLET, "UnstagedArea");


    /** Initialize a commander object. Save operands in _operand
     * @param args The command line arguments.
     * */
    public Commander(String[] args) {
        if (args == null) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        _command = args[0];
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                _operands.add(args[i]);
            }
        }
    }

    /** Handle init command. */
    public void init() throws IOException {
        if (GITLET.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        GITLET.mkdir();
        STAGE.createNewFile();
        UNSTAGE.createNewFile();
        StagingArea.persistence();
        new Commit("initial commit", null);
    }

    /** Handle the add command. Store the file location; Track it's content */
    public void add() throws Exception {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() == 0) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        for (String s : _operands) {
            File targetFile = Utils.join(CWD, s);
            if (!targetFile.exists()) {
                System.out.printf("File does not exist.");
                System.exit(0);
            }
            if (StagingArea.getUnStaged().keySet().contains(s)) {
                StagingArea.getUnStaged().remove(targetFile.getName());
                StagingArea.persistence();
                continue;
            }
            if (StagingArea.checkUnchangedContent(targetFile)){
                continue;
            }
            StagingArea.saveFile(s);
        }
    }

    /** Handle the commit command. */
    public void makeCommit() throws IOException {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (_operands.size() == 0
            || _operands.get(0).equals("")) {
            System.out.println("Please enter a commit message");
            System.exit(0);
        }
        if (!StagingArea.hasStagedFile() && StagingArea.getUnStaged().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        String message = _operands.get(0);
        new Commit(message, Branch.getHeadID());
        StagingArea.clear();
    }

    /** Handle the rm command. If a file is tracked in the current head
     * commit and you call gitlet rm on that file, then the file would be
     * untracked (only for the purpose of constructing a child commit, so
     * put it in an `unstaged` area don't serialize the commit again) and also
     * deleted form the working directory. If you had simply just added the
     * file to staging area and hadn't committed it yet, then rm would only
     * unstage it and you would act as though nothing has changed, since the
     * file wasn't tracked and gitlet continues to pretend it isn't.
     */
    public void rm() throws IOException {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String filename = _operands.get(0);
        File targetFile = Utils.join(CWD, filename);
        Commit cCommit = Branch.getCurrentCommit();
        if (StagingArea.getStagedFile().keySet().contains(filename)) {
            StagingArea.getStagedFile().remove(filename);
            StagingArea.persistence();
            return;
        } else if (cCommit.getContent().keySet().contains(filename)) {
            String blobPath = cCommit.getContent().get(filename);
            StagingArea.unstage(filename, blobPath);
            targetFile.delete();
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }
    /** Make a new branch but do NOT point Head to it yet. */
    public void branch() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branchName = _operands.get(0);
        if (Branch.getAllBranches().keySet().contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String shaName = Branch.getAllBranches().get(Branch.getHead());
        Branch.makeBranch(branchName, shaName);
    }

    /** Deletes the branch with the given name. This only means to
     * delete the pointer associated with the branch; it does not
     * mean to delete all commits that were created under the branch,
     * or anything like that.
     */
    public void rmBranch() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String branchName = _operands.get(0);
        if (!Branch.getAllBranches().keySet().contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String cBranch = Branch.getHead();
        if (branchName.equals(cBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        Branch.removeBranch(branchName);
    }

    /** Handle the checkout command. */
    public void checkout() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() == 0 || _operands.size() > 3
            || (_operands.size() == 2 && !_operands.get(0).equals("--"))
            || (_operands.size() == 3 && !_operands.get(1).equals("--")) ){
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (_operands.size() == 1) {
            String branchName = _operands.get(0);

            if (!Branch.getAllBranches().containsKey(branchName)) {
                System.out.println("No such branch exists");
                System.exit(0);
            }
            if (branchName.equals(Branch.getHead())) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            String bCommitSha = Branch.getAllBranches().get(branchName);
            Commit bCommit = Commit.getCommitObject(bCommitSha);

            File[] allFiles = CWD.listFiles();
            for (File f : allFiles) {
                if (f.getName().equals(".gitlet")) {continue;}
                String fileName = f.getName();
                String fileContent = Utils.readContentsAsString(f);
                Commit cCommit = Branch.getCurrentCommit();
                if (!bCommit.getContent().containsKey(fileName)) {
                    if (!cCommit.getContent().containsKey(fileName)) {
                        System.out.println("There is an untracked file "
                                + "in the way; delete it, or add "
                                + "and commit it first.");
                        System.exit(0);
                    } else {
                        cCommit.rmFileCWD(fileName);
                    }
                } else {
                    String bCommitFileSha = bCommit.getContent().get(fileName);
                    String bCommitFileContent =
                            StagingArea.getContentFromSha(bCommitFileSha);

                    if (!bCommitFileContent.equals(fileContent)
                            && !cCommit.getContent().containsKey(fileName)) {
                        System.out.println("There is an untracked file "
                                + "in the way; delete it, or add "
                                + "and commit it first.");
                        System.exit(0);
                    } else {
                        cCommit.rmFileCWD(fileName);
                    }
                }
            }

            bCommit.writeToCWD();
            Branch.moveHead(branchName);
        }
        if (_operands.size() == 2) {
            String fileName = _operands.get(1);
            Commit tCommit = Branch.getCurrentCommit();
            if (!tCommit.getContent().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            tCommit.writeToCWD();
        }
        if (_operands.size() == 3) {
            handleThree();
        }
    }

    /** Handle the case where there are three operands in checkout command. */
    private void handleThree() {
        String shortSha = _operands.get(0);
        String fileName = _operands.get(2);
        Commit targetCommit = Commit.getCommitObject(shortSha);
        if (!targetCommit.getContent().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobID = targetCommit.getContent().get(fileName);
        targetCommit.writeFileToCWD(fileName);
    }

    /** Starting at the current head commit, display information about each
    commit backwards along the commit tree until the initial commit, following
     the first parent commit links, ignoring any second parents found in merge
     commits. */
    public void log() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() > 0) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Commit currentCommit = Branch.getCurrentCommit();
        byte[] serializedCommit = Utils.serialize(currentCommit);
        String shaID = Utils.sha1(serializedCommit);
        Commit.printAll(shaID, currentCommit);
    }

    /** Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids out
     * on separate lines.
     */
    public void find() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String message =  _operands.get(0);
        Commit.findMessage(message);
    }

    /** Show current status. */
    public void status() throws IOException {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() > 0) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String cBranch = Branch.getHead();
        System.out.println("=== Branches ===");
        Object[] sortedBranches = Branch.getAllBranches().keySet().toArray();
        Arrays.sort(sortedBranches);
        for (Object s : sortedBranches) {
            if (s.equals(cBranch)) {
                System.out.printf("*%s\n", cBranch);
            } else {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        if (STAGE.exists()) {
            Object[] sortedStagedFiles =
                    StagingArea.getStagedFile().keySet().toArray();
            Arrays.sort(sortedStagedFiles);
            for (Object s : sortedStagedFiles) {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (UNSTAGE.exists()) {
            Object[] sortedUnStagedFiles =
                    StagingArea.getUnStaged().keySet().toArray();
            for (Object s : sortedUnStagedFiles) {
                File tFile = Utils.join(CWD, (String) s);
                if (!tFile.exists()) {
                    System.out.println(s);
                }
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        //FIXME

        System.out.println();
        System.out.println("=== Untracked Files ===");
        //FIXME
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     */
    public void reset() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (_operands.size() > 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String targetId = _operands.get(0);
        Commit tarCommit = Commit.getCommitObject(targetId);
        Commit curCommit = Branch.getCurrentCommit();
        for (File file : CWD.listFiles()) {
            if (!curCommit.getContent().containsKey(file)
                    && tarCommit.getContent().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (String s : curCommit.getContent().keySet()) {
            if (!tarCommit.getContent().containsKey(s)) {
                Commit.rmFileCWD(s);
            }
        }
        for (String s : tarCommit.getContent().keySet()) {
            tarCommit.writeFileToCWD(s);
        }
        String curBranch = Branch.getHead();
        Branch.advanceBranch(curBranch, targetId);
        StagingArea.clear();
    }

    /** Command line commands. */
    private String _command;

    /** File name for add command and commit command. */
    private ArrayList<String> _operands = new ArrayList<>();


}
