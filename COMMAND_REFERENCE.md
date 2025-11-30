# Churn Command Reference - Updated for v1.1

## Overview

The command system has been completely refactored for better usability. Commands are now in English with a modular subcommand structure, allowing per-parameter configuration and settings persistence.

## Quick Start

```bash
# Show help
/churn

# Set configuration parameters
/churn world overworld
/churn radius 50
/churn threads 8

# Check your settings
/churn settings

# Start extraction with current settings
/churn start
```

## Command Categories

### Help & Information

| Command | Description |
|---------|-------------|
| `/churn` | Show help (same as `/churn help`) |
| `/churn help` | Detailed help message |
| `/churn settings` | Display current configuration |
| `/churn reset` | Reset all settings to defaults |

### Configuration Commands

These commands set individual parameters:

| Command | Range | Description |
|---------|-------|-------------|
| `/churn world <name>` | - | Set target world (overworld, nether, end, or custom) |
| `/churn radius <value>` | 1-2000 | Set chunk extraction radius |
| `/churn threads <value>` | 1-32 | Set number of worker threads |
| `/churn output <path>` | - | Set output directory path |
| `/churn minTps <value>` | 0-20 | Set minimum server TPS threshold |
| `/churn format <type>` | json/csv | Set output format |
| `/churn option <key> <value>` | - | Generic option setter |

### Execution Commands

| Command | Description |
|---------|-------------|
| `/churn start` | Start extraction with current settings |
| `/churn start <world> <radius> [options]` | Start with specific parameters (backward compatible) |
| `/churn status` | Show extraction progress |
| `/churn status json` | Show progress in JSON format |

### Job Management

| Command | Description |
|---------|-------------|
| `/churn pause` | Pause current extraction (creates checkpoint) |
| `/churn resume` | Resume from last checkpoint |
| `/churn resume <path>` | Resume from specific checkpoint file |
| `/churn cancel` | Cancel current extraction |

### Post-Processing

| Command | Description |
|---------|-------------|
| `/churn postprocess <path>` | Process extracted chunks to CSV |
| `/churn clean-checkpoints` | Remove all checkpoint files |
| `/churn clean-checkpoints <path>` | Remove checkpoints from specific path |

## Configuration Settings

### Default Values

- **World:** overworld
- **Radius:** 10 chunks
- **Threads:** 4
- **Output Path:** churn_output
- **Checkpoint Path:** churn_checkpoints
- **Minimum TPS:** 15.0
- **Output Format:** json
- **Fast Mode:** OFF

### Setting Persistence

Settings are stored per-player and persist for the session. They are used automatically when you run `/churn start` without parameters.

## Usage Examples

### Basic Extraction

```bash
# 1. Configure settings
/churn world overworld
/churn radius 25
/churn threads 6

# 2. Review settings
/churn settings

# 3. Start extraction
/churn start
```

### Monitor Progress

```bash
# Check status every few seconds
/churn status

# Get JSON format for parsing
/churn status json
```

### Pause and Resume

```bash
# Pause extraction (saves checkpoint)
/churn pause

# Do other things...

# Resume later
/churn resume
```

### Different Worlds

```bash
# Extract from Nether
/churn world nether
/churn radius 15
/churn start

# Pause, then switch
/churn pause
/churn world end
/churn radius 20
/churn start
```

### Custom Output Location

```bash
/churn output /path/to/custom/output
/churn format csv
/churn start
```

### Export to CSV

After extraction completes:

```bash
/churn postprocess ./churn_output
```

This reads all chunk JSON files and generates a CSV file for analysis.

## Auto-Completion

The command system supports Brigadier auto-completion:

- **Worlds:** Tab completes `overworld`, `nether`, `end`, custom worlds
- **Radius:** Tab suggests common values (16, 32, 64, 128, 256, 512)
- **Format:** Tab completes `json`, `csv`
- **Status:** Tab completes `json`, `plain`

Example: `/churn world [TAB]` shows world suggestions

## Performance Tuning

### For Fast Extraction

```bash
/churn threads 12
/churn minTps 10.0
/churn option fastMode true
/churn start
```

### For Low-Impact on Server

```bash
/churn threads 2
/churn minTps 18.0
/churn start
```

### Memory-Efficient

```bash
/churn option verbose false
/churn format json
/churn start
```

## Generic Option Setter

For advanced configuration, use `/churn option <key> <value>`:

| Key | Values | Effect |
|-----|--------|--------|
| `world` | string | Same as `/churn world` |
| `radius` | 1-2000 | Same as `/churn radius` |
| `threads` | 1-32 | Same as `/churn threads` |
| `output` | path | Same as `/churn output` |
| `minTps` | 0-20 | Same as `/churn minTps` |
| `format` | json/csv | Same as `/churn format` |
| `verbose` | true/false | Enable verbose logging |
| `fastMode` | true/false | Enable fast mode (higher load) |

Example:
```bash
/churn option threads 8
/churn option minTps 15.5
/churn option verbose true
```

## Backward Compatibility

The old command syntax is fully supported:

```bash
# Old style - still works!
/churn start myworld 50 {"threads": 8}
/churn start nether 25 @config.properties
```

This starts extraction immediately without modifying current settings.

## Error Handling

Invalid parameters show clear error messages:

```
/churn radius 5000
→ §cInvalid radius: 5000 must be 1-2000

/churn minTps invalid
→ §cInvalid TPS value: invalid (must be 0.0-20.0)

/churn format pdf
→ §cInvalid format: pdf (must be 'json' or 'csv')
```

## Tips & Tricks

### Save a Configuration

Use `/churn settings` to see current configuration, then document it for later use:

```bash
# Note current settings
/churn settings
→ Output: ./churn_output | Radius: 50 | Threads: 8 | MinTPS: 15.0

# Recreate later
/churn option output ./churn_output
/churn option radius 50
/churn option threads 8
/churn option minTps 15.0
```

### Batch Processing Multiple Worlds

```bash
# Process overworld
/churn world overworld
/churn radius 100
/churn start
# Wait for completion...

# Process nether
/churn world nether
/churn radius 50
/churn start
# Wait for completion...

# Process end
/churn world end
/churn radius 30
/churn start
```

### Reset to Defaults

If settings get messed up:

```bash
/churn reset
/churn settings
```

## Troubleshooting

**Q: Settings not persisting between server restarts?**
A: Settings are per-session. Re-set them after restart using `/churn settings` or `/churn option` commands.

**Q: Want to use old command syntax?**
A: Still fully supported: `/churn start world radius [options]`

**Q: Can I run multiple extractions?**
A: No - one at a time. Pause current, then start a new one with different settings.

**Q: How do I know if my command worked?**
A: Check `/churn settings` after setting a value to confirm it changed.

## Command Help Summary

```
/churn - shows help
/churn help - detailed help
/churn settings - show current settings
/churn reset - reset to defaults

/churn world <name> - set world (overworld, nether, end)
/churn radius <value> - set radius (1-2000 chunks)
/churn threads <value> - set threads (1-32)
/churn output <path> - set output directory
/churn minTps <value> - set minimum TPS (0-20)
/churn format <json|csv> - set format
/churn option <key> <value> - set any option

/churn start - start with current settings
/churn start <world> <radius> [options] - start directly

/churn status - show progress
/churn pause - pause extraction
/churn resume - resume from checkpoint
/churn cancel - cancel extraction
/churn postprocess <path> - export to CSV
/churn clean-checkpoints - remove checkpoints
```

---

**Note:** All commands require operator permissions on the server.
