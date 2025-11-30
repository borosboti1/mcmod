P# Churn — Pre-generation mod (skeleton)

This workspace contains a small Fabric mod skeleton for the "Churn" world pre-generation mod idea.

What is included
- `ChurnMod` — Fabric `ModInitializer` that registers the command entrypoint.
- `ChurnCommand` — brigadier command skeleton with `start`, `status`, `cancel`, and `postprocess` subcommands.
- `GeneratorManager` and `JobConfig` — basic placeholders for coordinating generation jobs.

How to build
1. Create a standard Fabric mod Gradle project (this skeleton does not include build files).
2. Place sources under `src/main/java` and add Fabric API dependency for Minecraft 1.21+.

Example commands (in-game as an operator):

```
/churn start minecraft:overworld 10000 8
/churn status
/churn cancel
/churn postprocess ./pre_generated_world
```

Notes
- This is a starting point. The next steps are implementing the core engine (workers, applier, IO layer), post-processors (lighting, fluids, entities), and robust command parsing/validation.
- For production use you'll need a `fabric.mod.json`, Gradle build files, and proper dependency configuration.
