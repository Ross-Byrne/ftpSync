# Ftp Sync

A simple ftp file syncing program that will sync files of a specified age. Written in Java using JavaFX.

### Currently a work-in-progress.

# Intro
Ftp Sync is a program that syncs the files on a ftp server, into a seleted folder on the users computer. Files will only be synced if they are newer then the set file age limit. The default age limit is 6, so only files that where last modified 5 days ago will be downloaded. Files that have been downloaded once will not be downloaded again, even if the local synced copy is deleted.

## Setup

The development IDE is IntelliJ IDEA and the project uses maven.

To set up dependencies, run:
```
mvn clean install
```
Maven will create native builds for the platform you are on.
