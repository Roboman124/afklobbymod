# afklobby-Fabric

Adds the ability to set the location of an AFK lobby using coordinates. When a player does not move with their keyboard for the configured amount of time, they are taken to the lobby. When they move again, they are taken back to where they were.

Veteran users can still set their own custom lobby just like before with `/afk setlobby`. New users — or anyone who wants a ready-made build — can use the included bundled structures: the **"WAITING LOBBY"** by Twistermns and the **Concert Stage** by HD Studios. You can find their original builds on Planet Minecraft. If either creator is reading this and would prefer their build not be included in the mod, please open an issue on GitHub and we will remove it.

## Features

- `/afk setlobby x y z` - Sets the AFK lobby location.
- `/afk status` - Checks AFK status.
- Persistent AFK time tracking with a leaderboard.
- Daily ceremony with sounds, particles, and fireworks.
- Winner crown for the player with the most AFK time.
- LuckPerms prefix integration.
- Live client HUD showing the AFK leaderboard.
- `/afktracker` admin commands for managing AFK data.

## Supported Versions

This repository contains the **1.20.1** version at the root. Additional standalone ports are available in the `versions/` folder:

- `1.19.4`
- `1.20.4`
- `1.21`
- `1.21.1`
- `1.21.4`
- `1.21.5`

Each version is a standalone Gradle project with its own `build.gradle` and dependency versions.

## Download

Releases are published to [Modrinth](https://modrinth.com/mod/afklobbymod).

The Modrinth release marks **Fabric API**, **Cloth Config**, **owo-lib**, and **LuckPerms** as required dependencies so your launcher can auto-download them. LuckPerms is optional at runtime for the mod itself; if it is not installed, the LuckPerms prefix feature is skipped gracefully.

## Contributing

Submit issues or pull requests on GitHub.

(THIS WAS CODED WITH THE HELP OF AI)
