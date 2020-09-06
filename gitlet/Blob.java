package gitlet;

import java.io.File;

/** A blob representation. A blob contains
 * the content of a file that has been added to the
 * staging area.
 * @author Heming Wu
 */
public class Blob {
    /** Current working directory. */
    static final File CWD = new File(".");

    /** Serialize the file FILENAME's content into a blob. */
    public Blob(String filename) {
        try {
            _name = filename;
            File file = Utils.join(CWD, filename);
            assert file.exists();
            _content = Utils.readContentsAsString(file);
            _byteContent = Utils.serialize(_content);
        } catch (AssertionError a) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }



    /** Get readable String content of the file.
     * @return Content of the file.
     * */
    String getContent() {
        return _content;
    }

    /** Get the serialized content of the file.
     * @return Serialized content.
     * */
    byte[] getSerialized() {
        return _byteContent;
    }

    /** Get name of the tile.
     * @return name of the file.
     * */
    String getName() {
        return _name;
    }


    /** Name of the file. */
    private String _name;
    /** Content of the file. */
    private String _content;
    /** Serialized content of the file. */
    private byte[] _byteContent;

}

