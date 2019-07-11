# WrikeTrelloSync

A simple tool that provides bidirectional synchronization between Trello cards and Wrike tasks.

At the moment, it only synchronizes the title, description, column, and position.

## Usage
### Requirements
* Java 8
* Trello API key and API token
* Wrike API token

### Running
Grab the [latest version](https://github.com/Lince-Facens/wrikeTrelloSync/releases).

If the `config.json` file doesn't exist, it will run a configuration wizard.

#### Diff Mode

Diff Mode looks for differences between the local cache and the current state of the platform, propagating the changes to other platforms.

Run it with `java -cp WrikeTrelloSync.jar com.guichaguri.wriketrellosync.DiffSync`.

You may also enable a timer to synchronize by setting its interval in minutes:
```bash
$ java -cp WrikeTrelloSync.jar -Dtimer.interval=30 com.guichaguri.wriketrellosync.DiffSync
```

#### WebHook Mode

WebHooks are triggered whenever a change is made, synchronizing it instantly.

The downside is that you need it to be running a server with a public IP address.

The webhook must be registered manually.

Run it with `java -cp WrikeTrelloSync.jar com.guichaguri.wriketrellosync.WebHookSync`. 

You may also pass the hostname, the port and enable diff mode timer:
```bash
$ java -cp WrikeTrelloSync.jar -Dwebhook.hostname=127.0.0.1 -Dwebhook.port=8091 -Dtimer.interval=30 com.guichaguri.wriketrellosync.WebHookSync
```

## TODO
* Add synchronization for assigned users
* Automatically register the webhook

## Implementation Notes
* If you're running it for the first time, only one of the platforms can be populated with cards, otherwise you'll end up with duplicates.
* Trello cards are always archived instead of being deleted.

## Contributors
* [Guilherme Chaguri](https://github.com/Guichaguri)