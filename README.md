# WrikeTrelloSync

A simple tool that provides bidirectional synchronization between Trello cards and Wrike tasks.

At the moment, it only synchronizes the title, description, column, position and assigned users.

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
* Automatically register the webhook
* Synchronize positions back to Trello properly

## Implementation Notes
* If you're running it for the first time, all existing cards will be created on the other platforms automatically.
* Trello cards are always archived instead of being deleted.

## Contributors
* [Guilherme Chaguri](https://github.com/Guichaguri)