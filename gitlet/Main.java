package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Heming Wu
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws Exception {
        Commander c = new Commander(args);
        String command = args[0];
        switch (command) {
        case "init":
            c.init();
            break;
        case "add":
            c.add();
            break;
        case "commit":
            c.makeCommit();
            break;
        case "checkout":
            c.checkout();
            break;
        case "rm":
            c.rm();
            break;
        case "branch":
            c.branch();
            break;
        case "rm-branch":
            c.rmBranch();
            break;
        case "find":
            c.find();
            break;
        case "log":
            c.log();
            break;
        case "global-log":
            Commit.printGlobal();
            break;
        case "status":
            c.status();
            break;
        case "reset":
            c.reset();
            break;
        default:
            System.out.println("No command with that name exists");
            System.exit(0);
        }
    }

}
