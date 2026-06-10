import subprocess
import sys
import os

def check_output():
    """Run SSH commands using subprocess with Windows OpenSSH"""

    commands = [
        'systemctl status nacos 2>&1 | head -30',
        'grep -v "^#" /home/cloudapp/application/nacos/conf/application.properties | grep -v "^$"',
        'cat /etc/systemd/system/nacos.service',
        'ps aux | grep "[n]acos" | head -5',
        'cat /home/cloudapp/application/nacos/conf/cluster.conf 2>/dev/null || echo "no cluster.conf"',
        'curl -s http://127.0.0.1:8848/nacos/v1/cs/health 2>&1 || echo "health check failed"'
    ]

    host = 'root@121.4.54.188'
    ssh_key = os.path.expanduser('~/.ssh/id_rsa')

    # Try key-based auth first (if key exists)
    if os.path.exists(ssh_key):
        print(f"Found SSH key: {ssh_key}")
        for cmd in commands:
            print(f"\n=== Running: {cmd[:50]}... ===")
            p = subprocess.run(
                ['ssh', '-o', 'StrictHostKeyChecking=no', '-o', 'BatchMode=yes', host, cmd],
                capture_output=True, text=True, timeout=15
            )
            if p.stdout:
                print(p.stdout[:1000])
            if p.stderr:
                print("STDERR:", p.stderr[:500])
            print(f"RC: {p.returncode}")
    else:
        print(f"No SSH key found at {ssh_key}")
        print("Cannot authenticate without sshpass or SSH key")

if __name__ == '__main__':
    check_output()
