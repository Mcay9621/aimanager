import paramiko
import os, time, sys

HOST = '122.51.136.129'
PORT = 22
USER = 'root'
PASSWORD = 'Wzp0201.'

BACKEND_JAR = r'D:\mcay\claudeCode001\backend\target\ai-manager-1.0.0.jar'
FRONTEND_DIST = r'D:\mcay\claudeCode001\frontend\dist'
SQL_MIGRATION = r'D:\mcay\claudeCode001\sql\migration_tokens.sql'

BACKEND_REMOTE = '/home/cloudapp/application/backend/'
FRONTEND_REMOTE = '/home/cloudapp/application/frontend/'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
print("Connecting to %s..." % HOST)
ssh.connect(HOST, PORT, USER, PASSWORD, look_for_keys=False, allow_agent=False)
sftp = ssh.open_sftp()

def run(cmd, echo=True):
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if echo:
        if out: print(out)
        if err: print("ERR:", err, file=sys.stderr)
    return out, err

print("=" * 50)

# ── Step 1: Backend JAR ──
print("\n[1/5] 上传后端 JAR...")
sftp.put(BACKEND_JAR, BACKEND_REMOTE + 'ai-manager-1.0.0.jar')
print("  OK")

# ── Step 2: SQL migration ──
print("\n[2/5] 上传并执行 SQL 迁移...")
sftp.put(SQL_MIGRATION, '/tmp/migration_tokens.sql')
out, err = run('mysql -uroot -pWzp0201. ai_manager < /tmp/migration_tokens.sql 2>&1')
if 'ERROR' in err or 'ERROR' in out:
    print("  SQL ERROR:", err or out)
else:
    print("  OK")

# ── Step 3: Frontend dist (clean + upload) ──
print("\n[3/5] 部署前端 dist...")
# Server-side: remove old assets, upload new ones
run('rm -rf ' + FRONTEND_REMOTE + 'assets 2>/dev/null; mkdir -p ' + FRONTEND_REMOTE + 'assets')

def upload_dir(local, remote):
    for root, dirs, files in os.walk(local):
        for d in dirs:
            rdir = os.path.join(remote, os.path.relpath(os.path.join(root, d), local)).replace('\\', '/')
            try:
                sftp.mkdir(rdir)
            except (IOError, OSError):
                pass
        for f in files:
            lpath = os.path.join(root, f)
            rpath = os.path.join(remote, os.path.relpath(lpath, local)).replace('\\', '/')
            sftp.put(lpath, rpath)

upload_dir(FRONTEND_DIST, FRONTEND_REMOTE)
print("  OK")

sftp.close()

# ── Step 4: Restart backend ──
print("\n[4/5] 重启后端服务...")
run('systemctl restart ai-manager')
time.sleep(4)
out, _ = run('systemctl is-active ai-manager')
if out == 'active':
    print("  后端状态: active (running)")
else:
    print("  后端状态:", out)

# ── Step 5: Verify ──
print("\n[5/5] 验证...")
out, _ = run('ss -tlnp | grep 8080')
if out:
    print("  端口 8080: 监听中")
else:
    print("  端口 8080: NOT LISTENING")

ssh.close()
print("\n" + "=" * 50)
print("部署完成!")
