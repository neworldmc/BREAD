# BREAD
Block Redstone Event Analyse and Diagnose

## Commands
The base command is `/bread`.

- `/bread start` - Start BREAD
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

The available plugins will be built as `sponge/target/BREAD-Sponge.jar` and `spigot/target/BREAD-Spigot.jar`.

## Metrics
This project uses bStats to collect data.

For Sponge, this function is disabled by default. You can enable it by typing `/sponge metrics bread enable` on your server to support our work.

For Spigot, this function is enabled by default. If you don't want to send data to bStats, you can disable bStats by changing `plugins/bStats/config.yml`.

## Contributing
Just fork this project, modify your fork, and send a pull request to our project, like contributing on other projects on GitHub.
