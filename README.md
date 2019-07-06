# wrikeTrelloSync

It provides bidirectional synchronization between Trello cards and Wrike tasks.

At the moment, it only synchronizes the title, description, column, and position.

## Usage
### Requirements
* Java 8
* Trello API key and API token
* Wrike API token

### Running
Grab the [latest version](https://github.com/Lince-Facens/wrikeTrelloSync/releases) and run it with `java -jar WrikeTrelloSync.jar`.

If the `config.json` file doesn't exist, it will run a configuration wizard.

## TODO
* Add synchronization for assigned users
* Fix card ordering updates

## Implementation Notes
* Trello cards are always be archived instead of deleting them.

## Contributors
* [Guilherme Chaguri](https://github.com/Guichaguri)