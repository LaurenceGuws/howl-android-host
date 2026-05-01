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
    private volatile boolean ready;
    private volatile boolean failed;
    private final Object stateLock;

    public UserlandSvc(android.content.Context context) {
        final String appRoot = context.getString(R.string.userland_app_root, context.getPackageName());
        this.prefix = appRoot + context.getString(R.string.userland_prefix_suffix);
        this.home = appRoot + context.getString(R.string.userland_home_suffix);
        this.tmp = appRoot + context.getString(R.string.userland_tmp_suffix);
        this.howlPm = this.prefix + context.getString(R.string.userland_howl_pm_suffix);
        this.shell = this.prefix + context.getString(R.string.userland_shell_suffix);
        this.manifestUrl = context.getString(R.string.userland_manifest_url);
        this.started = false;
        this.ready = false;
        this.failed = false;
        this.stateLock = new Object();
    }

    public String getShell() {
        return shell;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getHome() {
        return home;
    }

    public String buildShellCommand() {
        return "export APP_DATA_DIR=\"" + appDataDir() + "\";"
                + "export PREFIX=\"" + getPrefix() + "\";"
                + "export HOME=\"" + home + "\";"
                + "export TMPDIR=\"" + tmp + "\";"
                + "export TERM=\"xterm-256color\";"
                + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\";"
                + "export LD_LIBRARY_PATH=\"$PREFIX/lib\";"
                + "cd \"$HOME\";"
                + "exec \"" + shell + "\" -i";
    }

    public boolean waitUntilReady(long timeoutMs) {
        final long deadline = android.os.SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
        synchronized (stateLock) {
            while (!ready && !failed) {
                final long remaining = deadline - android.os.SystemClock.uptimeMillis();
                if (remaining <= 0) break;
                try {
                    stateLock.wait(remaining);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return ready;
        }
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    private void initUserland() {
        synchronized (stateLock) {
            ready = false;
            failed = false;
        }
        if (!ensureDir(home)) {
            failReady();
            return;
        }
        if (!ensureDir(tmp)) {
            failReady();
            return;
        }

        final boolean shellExists = new File(getShell()).isFile();
        final boolean pmExists = new File(howlPm).isFile();

        if (shellExists) {
            ensureShellErgonomics();
            runHowlPmDoctorAndList();
            markReady();
            return;
        }
        if (!pmExists) {
            failReady();
            return;
        }

        runHowlPmInstall();
        final boolean shellAfter = new File(getShell()).isFile();
        if (!shellAfter) {
            failReady();
            return;
        }
        ensureShellErgonomics();
        runHowlPmDoctorAndList();
        markReady();
    }

    private void markReady() {
        synchronized (stateLock) {
            ready = true;
            failed = false;
            stateLock.notifyAll();
        }
    }

    private void failReady() {
        synchronized (stateLock) {
            failed = true;
            ready = false;
            stateLock.notifyAll();
        }
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
        final String content =
                "# ~/.bashrc: executed by bash(1) for non-login shells.\n"
                        + "# Howl Android userland profile (Debian-style prompt + sane defaults).\n"
                        + "\n"
                        + "case $- in\n"
                        + "    *i*) ;;\n"
                        + "      *) return;;\n"
                        + "esac\n"
                        + "\n"
                        + "export APP_DATA_DIR=\"" + appDataDir() + "\"\n"
                        + "export PREFIX=\"" + getPrefix() + "\"\n"
                        + "export HOME=\"" + home + "\"\n"
                        + "export TMPDIR=\"" + tmp + "\"\n"
                        + "export TERM=\"xterm-256color\"\n"
                        + "export USER=\"home\"\n"
                        + "export LOGNAME=\"home\"\n"
                        + "export HOSTNAME=\"howl\"\n"
                        + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\"\n"
                        + "export LD_LIBRARY_PATH=\"$PREFIX/lib\"\n"
                        + "\n"
                        + "HISTCONTROL=ignoreboth\n"
                        + "shopt -s histappend\n"
                        + "HISTSIZE=1000\n"
                        + "HISTFILESIZE=2000\n"
                        + "shopt -s checkwinsize\n"
                        + "\n"
                        + "debian_chroot=\"howl\"\n"
                        + "color_prompt=yes\n"
                        + "\n"
                        + "if [ \"$color_prompt\" = yes ]; then\n"
                        + "    PS1='${debian_chroot:+($debian_chroot)}\\[\\033[01;32m\\]\\u@\\h\\[\\033[00m\\]:\\[\\033[01;34m\\]\\w\\[\\033[00m\\]\\$ '\n"
                        + "else\n"
                        + "    PS1='${debian_chroot:+($debian_chroot)}\\u@\\h:\\w\\$ '\n"
                        + "fi\n"
                        + "unset color_prompt\n"
                        + "PROMPT_DIRTRIM=3\n"
                        + "\n"
                        + "case \"$TERM\" in\n"
                        + "xterm*|rxvt*)\n"
                        + "    PS1=\"\\[\\e]0;${debian_chroot:+($debian_chroot)}\\u@\\h: \\w\\a\\]$PS1\"\n"
                        + "    ;;\n"
                        + "*) ;;\n"
                        + "esac\n"
                        + "\n"
                        + "LS_COLORS='di=34:ln=35:ex=31'\n"
                        + "export LS_COLORS\n"
                        + "\n"
                        + "alias ls='ls --color=auto'\n"
                        + "alias grep='grep --color=auto'\n"
                        + "alias fgrep='fgrep --color=auto'\n"
                        + "alias egrep='egrep --color=auto'\n"
                        + "alias ll='ls -l --color=auto'\n"
                        + "alias la='ls -A'\n"
                        + "alias l='ls -la --color=auto'\n"
                        + "alias pkg='howl-pm pkg'\n"
                        + "\n"
                        + "if [ -f ~/.bash_aliases ]; then\n"
                        + "  . ~/.bash_aliases\n"
                        + "fi\n"
                        + "\n"
                        + "if ! shopt -oq posix; then\n"
                        + "  if [ -f /usr/share/bash-completion/bash_completion ]; then\n"
                        + "    . /usr/share/bash-completion/bash_completion\n"
                        + "  elif [ -f /etc/bash_completion ]; then\n"
                        + "    . /etc/bash_completion\n"
                        + "  fi\n"
                        + "fi\n";
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

    private String appDataDir() {
        final String marker = "/data/data/";
        if (!prefix.startsWith(marker)) {
            return "/data/data/" + extractPackageName() + "/files";
        }
        final String rest = prefix.substring(marker.length());
        final int slash = rest.indexOf('/');
        if (slash < 0) {
            return "/data/data/" + extractPackageName() + "/files";
        }
        return marker + rest.substring(0, slash) + "/files";
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
            pb.directory(new File(home));
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
