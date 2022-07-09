# Stalemate Game
This is a turn-based strategy game.

## Support
You can report issues in issue tracker.

## Contributing
Contributions are welcomed.

## Building
In order to build jar and executables run gradle task `releaseBuild`. If you just want to run Stalemate execute gradle 
task `runClient` or `runServer`. 

## Credits
Weltspear (Dev & Sprites)\
SP7 (Sprites)

## License
See [NOTICE](NOTICE.md) and [LICENSE](LICENSE)

## TODO
- [ ] Make a tutorial on how to play this game.
- [x] Replace `System.out.println` with a logger.
- [x] Make compression `Base64` handle unexpected things.
- [x] Correct `ConnectionHandler` saying wrong ip after user connects.
- [ ] Hardware acceleration (?)
- [ ] Add Options menu
- [ ] If there is a player lobby which more than two players don't terminate the game
## `v0.3a` TODO:
- [x] Rework lobby system
- [x] Swingify UI
  - [x] Remove custom UI in main menu and connection screen
  - [x] Reworked lobby selection screen
  - [x] Add Error messagebox if client disconnects from server for some reason
- [x] Add `AssetLoader`
- [x] Add packet version
- [x] Add credits
- [x] Replace raw `HashMap`s with `EntryTable`s (only partial replacement was needed).
- [x] Revision of code in `Lobby`
  - [x] Lobby itself
  - [x] Player
  - [x] Add connection termination cause
- [x] More `ReetrantLock`s everywhere
- [x] `LobbySelectMenu` should show server's description

## Project status
I commit to this repository from time to time. And this is very W.I.P.

