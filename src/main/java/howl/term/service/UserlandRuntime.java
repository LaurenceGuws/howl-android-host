package howl.term.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Android host userland runtime skeleton. */
public final class UserlandRuntime {
    private static final String TAG = "howl.userland";
    private static final String PACKAGE_NAME = "howl.term";
    private static final String PREFIX = "/data/data/" + PACKAGE_NAME + "/files/usr";
    private static final String HOME = "/data/data/" + PACKAGE_NAME + "/files/home";
    private static final String TMP = "/data/data/" + PACKAGE_NAME + "/cache/tmp";
    private static final String HOWL_PM = PREFIX + "/bin/howl-pm";
    private static final String BASH = PREFIX + "/bin/bash";
    private static final String MANIFEST_URL =
            "https://github.com/LaurenceGuws/howl-pm/releases/download/android-dev-2026.04.18.182005/android-dev-prefix.release.manifest.json";

    public UserlandRuntime() {
        android.util.Log.i(TAG, "userland.init.01 runtime.constructed");
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    private void initUserland() {
        android.util.Log.i(TAG, "userland.init.02 release.manifest_url=" + MANIFEST_URL);
        android.util.Log.i(TAG, "userland.init.03 path.prefix=" + PREFIX);
        android.util.Log.i(TAG, "userland.init.04 path.home=" + HOME);
        android.util.Log.i(TAG, "userland.init.05 path.tmp=" + TMP);
        if (!ensureDir(HOME, "userland.init.06")) {
            return;
        }
        if (!ensureDir(TMP, "userland.init.07")) {
            return;
        }

        final boolean bashExists = new File(BASH).isFile();
        final boolean pmExists = new File(HOWL_PM).isFile();
        android.util.Log.i(TAG, "userland.init.08 check.bash exists=" + bashExists + " path=" + BASH);
        android.util.Log.i(TAG, "userland.init.09 check.howl_pm exists=" + pmExists + " path=" + HOWL_PM);

        if (bashExists) {
            android.util.Log.i(TAG, "userland.ready.01 bash_present");
            runHowlPmDoctorAndList();
            return;
        }
        if (!pmExists) {
            android.util.Log.e(TAG, "userland.blocked.01 howl_pm_missing path=" + HOWL_PM);
            return;
        }

        android.util.Log.i(TAG, "userland.install.01 begin profile=dev-baseline");
        runHowlPmInstall();
        final boolean bashAfter = new File(BASH).isFile();
        android.util.Log.i(TAG, "userland.install.02 postcheck.bash exists=" + bashAfter + " path=" + BASH);
        if (bashAfter) {
            android.util.Log.i(TAG, "userland.ready.02 install_success");
            runHowlPmDoctorAndList();
        } else {
            android.util.Log.e(TAG, "userland.failed.01 install_completed_without_bash");
        }
    }

    private boolean ensureDir(String path, String step) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            android.util.Log.i(TAG, step + " mkdir.skip exists=true path=" + path);
            return true;
        }
        final boolean ok = dir.mkdirs();
        android.util.Log.i(TAG, step + " mkdir.created=" + ok + " path=" + path);
        if (!ok && !dir.isDirectory()) {
            android.util.Log.e(TAG, "userland.failed.00 mkdir path=" + path);
            return false;
        }
        return true;
    }

    private void runHowlPmInstall() {
        runHowlPm(
                "userland.install",
                "install",
                "--manifest",
                MANIFEST_URL,
                "--prefix",
                PREFIX,
                "dev-baseline");
    }

    private void runHowlPmDoctorAndList() {
        android.util.Log.i(TAG, "userland.pm.01 doctor.begin");
        runHowlPm("userland.pm.doctor", "doctor", "--prefix", PREFIX);
        android.util.Log.i(TAG, "userland.pm.02 list_available.begin");
        runHowlPm("userland.pm.list_available", "list-available", "--manifest", MANIFEST_URL, "--prefix", PREFIX);
    }

    private int runHowlPm(String logPrefix, String... args) {
        try {
            final String[] cmd = new String[args.length + 1];
            cmd[0] = HOWL_PM;
            System.arraycopy(args, 0, cmd, 1, args.length);
            final ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("HOME", HOME);
            pb.environment().put("TMPDIR", TMP);
            pb.environment().put("PREFIX", PREFIX);
            pb.environment().put("PATH", PREFIX + "/bin:/system/bin");
            pb.environment().put("SHELL", BASH);
            pb.environment().put("LD_LIBRARY_PATH", PREFIX + "/lib");
            final Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int n = 0;
                while ((line = reader.readLine()) != null) {
                    n += 1;
                    android.util.Log.i(TAG, logPrefix + ".output line=" + n + " " + line);
                }
            }
            final int exit = process.waitFor();
            android.util.Log.i(TAG, logPrefix + ".exit_code=" + exit);
            return exit;
        } catch (IOException err) {
            android.util.Log.e(TAG, logPrefix + ".io_exception " + err.getClass().getSimpleName() + ": " + err.getMessage());
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            android.util.Log.e(TAG, logPrefix + ".interrupted");
        }
        return -1;
    }

}
