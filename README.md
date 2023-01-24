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
## `v0.3.x a` TODO:
- [ ] Planes
  - [x] AirUnit and packet stuff related to it
  - [ ] Fighter
  - [ ] Heavy fighter
  - [ ] CAS bomber
  - [ ] Strategic bomber
  - [ ] Anti-Air
  - [ ] An icon above a unit signalising that there's a plane over it
- [x] Minimap
  - [x] Initial minimap implementation
  - [ ] Make units on minimap blink when attacked
- [ ] Make the entire game use one JFrame
- [x] Unit flags
- [ ] Terrain buffs and debuffs
- [ ] Remove entrenchment when unit killed another unit and moved
- [ ] Unit experience
- [ ] Check how does multiplayer with more than 2 players work
  - [ ] If there is a player lobby with more than two players don't terminate the game
- [ ] Return to old supply system (Buildings don't have supply)
- [ ] Increase SupplyStation range
- [x] Protocol refactor

## Project status
I commit to this repository from time to time. And this is very W.I.P.

