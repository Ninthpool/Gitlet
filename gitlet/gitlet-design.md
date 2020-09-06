# Gitlet Design Document

**Name**: Heming Wu

## Classes and Data Structures

### Main Class

* Call methods

### Commit Class

* Create a commit by calling the constructor, which takes in commit messages,
and it should call a function that assigns time stamp.

* Should store branches name in a data structure (Maybe set?)

* Commit tree should be similar to a linkedList-tree structure so that it can go back
and forth but can also diverge by creating different branches. (Update: Probably
we only need the commit to point to a parent commit?)

* Reference to each commit is unique by the SHA-1 of it's content. So there should be
a method generating SHA-1. Let's make branches just as a String of SHA-1.

* Every time we create a commit, we basically cloned the HEAD pointer and modify it's content.

* This class seems to have too many functions; Should break it down.

* Creating a branch is like creating a new pointer.

* Branches are references to a commit, tags can refer to any object;
  But most importantly, the branch ref is updated at each commit. This means that whenever you commit, Git actually does this:
  a new commit object is created, with the current branch’s ID as its parent;
  the commit object is hashed and stored;
  the branch ref is updated to refer to the new commit’s hash.

* Having our metadata consist only of a timestamp and log message. A commit,
therefore, will consist of a log message, timestamp, a mapping of
file names to blob references, a parent reference, and (for merges)
a second parent reference.


### Blob Class

* Saves all the messages in a commit for later serialization.

* Blob should be connected to the commit in some sort of ways.

### Stage Class (Maybe I don't need this)

* Not sure if I really want to have this class. But if I do,
it should be representing the staging area and update if a file
is removed from the staging area.


### Repository Class (Maybe I don't need this)

* Represent each repository as a class, containing paths and contents, etc.

* Need to find a way for the class to handle paths and create directories.

* Branches should be maintained here in repository class?

### Comander Class

* A class that act like a central control room to facilitate command line commands
to do work among different class.

* It should have corresponding methods of `commit`, `log`, `add`, `merge`, etc.

* Don't do anything to specific; This is just a class to call commands from other
classes.

### StagingArea Class

* Represent the staging area of current working directory. We need
this to point to the files we are going to track.

* Should have methods that can work with the current working directory,
like removing files, writing files, writing blobs to certain directory, etc.

### Branch class

* Contains the Head pointer(most of the time pointing to a branch).

* Also contains the collection of branch names.

* Has a method that can create a new branch.

* Has a method that can delete the branch.

* Has a method that can move the HEAD pointer.

* `_allBranches` should be serializable and persisted, so does the head pointer.

* Maybe we can make this class to just contain the static methods without a constructor.

* Has a static method that can move the branch and head pointer together.

* Should have methods that can check what is the current commit (get it from Head) so that
it's easier to check current state.

* A head pointer either points to a branch (usually) or points to a commit (I don't know if we
need to account for this situation in this project).
## Algorithms

### Commit Class

* Updating and creating a reasonable commit tree by calling methods in
the Commit class.

* Keep track of what files the commit is tracking in `_content`, which contains
the file's sha1 name.

* Has an algorithm that can print all commits (in a thread) out.

* Has an algorithm that can print all commits that have ever been made out,
but the order is not guranteed.


### Blob Class

* Should be having a method to check hos the content of a file is changing so
that we can indicate if there's changes and if there's a merge conflict.

* Should somehow has a way to check if a file's content has changed or not (Nope).

### StagingArea Class

* Keep track of what files are in the staging area and what files are not.

* Record every file that are tracked.



## Persistence

* All the commits object should be serializable and saved in `.gitlet/objects`

* All the head pointers (they are commit objects themselves) should be serializable
so that we can go back to previous commits by usging `checkout`

* Content of the file (Blob) should also be serializable so that it's easier for
us to revert back to a previous version.

* Each commit files should be unique. The serialized byte code file should have a
SHA-1 name.

* Store what commit the HEAD is pointing
to in `.gitlet/HEAD/banche_name(like Master)`

* Store the file in the staging area in the **file** `Index`

* Most important files are `objects` the objects we are tracking, and `refs`
the name of the objects.

* Need to keep track of staging area and all tracked files.

* How to remove file? locate the current commit using HEAD, and remove the file
from it's `_content`.

* Static method in the persisted class need to be loaded from disc.
But I don't think we need to fish the file up from disc if it's in an instance method.

### `add`:
1.  Blob object is created and saved in `object` folder
2.  Make an entry in the `Index` file (represents the staging area). `Index` is a
list of every file that Gitlet has been told to keep track of. The entry to the `Index`
contains: the path to the file and the hash to the content of the file (in Object directory).

### `commit`:
1. It's an object itself, contains tree, timestamp, author, "pointer" to the blob, etc.
2. The tree object contains blobs object (in serialized form).
3. There's a `HEAD` file with path `.git/HEAD` which normally contains the reference (basically
String) of the path of a branch name. But it can be pointed to a commit too, which results in a
detached HEAD state.
4. Only store the difference, so if a blob didn't change, the new commit still point to that.

### `branch`:
1.  Create a file named `Branches` with a path `.gitlet/Branches` that stores all the branches
created.
2. Basically, it first finds out what the HEAD is pointing to, and then create a file, put whatever the
file is pointing to inside that file.
