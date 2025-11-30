# Churn Mod - Usage Guide

## Installation

1. Place the compiled JAR file (`churn-*.jar`) in your Fabric mods directory
2. Ensure Fabric Loader 0.16.10+ is installed
3. Requires Minecraft 1.21 with Fabric API 0.98.0+

## Quick Start

### Starting a Chunk Extraction

```bash
/churn start myworld 10
```

This extracts all chunks within a 10-chunk radius from the player's current position.

### Checking Progress

```bash
/churn status
```

Returns real-time extraction statistics:
- Current progress percentage
- Extracted chunks vs. total
- Extraction throughput (chunks/sec)
- Estimated time remaining
- Current TPS

### JSON Status Output

```bash
/churn status json
```

Returns JSON format for scripting:
```json
{
  "world": "myworld",
  "radius": 10,
  "totalChunks": 441,
  "doneChunks": 203,
  "percent": 46.03,
  "tps": 18.5,
  "paused": false,
  "applierPending": 12,
  "workerThreads": 4,
  "chunksPerSecond": 5.23,
  "etaSeconds": 45.5
}
```

## Advanced Usage

### Configuration File

Create a properties file `extraction.properties`:

```properties
world=myworld
radius=50
threads=8
outputPath=./churn_output
minTps=15.0
logPath=./churn_progress.log
checkpointPath=./churn_checkpoints
force=false
```

Then run:

```bash
/churn start myworld 50 @extraction.properties
```

### Inline JSON Configuration

```bash
/churn start myworld 25 {"threads": 6, "minTps": 14.5, "batch": 32}
```

### Command-line Arguments

```bash
/churn start myworld 30 --threads 8 --minTps 15 --batch 32
```

## Job Management

### Pausing Extraction

```bash
/churn pause
```

Saves current progress to checkpoint files for later resumption.

### Resuming from Checkpoint

```bash
/churn resume ./churn_checkpoints/churn_last_job.meta
```

Resumes from where you left off, skipping already-extracted chunks.

### Canceling Current Job

```bash
/churn cancel
```

Immediately terminates all worker threads.

### Cleaning Up Old Checkpoints

```bash
/churn clean-checkpoints ./churn_checkpoints
```

Removes all checkpoint data to free disk space.

## Output Processing

### Post-Processing to CSV

After extraction completes, aggregate results:

```bash
/churn postprocess ./churn_output
```

This reads all `chunk_*.json` files and generates `chunks.csv` with columns:
- `chunkX`, `chunkZ`: Chunk coordinates
- `minY`, `maxY`: Height range
- `blockCount`: Number of blocks
- `entityCount`: Number of entities
- `timestamp`: Extraction time

### Output File Structure

```
churn_output/
├── chunk_0_0.json
├── chunk_0_1.json
├── chunk_1_0.json
├── chunk_1_1.json
├── chunks.csv
└── chunks.json (if combined mode)
```

## Performance Tuning

### Threads Configuration

- **Default:** 4 threads
- **Fast extraction:** 8-12 threads (if CPU permits)
- **Low-impact:** 1-2 threads (preserves server responsiveness)

### TPS Throttling

- **minTps 15.0:** Pauses extraction if server TPS drops below 15
- **minTps 0.0:** Disables throttling (may impact server performance)
- **Recommended:** 14.5-15.0 for survival servers

### Batch Size

- **Default:** 32 blocks/tick
- **Increase:** 64 for faster processing (higher server load)
- **Decrease:** 8 for minimal impact (slower extraction)

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `world` | (required) | World name or ID |
| `radius` | 10 | Chunk radius to extract |
| `threads` | 4 | Parallel worker threads |
| `outputPath` | churn_output | Output directory |
| `checkpointPath` | churn_checkpoints | Checkpoint directory |
| `minTps` | 15.0 | Minimum server TPS threshold |
| `batch` | 32 | Results per application tick |
| `logPath` | (optional) | Progress log file path |
| `logMaxBytes` | 10485760 | Max log file size (10MB) |
| `logRotateCount` | 5 | Number of rotated log files |
| `force` | false | Ignore checkpoint mismatches |

## Data Format

### JSON Output

Each chunk is stored as:

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
    "region_z": "0",
    "local_x": "0",
    "local_z": "0"
  }
}
```

### CSV Output

```csv
chunkX,chunkZ,minY,maxY,blockCount,entityCount,timestamp
0,0,-64,320,24576,3,1704067200000
0,1,-64,320,22144,1,1704067200100
1,0,-64,320,25984,5,1704067200200
```

## Troubleshooting

### "World not found" Error

- Verify the world name matches exactly (case-sensitive)
- Check that the world directory contains `level.dat`
- Try specifying the full path: `/churn start C:\path\to\world 10`

### Low Extraction Rate

- Reduce `threads` to lower server load
- Increase `minTps` threshold to avoid server throttling
- Check disk I/O with system monitoring tools

### Extraction Stops Unexpectedly

- Check server logs for errors
- Try resuming from checkpoint: `/churn resume churn_last_job.meta`
- Verify output directory has sufficient disk space

### "Checkpoint validation failed"

- Use `--force` flag to override: `/churn start world 10 force:true`
- Clean checkpoints and restart: `/churn clean-checkpoints`

## Advanced Scenarios

### Extracting Multiple Worlds

Create separate output directories:

```bash
/churn start world1 20 outputPath:./output_world1
/churn pause
/churn start world2 20 outputPath:./output_world2
```

### Resuming After Server Restart

```bash
/churn resume ./churn_checkpoints/churn_last_job.meta
```

Progress is restored from the last checkpoint.

### Analyzing Extracted Data

The CSV output is compatible with:
- Excel/Google Sheets for visualization
- Python/R for statistical analysis
- PostgreSQL for complex queries
- Grafana for real-time dashboards

## Limitations

- Maximum chunk radius: 2000 chunks (practical limit ~500 before performance degrades)
- Extraction rate depends on disk I/O and CPU
- Large worlds (>100MB region data) may require multiple sessions
- Entity data may not include all NBT fields

## Getting Help

For detailed technical information, see `IMPLEMENTATION_SUMMARY.md`.

For issues or feature requests, check the mod repository.
