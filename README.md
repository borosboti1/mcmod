# Churn — Minecraft Chunk Reverse Engineering Mod

A Fabric mod for extracting, analyzing, and exporting Minecraft world chunk data in parallel. Churn provides a comprehensive solution for accessing the Anvil format region files, parsing NBT-encoded chunk data, and exporting results for analysis.

## Features

✅ **Chunk Extraction Engine**
- Reads Minecraft Anvil format region files (.mca)
- Extracts chunks by radius from any world
- Full support for Minecraft 1.21+ chunk format

✅ **NBT Parsing**
- Comprehensive Named Binary Tag (NBT) parser
- Supports all NBT tag types (compounds, arrays, lists)
- Extracts chunk metadata, entities, and block data

✅ **Parallel Processing**
- Configurable worker thread pool
- TPS-aware throttling to protect server performance
- Checkpoint system for pause/resume capability

✅ **Data Export**
- JSON output per chunk or combined
- CSV format for analysis and visualization
- Post-processing pipeline for data aggregation

✅ **World Navigation**
- Auto-detects Minecraft world directories
- Supports multiple world search patterns
- Validates world structure and accessibility

## Quick Start

### Installation

1. Download `churn-*.jar` from releases
2. Place in your Fabric mods directory
3. Requires Minecraft 1.21 with Fabric Loader 0.16.10+

### Basic Usage

```bash
# Extract all chunks within 10-chunk radius
/churn start myworld 10

# Check extraction progress
/churn status

# Pause for later resumption
/churn pause

# Resume from checkpoint
/churn resume ./churn_checkpoints/churn_last_job.meta

# Process results to CSV
/churn postprocess ./churn_output
```

For detailed usage instructions, see [USAGE_GUIDE.md](USAGE_GUIDE.md).

## Architecture

### Core Components

1. **ChunkExtractor** - Parses Anvil region files and extracts chunk NBT data
2. **NBTParser** - Comprehensive NBT decoder supporting all tag types
3. **WorldNavigator** - Locates and validates Minecraft world directories
4. **ChunkData** - Structured representation of extracted chunk information
5. **OutputFormatter** - Exports data to JSON and CSV formats
6. **Worker** - Background thread orchestrating extraction and serialization
7. **GeneratorManager** - Coordinates the entire extraction pipeline

### Data Flow

```
Command Input
    ↓
World Detection (Minecraft Server API)
    ↓
WorldNavigator (locate world files)
    ↓
ChunkExtractor (read region files, parse NBT)
    ↓
NBTParser (extract chunk data)
    ↓
Worker Threads (parallel processing)
    ↓
ChunkData (structured output)
    ↓
JSON/CSV Export
```

## Configuration

### Properties File

Create `extraction.properties`:

```properties
world=myworld
radius=50
threads=8
outputPath=./churn_output
minTps=15.0
checkpointPath=./churn_checkpoints
```

Then use: `/churn start world 50 @extraction.properties`

### Inline Parameters

```bash
/churn start myworld 25 {"threads": 6, "minTps": 14.5}
/churn start myworld 30 --threads 8 --minTps 15 --batch 32
```

## Performance

- **Default Rate:** 5-10 chunks/second (depends on hardware)
- **Recommended:** 4-8 worker threads
- **TPS Throttling:** Automatically pauses if server TPS drops below threshold
- **Batch Mode:** Results applied in configurable batches per tick

## Output Format

### JSON (per chunk)

```json
{
  "x": 0,
  "z": 0,
  "minY": -64,
  "maxY": 320,
  "blocks": 24576,
  "timestamp": 1704067200000,
  "entities": 3,
  "metadata": {
    "region_x": "0",
    "region_z": "0"
  }
}
```

### CSV (aggregated)

```csv
chunkX,chunkZ,minY,maxY,blockCount,entityCount,timestamp
0,0,-64,320,24576,3,1704067200000
0,1,-64,320,22144,1,1704067200100
```

## Development

### Building from Source

```bash
./gradlew build
```

Requirements:
- Java 21+
- Gradle 8.14
- Fabric Loom 1.13.3

### Project Structure

```
src/main/java/net/fabricmc/churn/
├── ChurnMod.java              # Main mod entry point
├── command/
│   └── ChurnCommand.java      # Command dispatcher
└── generator/
    ├── ChunkExtractor.java    # Anvil parser
    ├── ChunkData.java         # Data model
    ├── NBTParser.java         # NBT decoder
    ├── WorldNavigator.java    # World discovery
    ├── OutputFormatter.java   # Format export
    ├── GeneratorManager.java  # Pipeline orchestration
    ├── Worker.java            # Worker thread
    ├── ChurnWorkQueue.java    # Task queue
    ├── MainThreadApplier.java # Result application
    ├── RegionCheckpointManager.java # Pause/resume
    ├── ProgressLogger.java    # Progress tracking
    ├── TPSMonitor.java        # Server TPS monitoring
    └── JobConfig.java         # Configuration
```

## Commands

| Command | Usage | Description |
|---------|-------|-------------|
| `start` | `/churn start <world> <radius> [options]` | Begin chunk extraction |
| `status` | `/churn status [json]` | Show extraction progress |
| `pause` | `/churn pause` | Pause and save checkpoint |
| `resume` | `/churn resume <path>` | Resume from checkpoint |
| `cancel` | `/churn cancel` | Cancel current job |
| `postprocess` | `/churn postprocess <path>` | Export to CSV format |
| `clean-checkpoints` | `/churn clean-checkpoints <path>` | Remove checkpoints |
| `help` | `/churn help` | Show help message |

## Troubleshooting

### World Not Found
- Ensure world name matches exactly (case-sensitive)
- Verify world directory contains `level.dat`

### Low Extraction Rate
- Reduce worker threads: `--threads 4`
- Increase TPS threshold: `--minTps 10`

### Extraction Stops
- Resume from checkpoint: `/churn resume churn_last_job.meta`
- Check server logs for I/O errors

See [USAGE_GUIDE.md](USAGE_GUIDE.md#troubleshooting) for detailed troubleshooting.

## Technical Details

For comprehensive technical documentation, see [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md).

Topics covered:
- NBT tag format specifications
- Anvil region file format
- Chunk coordinate systems
- Error recovery mechanisms
- Performance optimization strategies
- Future enhancement roadmap

## Dependencies

- **Minecraft:** 1.21
- **Fabric Loader:** 0.16.10+
- **Fabric API:** 0.98.0+
- **Java:** 21+

## License

See LICENSE file for details.

## Contributing

This is an actively maintained project. Contributions are welcome:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Changelog

### Version 1.0 (Current)

- ✅ ChunkExtractor with Anvil format support
- ✅ Comprehensive NBT parser
- ✅ WorldNavigator for automatic world detection
- ✅ Parallel chunk extraction with configurable workers
- ✅ TPS-aware throttling
- ✅ Checkpoint system for pause/resume
- ✅ JSON and CSV export formats
- ✅ Post-processing pipeline
- ✅ Full error recovery and resilience

### Planned Features

- [ ] Biome and noise configuration extraction
- [ ] Block type enumeration with counts
- [ ] CRC32 validation for data integrity
- [ ] Protocol Buffers output format
- [ ] Real-time streaming to external tools
- [ ] Multi-dimension simultaneous extraction
- [ ] Database backend support

## Contact & Support

For issues, questions, or feature requests, please check the GitHub repository or refer to [USAGE_GUIDE.md](USAGE_GUIDE.md).

---

**Status:** Production Ready ✓
**Last Updated:** 2024
**Build:** Successful
**Tests:** Manual verification completed
