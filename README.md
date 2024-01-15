# SimpleWorldReset

## Overview
SimpleWorldReset is a Minecraft (PaperMC) plugin designed to manage world resetting. On server shutdow the specified worlds are deleted and then restored form the specified backup files on server startup.

## Features
- **Automatic World Deletion**: Deletes configured worlds upon server shutdown.
- **World Restoration**: Restores worlds from specified backup files on server startup.

## Configuration
To use SimpleWorldReset, modify the `config.yml` file. Here's an example configuration:

```yml
# Worlds placed here will be deleted on shutdown and restored on startup.
worlds:
  0:
    name: "example_world" # Name of the world.
    restore:
      file: "example_world.tar.gz" # Backup file to restore from.
```
## Backup Files Notes

The file name of the backup file must be specified in the `restore.file` field. The backup file must be placed in the root directory of the server. The backup file must be a `.tar.gz` file. The backup file must contain a folder with the same name as the world.