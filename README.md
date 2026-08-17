# rewrite-drl10

CLI tool to migrate legacy DRL syntax to DRL 10-compatible syntax.

## What It Rewrites

The migration currently covers:

- custom operators without the `##` prefix
- half constraints
- `agenda-group` to `ruleflow-group`
- LHS `&&` to `and` (`&&` in constraint expressions is preserved)
- LHS `||` to `or` (`||` in constraint expressions is preserved)
- annotations between LHS logical operators and the following expression

Example of the last case:

```drl
( Double()
  or @Annot1 String()
  or @Annot2 Integer() )
```

becomes:

```drl
( Double()
  or String()
  or Integer() )
```

Trailing pattern annotations are preserved:

```drl
String() @watch(!*, age)
```

## Usage

Download the latest JAR from [GitHub Releases](https://github.com/tkobayas/rewrite-drl10/releases) and run:

```bash
java -jar rewrite-drl10-1.0.0.jar migrate <path>
```

`<path>` may be either:

- a directory, scanned recursively for `*.drl`
- a single `.drl` file

Examples:

```bash
java -jar rewrite-drl10-1.0.0.jar migrate ./rules --dry-run
java -jar rewrite-drl10-1.0.0.jar migrate ./rules
java -jar rewrite-drl10-1.0.0.jar migrate ./rules --backup-dir /tmp/drl-backups
java -jar rewrite-drl10-1.0.0.jar migrate ./rules --no-backup
```

Requires Java 17 or later.

## Options

- `--dry-run`: scan and report rewrites without modifying files
- `--no-backup`: rewrite files in place without creating backups
- `--backup-dir <dir>`: store backups under the given directory

By default, backups are created under a timestamped directory next to the target:

```text
.rewrite-drl10-backups/<timestamp>/<target-name>/...
```

## Build from Source

```bash
mvn package
```

This produces a fat JAR at `target/rewrite-drl10-1.0.0-SNAPSHOT.jar`.

## Example Test Data

Sample DRL files are available under `cli-test/`:

- `cli-test/customers/legacy-customer-rules.drl`
- `cli-test/orders/nested/legacy-order-rules.drl`
- `cli-test/annotations/trailing-annotation-ok.drl`
- `cli-test/already-migrated/no-change-needed.drl`

Try:

```bash
java -jar target/rewrite-drl10-1.0.0-SNAPSHOT.jar migrate ./cli-test --dry-run
java -jar target/rewrite-drl10-1.0.0-SNAPSHOT.jar migrate ./cli-test
```

## Output

The CLI prints:

- one line per rewritten file
- warnings for dropped operator-position LHS annotations
- a final summary with scanned file count, changed file count, and backup location

Example:

```text
[main] WARN org.drools.rewrite.drl.ast.AstLhsAnnotationRecipe - Dropping unsupported LHS operator annotation at line 4, column 9: @Annot1
Rewrote: /path/to/rules/sample.drl

Summary
Scanned: 1 file(s)
Changed: 1 file(s)
Backup: /path/to/.rewrite-drl10-backups/20260409-122905/rules
```
