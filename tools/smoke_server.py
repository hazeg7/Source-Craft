import os
import subprocess
import sys
import time

proj = r"C:\Users\hammo\Documents\MC Mods\sourcecraft"
env = os.environ.copy()
env["JAVA_HOME"] = r"C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
env["PATH"] = env["JAVA_HOME"] + r"\bin;" + env["PATH"]

run_dir = os.path.join(proj, "run")
os.makedirs(run_dir, exist_ok=True)
with open(os.path.join(run_dir, "eula.txt"), "w", encoding="ascii") as f:
    f.write("eula=true\n")

proc = subprocess.Popen(
    [os.path.join(proj, "gradlew.bat"), "runServer", "--args=--nogui", "--no-daemon", "--console=plain"],
    cwd=proj,
    env=env,
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1,
)

sent_stop = False
lines = []
t0 = time.time()
for line in proc.stdout:
    s = line.rstrip("\r\n")
    lines.append(s)
    if (
        "SourceCraft" in s
        or "sourcecraft" in s
        or "Done (" in s
        or "ERROR" in s
        or "Exception" in s
        or "Mixin" in s
        or "Stopping server" in s
        or "ThreadedAnvilChunkStorage" in s
    ):
        print(s, flush=True)
    if not sent_stop and "Done (" in s:
        sent_stop = True
        proc.stdin.write("stop\n")
        proc.stdin.flush()
    if time.time() - t0 > 120:
        print("TIMEOUT", flush=True)
        proc.kill()
        break

rc = proc.wait(timeout=20)
print(f"SERVER_EXIT_CODE={rc}", flush=True)
print(f"SENT_STOP={sent_stop}", flush=True)
errs = [
    l
    for l in lines
    if "MixinApplyError" in l
    or "FATAL" in l
    or ("ERROR]" in l and "JAVA_TOOL_OPTIONS" not in l)
    or ("Exception" in l and "JAVA_TOOL_OPTIONS" not in l)
]
print(f"ERROR_LINES={len(errs)}", flush=True)
for e in errs[:12]:
    print("ERR:", e, flush=True)
sys.exit(0 if rc == 0 and sent_stop and len(errs) == 0 else 1)
