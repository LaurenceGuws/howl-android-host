package howl.term.service;

import howl.term.R;

import java.io.BufferedReader;
import java.io.File;
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
