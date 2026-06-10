import paramiko, os, time

host = '122.51.136.129'
port = 22
user = 'root'
password = 'Wzp0201.'
jar_local = 'D:\\mcay\\claudeCode001\\backend\\target\\ai-manager-1.0.0.jar'
jar_remote = '/home/cloudapp/application/backend/etc/ai-manager-1.0.0.jar'
jar_backup = '/home/cloudapp/application/backend/etc/ai-manager-1.0.0.jar.bak'

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(host, port, user, password, look_for_keys=False, allow_agent=False)
print('Connected')

# 1. Backup old JAR
stdin, stdout, stderr = ssh.exec_command(f'cp {jar_remote} {jar_backup} && echo "backup ok"')
print(stdout.read().decode().strip())

# 2. Transfer new JAR via SFTP
sftp = ssh.open_sftp()
sftp.put(jar_local, jar_remote)
sftp.close()
print(f'Uploaded {os.path.getsize(jar_local)} bytes')

# 3. Set permissions
ssh.exec_command(f'chown cloudapp:cloudapp {jar_remote} && chmod 644 {jar_remote}')

# 4. Restart service
stdin, stdout, stderr = ssh.exec_command('systemctl restart ai-manager && echo "restart ok"')
out = stdout.read().decode().strip()
err = stderr.read().decode().strip()
print(f'Restart: {out} {err}')

time.sleep(3)

# 5. Check status
stdin, stdout, stderr = ssh.exec_command('systemctl status ai-manager --no-pager 2>&1 | head -20')
print(stdout.read().decode().strip())

ssh.close()
print('Done')
