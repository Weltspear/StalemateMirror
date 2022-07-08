# Stalemate Game
This is a turn-based strategy game.

## Support
You can report issues in issue tracker.

## Contributing
Contributions are welcomed.

## Building
This is a gradle project, so you can import it into IntelliJ IDEA and simply build it.

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
## `v0.3a` TODO:
- [x] Rework lobby system
- [ ] Swingify UI
  - [x] Remove custom UI in main menu and connection screen
  - [ ] Add Options menu
  - [x] Reworked lobby selection screen
  - [x] Add Error messagebox if client disconnects from server for some reason
- [x] Add `AssetLoader`
- [ ] Add packet version
- [x] Add credits
- [x] Replace raw `HashMap`s with `EntryTable`s (only partial replacement was needed).
- [x] Revision of code in `Lobby`
  - [x] Lobby itself
  - [x] Player
  - [x] Add connection termination cause
- [x] More `ReetrantLock`s everywhere
- [ ] `LobbySelectMenu` should show server's description

## Project status
I commit to this repository from time to time. And this is very W.I.P.

