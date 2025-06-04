# CarpetBotRestriction

Allows non-operator players to control only the carpet bots that they spawn and limits the bots they can spawn.

**NOTE:** You must have carpet installed and set **/carpet commandPlayer true** for the mod to work! This will allow non-op players to spawn bots. To set this perminantly, run **/carpet setDefault commandPlayer true**.

Includes LuckPerms support (optional)

## Config:
If LuckPerms is installed, using the `/cbr` command requires `carpetbotrestriction.config`.
* `/cbr defaultMaxBots [<number>]`: set the maximum bot limit.
* `/cbr removeOnDisconnect [<true/false>]`: if true, when a player leaves the server their bots will be removed.
* `/cbr player [<player name>] maxBots set [<number>]`: set the maximum bot limit for a specific player.
* `/cbr player [<player name>] maxBots unset`: player will revert to using the defaultMaxBots limit.

## LuckPerms permissions:
### Regular permissions:
* `carpetbotrestriction.user.create_own`: allows user to create a new bot (default: `true`).
* `carpetbotrestriction.user.manipulate_own`: allows user to manipulate/remove the bots they create (default: `true`).
* `carpetbotrestriction.user.shadow`: allows user to shadow (default: `true`).
### Admin permissions:
* `carpetbotrestriction.admin.create_unlimited`: allows user to create unlimited bots (default: ops level `2`).
* `carpetbotrestriction.admin.manipulate_all`: allows user to manipulate all bots, not just their own (default: ops level `2`).
* `carpetbotrestriction.admin.create_real`: allows user to create bots of players that have logged onto the server (default: ops level `2`).