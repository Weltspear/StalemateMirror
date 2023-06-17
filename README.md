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
- [ ] Hardware acceleration (?)
- [ ] Add Options menu
## `v0.3.x a` TODO:
- [ ] Planes
  - [x] AirUnit and packet stuff related to it
  - [X] Fighter
  - [X] Heavy fighter
  - [X] CAS bomber
  - [X] Strategic bomber
  - [X] Anti-Air
  - [X] Transport Plane
  - [X] An icon above a unit signalising that there's a plane over it
  - [X] Para-dropping
  - [ ] Textures
- [ ] Terrain buffs and debuffs
- [ ] Unit experience
- [x] Minimap
  - [x] Initial minimap implementation
  - [x] Make units on minimap blink when attacked
- [X] Better button hover highlighting
- [X] Button press animation
- [X] Make the entire game use one JFrame
- [x] Unit flags
- [X] Remove entrenchment when unit killed another unit and moved
- [X] Check how does multiplayer with more than 2 players work
  - [X] If there is a player lobby with more than two players don't terminate the game
- [X] Return to old supply system (Buildings don't have supply)
- [X] Increase SupplyStation range
- [x] Protocol refactor
- [x] Cache uname monogram
- [X] BUG: AirUnits are over ground units when in ground mode
- [X] BUG: Units spawning on each other
- [ ] Improve map loading
  - [ ] Make MapLoader call constructor which takes Game parameter
  - [ ] Make MapLoader not crash the game if it failed to load map
- [X] Reduce amount of supply of truck
- [X] Increase SupplyStation supply gen
- [ ] Make FortressAI deal with AttackButton move
- [X] Fix incorrect font at tooltip
- [X] Unit queue "overflow" bug
- [X] Weird repair overheal bug reducing hp
- [X] Rebalance fortification
- [ ] Rename Fortress to mass assault
- [X] Fix tankette alignment
- [X] Rounding error when selecting with selector
  - [X] Standard selector broken when offset is there   
- [ ] More QOL
  - [X] Highlight deployment point when mouse pointer is on change deployment point button
  - [X] Shift unit selection on certain conditions
- [ ] Fix special team repr big unit texture
- [ ] Add replays
- [ ] Campaign mode
  - [ ] Add possibility of creating own player lose conditions
## Project status
I commit to this repository from time to time. And this is very W.I.P.

## Official mirrors
Codeberg: https://codeberg.org/Weltspear/StalemateMirror \
GitHub: https://github.com/Weltspear/StalemateMirror 

Note: If you want to contribute, send your issues/merge requests to main repository at Gitlab: \
https://gitlab.com/weltspear/stalemate-pub-repo


