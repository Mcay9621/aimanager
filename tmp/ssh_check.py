import subprocess
import os
import sys

# Use sshpass via download
sshpass_path = r'D:\mcay\claudeCode001\tmp\sshpass.exe'

# Check if sshpass exists
if os.path.exists(sshpass_path):
    cmd = [sshpass_path, '-p', 'Wzp0201.', 'ssh', '-o', 'StrictHostKeyChecking=no', 'root@121.4.54.188', 'systemctl status nacos 2>&1 | head -30']
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    print(result.stdout)
    if result.stderr:
        print('STDERR:', result.stderr[:500])
    print('RC:', result.returncode)
else:
    print(f'sshpass not found at {sshpass_path}')
    # Try with expect-like approach
    import select
    import pty

    def ssh_with_password(host, password, command):
        output = []
        pid, fd = pty.fork()
        if pid == 0:
            os.execvp('ssh', ['ssh', '-o', 'StrictHostKeyChecking=no', host, command])
        else:
            while True:
                try:
                    r, w, e = select.select([fd], [], [], 10)
                    if r:
                        data = os.read(fd, 4096)
                        if not data:
                            break
                        text = data.decode('utf-8', errors='replace')
                        output.append(text)
                        if 'password:' in text.lower() or 'password' in text.lower():
                            os.write(fd, (password + '\n').encode())
                except (OSError, select.error):
                    break
            return ''.join(output)

    result = ssh_with_password('root@121.4.54.188', 'Wzp0201.', 'systemctl status nacos 2>&1 | head -30')
    print(result)
