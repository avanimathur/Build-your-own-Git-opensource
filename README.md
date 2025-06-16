# Build-Your-Own-Git

This project is a Java-based implementation of Git, developed as part of the "Build Your Own Git" challenge by [Codecrafters](https://codecrafters.io). This project aims to deepen understanding of Git's internal mechanics by recreating some of its core functionalities from scratch.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

- **Repository Initialization**: Initialize a new Git repository.
- **Commit Creation**: Stage changes and create commits.
- **Repository Cloning**: Clone existing public repositories.
- **Object Management**: Handle Git objects such as blobs, trees, and commits.

## Getting Started

### Installation

1. Clone the Repository:
   bash
   git clone https://github.com/avanimathur/Build-your-own-Git-opensource.git
   cd Git-CodeCrafter
Build the Project: Ensure you have Java (JDK 11 or higher) installed. Run the following command to compile the project:
bash
javac -d bin src/*.java
Run the Program: Execute the custom Git implementation using:
java -cp bin Main

Usage Examples
Below are step-by-step examples demonstrating how to use the basic Git operations provided by this custom Git implementation. These examples assume you have built and are running the program as described above.

1. Initializing a New Repository (init)
The init command creates a new Git repository by setting up the necessary directory structure (e.g., .git folder).

Command:

bash
java -cp bin Main init

Expected Output:
Initialized empty Git repository in /current/directory/.git/

What Happens:

A .git directory is created in your current working directory.
Essential subdirectories (e.g., objects, refs) and files (e.g., HEAD) are set up.

Next Steps:
You can now start adding files and making commits in this repository.

2. Creating a Commit (commit)
The commit command records changes to the repository by creating a commit object. First, you need to stage files, then commit them.

Step-by-Step:

Create a File:
Create a sample file in your repository:

bash
echo "Hello, Git!" > example.txt
Stage the File:
Use the add command to stage example.txt for committing:

bash
java -cp bin Main add example.txt
Expected Output:
Added example.txt to index

Create a Commit:
Commit the staged changes with a message:

bash
java -cp bin Main commit -m "Add example.txt"

Expected Output:
[master (root-commit) abc1234] Add example.txt
1 file changed, 1 insertion(+)
create mode 100644 example.txt

What Happens:

A blob object is created for example.txt.
A tree object represents the directory structure.
A commit object is created, linking to the tree and including your message.
3. Cloning a Repository (clone)
The clone command copies an existing public Git repository to your local machine.

Command:
Clone a sample public repository (replace <repository-url> with a real public repo URL):

bash
java -cp bin Main clone <repository-url> my-cloned-repo
Example:

bash
java -cp bin Main clone https://github.com/example/public-repo.git my-cloned-repo
Expected Output:
Cloning into 'my-cloned-repo'...
remote: Fetching objects: 100% (50/50), done.
Repository cloned successfully to /current/directory/my-cloned-repo

What Happens:

The repository's objects (blobs, trees, commits) are downloaded.
A new directory (my-cloned-repo) is created with the repository's contents.
The working directory is set up with the latest commit's files.
Next Steps:
Navigate to the cloned repository and start working:

bash
cd my-cloned-repo

Tips for Beginners
Run Commands in the Correct Directory: Ensure you’re in the project’s root directory or the repository directory when running commands.
Check the .git Folder: After running init, explore the .git folder to see how Git organizes objects and references.
Experiment Safely: Create test directories to practice these commands without affecting real projects.
Error Messages: If you encounter errors, check that files exist or the repository URL is valid.
Contributing
We welcome contributions! To get started:

Fork the repository.
Create a new branch (git checkout -b feature/your-feature).
Make your changes and commit them (git commit -m "Add your feature").
Push to your fork (git push origin feature/your-feature).
Open a pull request.
Please ensure your code follows the project’s style guidelines and includes tests where applicable.

License
This project is licensed under the MIT License. See the LICENSE file for details.

