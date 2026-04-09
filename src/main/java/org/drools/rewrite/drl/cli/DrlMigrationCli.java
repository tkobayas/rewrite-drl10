package org.drools.rewrite.drl.cli;

import org.drools.rewrite.drl.ast.AstDrlMigrationRecipe;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DrlMigrationCli {

    private DrlMigrationCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            return;
        }

        if (!"migrate".equals(args[0])) {
            System.err.println("Unknown command: " + args[0]);
            printUsage();
            System.exit(2);
        }

        try {
            CliOptions options = CliOptions.parse(args);
            int exitCode = migrate(options);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }
    }

    private static int migrate(CliOptions options) throws IOException {
        Path target = options.target().toAbsolutePath().normalize();
        if (!Files.exists(target)) {
            System.err.println("Target does not exist: " + target);
            return 2;
        }

        List<Path> drlFiles = findDrlFiles(target);
        if (drlFiles.isEmpty()) {
            System.out.println("No .drl files found under " + target);
            return 0;
        }

        Path projectRoot = rootFor(target);
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            System.err.println("Rewrite error: " + t.getMessage());
            t.printStackTrace(System.err);
        });

        PlainTextParser parser = new PlainTextParser();
        List<Parser.Input> inputs = drlFiles.stream()
                .map(Parser.Input::fromFile)
                .toList();
        List<SourceFile> parsed = parser.parseInputs(inputs, projectRoot, ctx).toList();

        Recipe recipe = new AstDrlMigrationRecipe();
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(parsed), ctx);
        List<Result> results = run.getChangeset().getAllResults();

        if (results.isEmpty()) {
            System.out.println("Scanned " + drlFiles.size() + " DRL file(s). No changes needed.");
            return 0;
        }

        Path backupRoot = null;
        if (!options.dryRun() && options.backupEnabled()) {
            backupRoot = options.backupDir() != null ? options.backupDir().toAbsolutePath().normalize() : defaultBackupRoot(target);
            Files.createDirectories(backupRoot);
        }

        int changed = 0;
        for (Result result : results) {
            if (result.getBefore() == null || result.getAfter() == null) {
                continue;
            }

            Path relative = result.getAfter().getSourcePath();
            Path file = projectRoot.resolve(relative).normalize();
            if (!Files.exists(file)) {
                continue;
            }

            changed++;
            if (options.dryRun()) {
                System.out.println("Would rewrite: " + file);
                continue;
            }

            if (backupRoot != null) {
                Path backupFile = backupRoot.resolve(relative).normalize();
                Files.createDirectories(Objects.requireNonNullElse(backupFile.getParent(), backupRoot));
                Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }

            Charset charset = result.getAfter().getCharset() != null ? result.getAfter().getCharset() : Charset.defaultCharset();
            Files.writeString(
                    file,
                    result.getAfter().printAll(),
                    charset,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            System.out.println("Rewrote: " + file);
        }

        System.out.println();
        System.out.println("Summary");
        System.out.println("Scanned: " + drlFiles.size() + " file(s)");
        System.out.println("Changed: " + changed + " file(s)");
        if (options.dryRun()) {
            System.out.println("Mode: dry-run");
        } else if (backupRoot != null) {
            System.out.println("Backup: " + backupRoot);
        } else {
            System.out.println("Backup: disabled");
        }
        return 0;
    }

    private static List<Path> findDrlFiles(Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            if (target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".drl")) {
                return List.of(target);
            }
            return List.of();
        }

        try (var stream = Files.walk(target)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".drl"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    private static Path rootFor(Path target) {
        if (Files.isDirectory(target)) {
            return target;
        }
        Path parent = target.getParent();
        return parent != null ? parent : Path.of(".");
    }

    private static Path defaultBackupRoot(Path target) {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path normalized = target.toAbsolutePath().normalize();
        Path parent = normalized.getParent() != null ? normalized.getParent() : Path.of(".").toAbsolutePath().normalize();
        return parent.resolve(".rewrite-drl10-backups").resolve(timestamp).resolve(normalized.getFileName());
    }

    private static boolean isHelp(String arg) {
        return "-h".equals(arg) || "--help".equals(arg) || "help".equals(arg);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  rewrite-drl10 migrate <path> [--dry-run] [--no-backup] [--backup-dir <dir>]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rewrite-drl10 migrate ./rules");
        System.out.println("  rewrite-drl10 migrate ./src/main/resources --dry-run");
        System.out.println("  rewrite-drl10 migrate ./rules --backup-dir /tmp/drl-backups");
    }

    private record CliOptions(Path target, boolean dryRun, boolean backupEnabled, Path backupDir) {
        private static CliOptions parse(String[] args) {
            Path target = null;
            boolean dryRun = false;
            boolean backupEnabled = true;
            Path backupDir = null;

            List<String> remaining = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                remaining.add(args[i]);
            }

            for (int i = 0; i < remaining.size(); i++) {
                String arg = remaining.get(i);
                switch (arg) {
                    case "--dry-run" -> dryRun = true;
                    case "--no-backup" -> backupEnabled = false;
                    case "--backup-dir" -> {
                        if (i + 1 >= remaining.size()) {
                            throw new IllegalArgumentException("--backup-dir requires a path");
                        }
                        backupDir = Path.of(remaining.get(++i));
                    }
                    default -> {
                        if (arg.startsWith("--")) {
                            throw new IllegalArgumentException("Unknown option: " + arg);
                        }
                        if (target != null) {
                            throw new IllegalArgumentException("Only one target path is supported");
                        }
                        target = Path.of(arg);
                    }
                }
            }

            if (target == null) {
                throw new IllegalArgumentException("Missing target path");
            }
            if (!backupEnabled && backupDir != null) {
                throw new IllegalArgumentException("--backup-dir cannot be used together with --no-backup");
            }
            return new CliOptions(target, dryRun, backupEnabled, backupDir);
        }
    }
}
