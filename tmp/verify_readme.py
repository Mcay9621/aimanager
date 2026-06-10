import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('122.51.136.129', 22, 'root', 'Wzp0201.', look_for_keys=False, allow_agent=False)

stdin, stdout, stderr = ssh.exec_command('cat /home/cloudapp/application/README.md')
content = stdout.read().decode('utf-8', errors='replace')
for line in content.split('\n'):
    if any(kw in line for kw in ['AI Manager', '项目目录', '服务列表', '配置文件', '关键路径', '数据库', 'Redis', '默认管理员', '部署命令', '查看日志', '---', '```']):
        print(line[:100])
ssh.close()
