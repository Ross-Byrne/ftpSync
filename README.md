# Ftp Sync

A simple ftp file syncing program that will sync files of a specified age. Written in Java using JavaFX.

# Introduction
Ftp Sync is a program that syncs the files on a ftp server, into a seleted folder on the users computer. Files will only be synced if they are newer then the set file age limit. The default age limit is 6, so only files that where last modified 5 days ago will be downloaded. Files that have been downloaded once will not be downloaded again, even if the local synced copy is deleted. The program uses ftp on port 21 and in passive mode. This can not currently be changed.

# Use Case
The main use case of this program is the following:<br>
You need to regularly download new files from a ftp server that are uploaded at a regular interval, eg. every Thursday. You need to edit these files and use them for something, so keeping the originals is not necessary. Old files are not removed from the server.

### Problem
How can you regularly download the new files, without having to click through folders to avoid older files you don't need? You don't want to store old copies of the files or re-download them.

### Solution
Use this program as a tool to download files that are newer then a set number of days. This allows you to get all your new files, without having to search through many folders to find them or without having to re-download old files you no longer need.

## Development Setup

The development IDE is IntelliJ IDEA and the project uses maven.

To set up dependencies, run:
```
mvn clean install
```
Maven will create native builds for the platform you are on.
