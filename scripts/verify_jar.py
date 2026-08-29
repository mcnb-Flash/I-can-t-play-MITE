import sys, zipfile

path = sys.argv[1]
try:
    with zipfile.ZipFile(path, "r") as z:
        bad = z.testzip()  # 仅校验 CRC，不读全量 local header
        names = z.namelist()
        # 全量读取每个 entry 的字节，确保 local header 也完整（防 invalid LOC header）
        total = 0
        for n in names:
            data = z.read(n)
            total += len(data)
    print(f"OK  entries={len(names)}  bytes_read={total}  testzip={bad}")
except Exception as e:
    print(f"FAIL  {type(e).__name__}: {e}")
    sys.exit(1)
