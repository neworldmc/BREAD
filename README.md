# BREAD
[![CircleCI](https://circleci.com/gh/neworldmc/BREAD.svg?style=svg)](https://circleci.com/gh/neworldmc/BREAD)
[![Sponge Project Page](https://img.shields.io/badge/project--page-sponge-blue)](https://ore.spongepowered.org/NEWorld-Minecraft/BREAD)
[![Spigot Project Page](https://img.shields.io/badge/project--page-spigot-blue)](https://www.spigotmc.org/resources/bread.72428/)

*Block Redstone Event Analyse and Diagnose*

BREAD is a plugin for analysing redstone data in the server. With this plugin, you can use a simple command to check most of redstone updates in the whole server at a time, and get a readable analysis report.

## Commands
The base command is `/bread`.

- `/bread start` - Start BREAD
- `/bread semi-fast` - Start semi-fast BREAD
- `/bread fast` - Start fast BREAD
- `/bread stop` - Stop running BREAD
- `/bread status` - View status of BREAD, and last BREAD result if it exists

## Permissions
- `bread.admin` - Allow admin to use BREAD. OPs have this permission by default.

## Requirements
- For Sponge: Java 8, any server platform (this project doesn't support clients) which supports Sponge API 7.x
- For Spigot: Java 8-11, Spigot (or its derivatives) 1.13 (or higher versions)

## Build
This project uses Maven. Just clone a copy of this project to your storage and run `mvn clean package` to build. (Notice: Please run maven with Java 8, otherwise your build will fail.)

The output files are `sponge/target/BREAD-Sponge.jar` and `spigot/target/BREAD-Spigot.jar`.

## Metrics
This project uses bStats to collect data.

For Sponge, this function is disabled by default. You can enable it by typing `/sponge metrics bread enable` on your server to support our work.

For Spigot, this function is enabled by default. If you don't want to send data to bStats, you can disable bStats by changing `plugins/bStats/config.yml`.

## Contributing
Just fork this project, modify your fork, and send a pull request to our project, like contributing on other projects on GitHub.

## Chat
The official IRC channel is `#mcplugin-bread` on [irc.freenode.net](https://webchat.freenode.net).
