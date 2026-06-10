import subprocess, os, time

lock = r"D:\mcay\claudeCode001\.git\index.lock"
for _ in range(5):
    try:
        os.remove(lock)
    except FileNotFoundError:
        pass
    time.sleep(0.5)

files = [
    "backend/src/main/java/com/example/aimanager/controller/ChatController.java",
    "backend/src/main/java/com/example/aimanager/config/SecurityConfig.java",
    "backend/src/main/java/com/example/aimanager/service/AiChatService.java",
    "backend/src/main/java/com/example/aimanager/common/Result.java",
    "backend/src/main/java/com/example/aimanager/config/PublicPathsProperties.java",
    "backend/src/main/java/com/example/aimanager/util/AesUtil.java",
]

git_dir = r"D:\mcay\claudeCode001"
for f in files:
    r = subprocess.run(["git", "checkout", "--", f], cwd=git_dir, capture_output=True)
    print(f, r.returncode, r.stderr.decode()[:100])
