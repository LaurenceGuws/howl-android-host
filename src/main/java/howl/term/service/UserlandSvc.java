package howl.term.service;

import howl.term.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Android host userland runtime skeleton. */
public final class UserlandSvc {
    private final String prefix;
    private final String home;
    private final String tmp;
    private final String howlPm;
    private final String shell;
    private final String manifestUrl;
    private boolean started;

    public UserlandSvc(android.content.Context context) {
        final String appRoot = context.getString(R.string.userland_app_root, context.getPackageName());
        this.prefix = appRoot + context.getString(R.string.userland_prefix_suffix);
        this.home = appRoot + context.getString(R.string.userland_home_suffix);
        this.tmp = appRoot + context.getString(R.string.userland_tmp_suffix);
        this.howlPm = this.prefix + context.getString(R.string.userland_howl_pm_suffix);
        this.shell = this.prefix + context.getString(R.string.userland_shell_suffix);
        this.manifestUrl = context.getString(R.string.userland_manifest_url);
        this.started = false;
    }

    public String getShell() {
        return shell;
    }

    public String getPrefix() {
        return prefix;
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    private void initUserland() {
        if (!ensureDir(home)) {
            return;
        }
        if (!ensureDir(tmp)) {
            return;
        }

        final boolean shellExists = new File(getShell()).isFile();
        final boolean pmExists = new File(howlPm).isFile();

        if (shellExists) {
            ensureShellErgonomics();
            runHowlPmDoctorAndList();
            return;
        }
        if (!pmExists) {
            return;
        }

        runHowlPmInstall();
        final boolean shellAfter = new File(getShell()).isFile();
        if (!shellAfter) {
            return;
        }
        ensureShellErgonomics();
        runHowlPmDoctorAndList();
    }

    private boolean ensureDir(String path) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            return true;
        }
        final boolean ok = dir.mkdirs();
        return ok || dir.isDirectory();
    }

    private void runHowlPmInstall() {
        final int rc = runHowlPm(
                "install",
                "--manifest",
                manifestUrl,
                "--prefix",
                getPrefix(),
                "dev-baseline");
        if (rc != 0) {
        }
    }

    private void runHowlPmDoctorAndList() {
        final int doctorRc = runHowlPm("doctor", "--prefix", getPrefix());
        if (doctorRc != 0) {
        }
        final int listRc = runHowlPm("list-available", "--manifest", manifestUrl, "--prefix", getPrefix());
        if (listRc != 0) {
        }
    }

    private void ensureShellErgonomics() {
        ensurePkgShim();
        ensureBashRc();
    }

    private void ensurePkgShim() {
        final File shim = new File(getPrefix() + "/bin/pkg");
        if (shim.isFile()) {
            return;
        }
        final String content = "#!/data/data/" + extractPackageName() + "/files/usr/bin/bash\n"
                + "exec \"" + howlPm + "\" pkg \"$@\"\n";
        writeExecutableFile(shim, content);
    }

    private void ensureBashRc() {
        final File bashrc = new File(home + "/.bashrc");
        if (bashrc.isFile()) {
            return;
        }
        final String content =
                "# howl android shell ergonomics\n"
                        + "export PREFIX=\"" + getPrefix() + "\"\n"
                        + "export HOME=\"" + home + "\"\n"
                        + "export TMPDIR=\"" + tmp + "\"\n"
                        + "export TERM=\"xterm-256color\"\n"
                        + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\"\n"
                        + "alias ll='ls -lah'\n"
                        + "alias la='ls -A'\n"
                        + "alias l='ls -CF'\n"
                        + "alias pkg='howl-pm pkg'\n"
                        + "PS1='\\u@howl:\\w\\$ '\n";
        writeFile(bashrc, content);
    }

    private String extractPackageName() {
        final String marker = "/data/data/";
        if (!prefix.startsWith(marker)) {
            return "howl.term";
        }
        final String rest = prefix.substring(marker.length());
        final int slash = rest.indexOf('/');
        if (slash < 0) {
            return "howl.term";
        }
        return rest.substring(0, slash);
    }

    private void writeExecutableFile(File file, String content) {
        writeFile(file, content);
        //noinspection ResultOfMethodCallIgnored
        file.setExecutable(true, false);
        //noinspection ResultOfMethodCallIgnored
        file.setReadable(true, false);
    }

    private void writeFile(File file, String content) {
        final File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
        } catch (IOException err) {
        }
    }

    private int runHowlPm(String... args) {
        final String[] cmd = new String[args.length + 1];
        cmd[0] = howlPm;
        System.arraycopy(args, 0, cmd, 1, args.length);
        try {
            final ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("HOME", home);
            pb.environment().put("TMPDIR", tmp);
            pb.environment().put("PREFIX", getPrefix());
            pb.environment().put("PATH", getPrefix() + "/bin:/system/bin");
            pb.environment().put("SHELL", getShell());
            pb.environment().put("LD_LIBRARY_PATH", getPrefix() + "/lib");
            final Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {}
            }
            return process.waitFor();
        } catch (IOException err) {
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
        return -1;
    }

}
