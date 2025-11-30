# Churn Mod Changelog

## Version 1.1 (Latest)

### Major Changes
- **Command System Refactor:** Complete rewrite from Hungarian to English
- **Subcommand Architecture:** Individual parameter setting commands instead of single complex command
- **Settings Persistence:** Per-player configuration state between commands

### New Features
- Per-parameter configuration: `/churn world`, `/churn radius`, `/churn threads`, etc.
- Settings management: `/churn settings` and `/churn reset`
- Generic option setter: `/churn option <key> <value>`
- Brigadier auto-completion for commands and parameters
- Improved help system with categorized commands

### New Commands
```
/churn                           - Show help
/churn help                      - Detailed help
/churn settings                  - Display current settings
/churn reset                     - Reset to defaults
/churn world <name>              - Set target world
/churn radius <value>            - Set extraction radius
/churn threads <value>           - Set worker threads
/churn output <path>             - Set output directory
/churn minTps <value>            - Set TPS threshold
/churn format <json|csv>         - Set output format
/churn option <key> <value>      - Generic option setter
/churn start                     - Start with current settings
/churn start <world> <radius> [options] - Direct start (backward compatible)
/churn status                    - Show progress
/churn pause                     - Pause extraction
/churn resume                    - Resume from checkpoint
/churn cancel                    - Cancel extraction
/churn postprocess <path>        - Export to CSV
/churn clean-checkpoints         - Remove checkpoints
```

### Backward Compatibility
- Old syntax `/churn start world radius [options]` fully supported
- All existing functionality preserved
- JSON/CSV output formats unchanged
- Checkpoint system still works

### Documentation
- Added COMMAND_REFERENCE.md for comprehensive command documentation
- Updated README.md with new command examples
- Updated USAGE_GUIDE.md with new command workflows

### Technical Improvements
- ChurnSettings class for state management
- Better error handling for invalid parameters
- Clearer error messages in English
- Auto-completion suggestions
- Improved player UUID tracking

---

## Version 1.0 (Initial Release)

### Core Features
- ✅ ChunkExtractor: Anvil format parser for Minecraft world files
- ✅ NBTParser: Comprehensive Named Binary Tag decoder
- ✅ WorldNavigator: World directory discovery and validation
- ✅ Parallel Processing: Configurable worker threads with TPS protection
- ✅ Data Export: JSON and CSV formats
- ✅ Post-Processing: Aggregation and analysis pipelines
- ✅ Checkpoint System: Pause/resume with progress saving
- ✅ Progress Tracking: Real-time extraction metrics

### Components
- ChunkExtractor.java - Anvil format parsing
- NBTParser.java - NBT tag decoder
- WorldNavigator.java - World file discovery
- ChunkData.java - Data model
- OutputFormatter.java - Export formats
- Worker.java - Parallel processing
- GeneratorManager.java - Pipeline orchestration
- ChurnWorkQueue.java - Task queue
- MainThreadApplier.java - Main-thread result application
- RegionCheckpointManager.java - Checkpoint management
- ProgressLogger.java - Progress tracking
- TPSMonitor.java - Server TPS monitoring
- JobConfig.java - Configuration management

### Initial Commands (Hungarian)
- /churn start <world> <radius> [options]
- /churn status
- /churn pause / resume / cancel
- /churn postprocess <path>

### Build System
- Gradle 8.14
- Fabric Loom 1.13.3
- Minecraft 1.21
- Java 21

### Known Limitations
- Single extraction at a time
- Settings reset on server restart (per-session only)
- Advanced NBT fields not fully extracted
- No real-time streaming output

---

## Roadmap

### Planned for v1.2
- [ ] Persistent settings storage (database or config file)
- [ ] Biome and noise configuration extraction
- [ ] Block type enumeration with counts
- [ ] CRC32 validation for data integrity

### Planned for v1.3+
- [ ] Protocol Buffers output format
- [ ] Real-time streaming to external tools
- [ ] Multi-dimension simultaneous extraction
- [ ] Database backend support (PostgreSQL, SQLite)
- [ ] Web UI for monitoring

### Long-term
- [ ] Integration with mapping tools
- [ ] Automatic world backup
- [ ] Block property preservation
- [ ] Entity attribute extraction
- [ ] Dimension-specific optimizations

---

## Migration Guide (v1.0 → v1.1)

### Old Usage
```bash
/churn start myworld 50 {"threads": 8}
```

### New Usage (Option 1: Direct)
```bash
/churn start myworld 50 {"threads": 8}  # Still works!
```

### New Usage (Option 2: Recommended)
```bash
/churn world myworld
/churn radius 50
/churn threads 8
/churn start
```

### Benefits of New Approach
- Can verify settings with `/churn settings`
- Easier to manage multiple configurations
- Auto-completion support
- Better error messages
- Settings review before starting

---

## Bug Fixes & Improvements

### v1.0.1
- Fixed edge case in NBT parsing with nested arrays
- Improved error handling for missing world files
- Better memory efficiency in chunk queue

### v1.0.2
- Fixed concurrency issue in RegionCheckpointManager
- Improved TPS monitoring accuracy
- Better logging output

---

## Support & Contributing

For issues, feature requests, or contributions:
1. Check existing issues on GitHub
2. Provide detailed reproduction steps
3. Include server logs if applicable
4. Suggest improvements with examples

## License

See LICENSE file for details.

---

**Current Status:** Production Ready ✓
**Last Updated:** 2024-11-30
**Latest Version:** 1.1
**Minecraft:** 1.21
**Fabric Loader:** 0.16.10+
