package howl.term.service;

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
    private final String bash;
    private final String manifestUrl;
    private boolean started;

    public UserlandRuntime(
            String prefix,
            String home,
            String tmp,
            String howlPm,
            String bash,
            String manifestUrl) {
        this.prefix = prefix;
        this.home = home;
        this.tmp = tmp;
        this.howlPm = howlPm;
        this.bash = bash;
        this.manifestUrl = manifestUrl;
        this.started = false;
        android.util.Log.i(TAG, "userland.init.01 runtime.constructed");
    }

    public void start() {
        if (started) return;
        started = true;
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    private void initUserland() {
        android.util.Log.i(TAG, "userland.init.02 release.manifest_url=" + manifestUrl);
        android.util.Log.i(TAG, "userland.init.03 path.prefix=" + prefix);
        android.util.Log.i(TAG, "userland.init.04 path.home=" + home);
        android.util.Log.i(TAG, "userland.init.05 path.tmp=" + tmp);
        if (!ensureDir(home, "userland.init.06")) {
            return;
        }
        if (!ensureDir(tmp, "userland.init.07")) {
            return;
        }

        final boolean bashExists = new File(bash).isFile();
        final boolean pmExists = new File(howlPm).isFile();
        android.util.Log.i(TAG, "userland.init.08 check.bash exists=" + bashExists + " path=" + bash);
        android.util.Log.i(TAG, "userland.init.09 check.howl_pm exists=" + pmExists + " path=" + howlPm);

        if (bashExists) {
            android.util.Log.i(TAG, "userland.ready.01 bash_present");
            runHowlPmDoctorAndList();
            return;
        }
        if (!pmExists) {
            android.util.Log.e(TAG, "userland.blocked.01 howl_pm_missing path=" + howlPm);
            return;
        }

        android.util.Log.i(TAG, "userland.install.01 begin profile=dev-baseline");
        runHowlPmInstall();
        final boolean bashAfter = new File(bash).isFile();
        android.util.Log.i(TAG, "userland.install.02 postcheck.bash exists=" + bashAfter + " path=" + bash);
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
                manifestUrl,
                "--prefix",
                prefix,
                "dev-baseline");
    }

    private void runHowlPmDoctorAndList() {
        android.util.Log.i(TAG, "userland.pm.01 doctor.begin");
        runHowlPm("userland.pm.doctor", "doctor", "--prefix", prefix);
        android.util.Log.i(TAG, "userland.pm.02 list_available.begin");
        runHowlPm("userland.pm.list_available", "list-available", "--manifest", manifestUrl, "--prefix", prefix);
    }

    private int runHowlPm(String logPrefix, String... args) {
        try {
            final String[] cmd = new String[args.length + 1];
            cmd[0] = howlPm;
            System.arraycopy(args, 0, cmd, 1, args.length);
            final ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("HOME", home);
            pb.environment().put("TMPDIR", tmp);
            pb.environment().put("PREFIX", prefix);
            pb.environment().put("PATH", prefix + "/bin:/system/bin");
            pb.environment().put("SHELL", bash);
            pb.environment().put("LD_LIBRARY_PATH", prefix + "/lib");
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
