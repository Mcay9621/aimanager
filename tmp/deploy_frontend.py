import paramiko
import os, sys

HOST = '122.51.136.129'
PORT = 22
USER = 'root'
PASSWORD = 'Wzp0201.'

FRONTEND_DIST = r'D:\mcay\claudeCode001\frontend\dist'
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

# Clean remote assets and upload fresh
print("\n[1/2] 清理远程前端目录...")
run('rm -rf ' + FRONTEND_REMOTE + 'assets 2>/dev/null')
run('rm -f ' + FRONTEND_REMOTE + 'index.html')
run('mkdir -p ' + FRONTEND_REMOTE + 'assets')

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
            print(f"  {f}")

print("\n[2/2] 上传前端文件...")
upload_dir(FRONTEND_DIST, FRONTEND_REMOTE)

sftp.close()
ssh.close()
print("\n" + "=" * 50)
print("前端部署完成!")
