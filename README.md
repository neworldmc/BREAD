# BREAD
Block Redstone Event Analyse and Diagnose

## Commands
The base command is `/bread`.
- `/bread start` (Sponge) / `/bread run` (Spigot) - Start BREAD
- `/bread fast` - Start fast BREAD
- `/bread stop` - Stop running BREAD
- `/bread status` - View status of BREAD, and last BREAD result if it exists

## Permissions
- `bread.admin` - Allow admin to use BREAD. OPs have this permission by default.

## Build
This project uses Maven. Just clone a copy of this project to your storage and run `mvn clean package` to build.

The available plugins will be built in directory `sponge/target` and `spigot/target`.

## Metrics
This project uses bStats to collect data.

For Sponge, this function is disabled by default. You can enable it by typing `sponge metrics bread enable` on your server to support our work.

For Spigot, this function is enabled by default. If you don't want to send data to bStats, you can disable bStats by changing `plugins/bStats/config.yml`.

## Contributing
Just fork this project, modify your fork, and send a pull request to our project, like contributing on other projects on GitHub.
