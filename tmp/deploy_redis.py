import paramiko, os, sys, time

HOST = '122.51.136.129'
PORT = 22
USER = 'root'
PASSWORD = 'Wzp0201.'

BACKEND_JAR = r'D:\mcay\claudeCode001\backend\target\ai-manager-1.0.0.jar'
FRONTEND_DIST = r'D:\mcay\claudeCode001\frontend\dist'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
print("Connecting...")
ssh.connect(HOST, PORT, USER, PASSWORD, look_for_keys=False, allow_agent=False)
sftp = ssh.open_sftp()

def run(cmd):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if out: print(out)
    if err: print("ERR:", err, file=sys.stderr)
    return out, err

# Step 1: Upload JAR
print("\n[1/4] Uploading JAR...")
sftp.put(BACKEND_JAR, '/home/cloudapp/application/backend/ai-manager-1.0.0.jar')
print("  OK")

# Step 2: Upload frontend dist
print("\n[2/4] Uploading frontend dist...")
run('rm -rf /home/cloudapp/application/frontend/assets 2>/dev/null')
run('mkdir -p /home/cloudapp/application/frontend/assets')

def upload_dir(local, remote):
    for root, dirs, files in os.walk(local):
        for d in dirs:
            rdir = os.path.join(remote, os.path.relpath(os.path.join(root, d), local)).replace('\\', '/')
            try: sftp.mkdir(rdir)
            except: pass
        for f in files:
            lpath = os.path.join(root, f)
            rpath = os.path.join(remote, os.path.relpath(lpath, local)).replace('\\', '/')
            sftp.put(lpath, rpath)

upload_dir(FRONTEND_DIST, '/home/cloudapp/application/frontend')
print("  OK")

sftp.close()

# Step 3: Restart backend
print("\n[3/4] Restarting backend...")
run('systemctl restart ai-manager')
time.sleep(5)
out, _ = run('systemctl is-active ai-manager')
print("  Backend status:", out)

# Step 4: Verify
print("\n[4/4] Verifying...")
time.sleep(3)
out, _ = run('ss -tlnp | grep 8080')
if out: print("  Port 8080: listening")
else: print("  Port 8080: NOT LISTENING")

ssh.close()
print("\nDeploy complete!")
