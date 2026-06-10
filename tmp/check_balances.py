import paramiko, json, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('122.51.136.129', 22, 'root', 'Wzp0201.', look_for_keys=False, allow_agent=False)

# Login
stdin, stdout, stderr = ssh.exec_command(
    'curl -s -X POST http://127.0.0.1:8080/api/auth/login '
    '-H "Content-Type: application/json" '
    '-d \'{"username":"admin","password":"admin123","loginType":"admin"}\''
)
login_resp = stdout.read().decode().strip()
print('Login:', login_resp[:100])

try:
    login_data = json.loads(login_resp)
    token = login_data.get('data', {}).get('token', '')
except:
    token = ''

if token:
    # Test balances
    stdin, stdout, stderr = ssh.exec_command(
        f'curl -s -H "Authorization: Bearer {token}" '
        f'http://127.0.0.1:8080/api/admin/models/balances'
    )
    resp = stdout.read().decode().strip()
    try:
        data = json.loads(resp)
        models = data.get('data', [])
        print(f'\nBalances ({len(models)} models):')
        for m in models:
            name = m.get('name', '?')
            avail = m.get('available', '?')
            bal = m.get('balance', {})
            print(f'  {name}: available={avail}, balance={bal.get("total", "N/A")}')
    except Exception as e:
        print(f'Balance parse error: {e}')
        print(resp[:300])

    # Test refresh timing
    start = time.time()
    stdin, stdout, stderr = ssh.exec_command(
        'curl -s -X POST http://127.0.0.1:8080/api/models/refresh'
    )
    refresh_resp = stdout.read().decode().strip()
    elapsed = time.time() - start
    print(f'\nRefresh took {elapsed:.1f}s')
    try:
        data = json.loads(refresh_resp)
        statuses = data.get('data', [])
        for s in statuses[:5]:
            print(f'  model {s["id"]}: available={s["available"]}, latency={s["latency"]}ms')
    except Exception as e:
        print(f'Refresh parse error: {e}')

ssh.close()
