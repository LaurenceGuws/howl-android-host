package howl.term.service;

import howl.term.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Android host userland runtime skeleton. */
public final class UserlandRuntime {
    private static final String TAG = "howl.userland";
    private final String prefix;
    private final String home;
    private final String tmp;
    private final String howlPm;
    private final String shell;
    private final String manifestUrl;
    private boolean started;

    public UserlandRuntime(android.content.Context context) {
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
            android.util.Log.e(TAG, "start called while already started");
            return;
        }
        started = true;
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    private void initUserland() {
        if (!ensureDir(home)) {
            android.util.Log.e(TAG, "failed to ensure home dir path=" + home);
            return;
        }
        if (!ensureDir(tmp)) {
            android.util.Log.e(TAG, "failed to ensure tmp dir path=" + tmp);
            return;
        }

        final boolean shellExists = new File(getShell()).isFile();
        final boolean pmExists = new File(howlPm).isFile();

        if (shellExists) {
            runHowlPmDoctorAndList();
            return;
        }
        if (!pmExists) {
            android.util.Log.e(TAG, "howl-pm binary missing path=" + howlPm);
            return;
        }

        runHowlPmInstall();
        final boolean shellAfter = new File(getShell()).isFile();
        if (!shellAfter) {
            android.util.Log.e(TAG, "shell still missing after install path=" + getShell());
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
            android.util.Log.e(TAG, "howl-pm install failed rc=" + rc);
        }
    }

    private void runHowlPmDoctorAndList() {
        final int doctorRc = runHowlPm("doctor", "--prefix", getPrefix());
        if (doctorRc != 0) {
            android.util.Log.e(TAG, "howl-pm doctor failed rc=" + doctorRc);
        }
        final int listRc = runHowlPm("list-available", "--manifest", manifestUrl, "--prefix", getPrefix());
        if (listRc != 0) {
            android.util.Log.e(TAG, "howl-pm list-available failed rc=" + listRc);
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
            android.util.Log.e(TAG, "process spawn/io failure cmd=" + cmd[0] + " err=" + err.getMessage());
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            android.util.Log.e(TAG, "process wait interrupted err=" + err.getMessage());
        }
        return -1;
    }

}
