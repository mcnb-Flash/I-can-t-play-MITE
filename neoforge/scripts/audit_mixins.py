# -*- coding: utf-8 -*-
"""NeoForge 侧 mixin 审计：核对每个 mixin 的注入目标是否真实存在于 1.21.11 mojmap(+NeoForge patch) 类中。

检查项：
  1. @Inject/@Redirect/@ModifyArg(s)/@ModifyVariable/@WrapOperation/@Overwrite 的 method= 选择器
     是否能在目标类（含父类/接口链）中找到（给描述符则精确匹配，仅名字则按名匹配）。
  2. @At(INVOKE/FIELD) target= 描述符指向的成员是否存在（防方法改名导致的 "Scanned 0 targets"）。
  3. @Accessor/@Invoker 的字段/方法是否存在。

用法：python scripts/audit_mixins.py [--verbose]
"""
import os
import re
import struct
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
NEOFORGE = os.path.dirname(HERE)
CLIENT_JAR = r"E:\.minecraft\libraries\net\neoforged\minecraft-client-patched\21.11.45\minecraft-client-patched-21.11.45.jar"
NEOFORGE_JAR = r"E:\.minecraft\libraries\net\neoforged\neoforge\21.11.45\neoforge-21.11.45-universal.jar"
SRC_DIRS = [
    os.path.join(NEOFORGE, "src", "main", "java", "name", "icpm", "mixin"),
    os.path.join(NEOFORGE, "src", "main", "java", "name", "icpm", "client", "mixin"),
]

# ---------------- class file parsing ----------------

def parse_class(data):
    """返回 (this_name, super_name, interfaces, methods, fields)；methods/fields = {name: set(desc)}"""
    off = 8
    cp = {}
    cp_count = struct.unpack_from(">H", data, off)[0]
    off += 2
    i = 1
    while i < cp_count:
        tag = data[off]
        off += 1
        if tag == 1:  # Utf8
            ln = struct.unpack_from(">H", data, off)[0]
            off += 2
            cp[i] = data[off:off + ln].decode("utf-8", "replace")
            off += ln
        elif tag in (7, 8, 16, 19, 20):  # Class, String, MethodType, Module, Package
            cp[i] = struct.unpack_from(">H", data, off)[0]
            off += 2
        elif tag in (15,):  # MethodHandle
            off += 3
        elif tag in (9, 10, 11, 12):  # Fieldref / Methodref / InterfaceMethodref / NameAndType
            a = struct.unpack_from(">H", data, off)[0]
            b = struct.unpack_from(">H", data, off + 2)[0]
            cp[i] = (a, b)
            off += 4
        elif tag in (3, 4, 17, 18):  # int/float/Dynamic/InvokeDynamic
            off += 4
        elif tag in (5, 6):  # long/double take two slots
            off += 8
            i += 1
        else:
            raise ValueError("bad cp tag %d" % tag)
        i += 1

    def utf(idx):
        v = cp.get(idx)
        while isinstance(v, int) and v in cp and not isinstance(cp[v], str):
            v = cp[v]
        return v if isinstance(v, str) else ""

    def cls(idx):
        return utf(cp.get(idx, 0)).replace("/", ".")

    off += 2  # access_flags
    this_name = cls(struct.unpack_from(">H", data, off)[0])
    off += 2
    super_name = cls(struct.unpack_from(">H", data, off)[0])
    off += 2
    ifc = []
    n = struct.unpack_from(">H", data, off)[0]
    off += 2
    for _ in range(n):
        ifc.append(cls(struct.unpack_from(">H", data, off)[0]))
        off += 2

    def members(off, want_code=False):
        out = {}
        code = {}
        n = struct.unpack_from(">H", data, off)[0]
        off += 2
        for _ in range(n):
            off += 2  # access
            name = utf(struct.unpack_from(">H", data, off)[0])
            off += 2
            desc = utf(struct.unpack_from(">H", data, off)[0])
            off += 2
            an = struct.unpack_from(">H", data, off)[0]
            off += 2
            for _ in range(an):
                aname = utf(struct.unpack_from(">H", data, off)[0])
                alen = struct.unpack_from(">I", data, off + 2)[0]
                abody = data[off + 6:off + 6 + alen]
                off += 6 + alen
                if want_code and aname == "Code" and len(abody) > 8:
                    clen = struct.unpack_from(">I", abody, 4)[0]
                    code.setdefault((name, desc), set()).update(
                        scan_code(abody[8:8 + clen], cp, utf))
            out.setdefault(name, set()).add(desc)
        return out, code, off

    fields, _, off = members(off)
    methods, code, off = members(off, want_code=True)
    return this_name, super_name, ifc, methods, fields, code


# ---------------- JVM bytecode scan（取注入点用：invoke/field 引用） ----------------

FIXED = {16: 2, 17: 3, 18: 2, 19: 3, 20: 3, 132: 3, 153: 3, 154: 3, 155: 3, 156: 3, 157: 3,
         158: 3, 159: 3, 160: 3, 161: 3, 162: 3, 163: 3, 164: 3, 165: 3, 166: 3, 167: 3,
         168: 3, 169: 2, 178: 3, 179: 3, 180: 3, 181: 3, 182: 3, 183: 3, 184: 3, 185: 5,
         186: 5, 187: 3, 188: 2, 189: 3, 192: 3, 193: 3, 197: 4, 198: 3, 199: 3,
         200: 5, 201: 5}
for _o in list(range(21, 26)) + list(range(54, 59)):
    FIXED[_o] = 2


def scan_code(code, cp, utf):
    refs = set()
    i = 0
    n = len(code)
    while i < n:
        op = code[i]
        if op == 170:  # tableswitch
            pad = (4 - (i + 1) % 4) % 4
            base = i + 1 + pad
            low = struct.unpack_from(">i", code, base + 4)[0]
            high = struct.unpack_from(">i", code, base + 8)[0]
            i = base + 12 + 4 * (high - low + 1)
            continue
        if op == 171:  # lookupswitch
            pad = (4 - (i + 1) % 4) % 4
            base = i + 1 + pad
            npairs = struct.unpack_from(">i", code, base + 4)[0]
            i = base + 8 + 8 * npairs
            continue
        if op == 196:  # wide
            i += 6 if code[i + 1] == 132 else 4
            continue
        ln = FIXED.get(op, 1)
        if op in (178, 179, 180, 181, 182, 183, 184, 185):
            idx = struct.unpack_from(">H", code, i + 1)[0]
            e = cp.get(idx)
            if isinstance(e, tuple):
                owner = cp.get(e[0])
                nt = cp.get(e[1])
                if isinstance(owner, int):
                    owner = cp.get(owner)
                if isinstance(nt, tuple):
                    nm, ds = cp.get(nt[0]), cp.get(nt[1])
                    if isinstance(owner, str) and isinstance(nm, str) and isinstance(ds, str):
                        sep = ":" if op >= 178 and op <= 181 else ""
                        sym = "%s.%s%s%s" % (owner.replace("/", "."), nm, sep, ds)
                        refs.add(sym)
        i += ln
    return refs


class Index:
    def __init__(self, jars):
        self.methods = {}
        self.fields = {}
        self.supers = {}
        self.code = {}
        self.missing = set()
        for j in jars:
            if not os.path.exists(j):
                continue
            with zipfile.ZipFile(j) as z:
                for n in z.namelist():
                    if not n.endswith(".class") or n.startswith("META-INF"):
                        continue
                    name = n[:-6]
                    try:
                        t, s, ifc, m, f, c = parse_class(z.read(n))
                    except Exception:
                        continue
                    if t:
                        self.methods.setdefault(t, {}).update(m)
                        self.fields.setdefault(t, {}).update(f)
                        self.code.setdefault(t, {}).update(c)
                        self.supers[t] = (s, ifc)

    def _chain(self, cls):
        seen, stack = set(), [cls]
        while stack:
            c = stack.pop()
            if not c or c in seen:
                continue
            seen.add(c)
            s, ifc = self.supers.get(c, ("", []))
            stack.append(s)
            stack.extend(ifc or [])
        return seen

    def has_method(self, cls, name, desc=None):
        for c in self._chain(cls):
            ds = self.methods.get(c, {}).get(name)
            if ds and (desc is None or desc in ds):
                return True
        return False

    def has_field(self, cls, name, desc=None):
        for c in self._chain(cls):
            ds = self.fields.get(c, {}).get(name)
            if ds and (desc is None or desc in ds):
                return True
        return False

    def code_refs(self, cls, name):
        """目标类自身 code 中所有 invoke/field 引用（按方法名过滤），用于验证注入点存在"""
        out = set()
        for (mn, md), refs in self.code.get(cls, {}).items():
            if mn == name:
                out |= refs
        return out

    def known(self, cls):
        return cls in self.methods or cls in self.supers


# ---------------- mixin source parsing ----------------

DESC = re.compile(r"\([^)]*\)[^\s;]+")


def strip_comments(src):
    src = re.sub(r"//.*", "", src)
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return src


def line_of(src, idx):
    return src.count("\n", 0, idx) + 1


def parse_imports(src):
    out = {}
    for m in re.finditer(r"^import\s+(?:static\s+)?([\w.$]+)\.(\w+);", src, re.M):
        out[m.group(2)] = m.group(1) + "." + m.group(2)
    return out


def resolve(name, imports, pkg):
    name = name.strip()
    if name.endswith(".class"):
        name = name[:-6]
    if "." in name:
        return name
    if name in imports:
        return imports[name]
    return pkg + "." + name


def targets_of(body, imports, pkg):
    out = []
    m = re.search(r"@Mixin\s*\(([^)]*)\)", body, re.S)
    if not m:
        return out
    arg = m.group(1)
    t = re.findall(r"targets\s*=\s*[\"']([\w.$]+)[\"']", arg)
    if t:
        out.extend(t)
    for g in re.findall(r"([\w.$]+)\.class", arg):
        out.append(resolve(g, imports, pkg))
    return out


def balanced_args(src, open_idx):
    """open_idx 指向 '('，返回括号内文本（先把 "a" + "b" 形式拼接合并）"""
    depth = 0
    i = open_idx
    while i < len(src):
        c = src[i]
        if c == '"' or c == "'":
            q, i = c, i + 1
            while i < len(src) and src[i] != q:
                i += 2 if src[i] == "\\" else 1
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return join_literals(src[open_idx + 1:i])
        i += 1
    return ""


def join_literals(arg):
    """把 "a" + "b" 形式的相邻字符串字面量拼成一个"abc" """
    def repl(m):
        return '"%s"' % (m.group(1) + m.group(2))
    prev = None
    while prev != arg:
        prev = arg
        arg = re.sub(r'"([^"]*)"\s*\+\s*"([^"]*)"', repl, arg)
    return arg


def injectors(body, imports=None, pkg=""):
    """产出 (kind, method_selector, [at_targets], line)"""
    out = []
    kinds = ("Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyVariable", "ModifyConstant",
             "WrapOperation", "WrapWithCondition", "WrapMethod", "Overwrite", "ModifyExpressionValue")
    for m in re.finditer(r"@(%s)\s*\(" % "|".join(kinds), body):
        args = balanced_args(body, body.index("(", m.start()))
        sel = re.search(r"method\s*=\s*[\"']([^\"']+)[\"']", args)
        ats = [t.group(1) for t in re.finditer(r"target\s*=\s*[\"']([^\"']+)[\"']", args)]
        out.append((m.group(1), sel.group(1) if sel else None, ats, line_of(body, m.start())))
    return out


def selectors(body):
    return [(k, s, ln) for k, s, _a, ln in injectors(body)]


def at_targets(body):
    out = []
    for m in re.finditer(r"@At\s*\(", body):
        args = balanced_args(body, body.index("(", m.start()))
        tgt = re.search(r"target\s*=\s*[\"']([^\"']+)[\"']", args)
        if tgt:
            out.append((tgt.group(1), line_of(body, m.start())))
    return out


def member_refs(body):
    out = []
    for m in re.finditer(r"@(Accessor|Invoker)\s*\(\s*[\"']([\w$]+)[\"']", body):
        out.append((m.group(1), m.group(2), line_of(body, m.start())))
    for m in re.finditer(r"@(Accessor|Invoker)\s*\(\s*value\s*=\s*[\"']([\w$]+)[\"']", body):
        out.append((m.group(1), m.group(2), line_of(body, m.start())))
    return out


def registered_mixins():
    """从 *.mixins.json 收集已注册的 mixin 简单类名（未注册文件不参与审计）"""
    names = set()
    for root, _, files in os.walk(os.path.join(NEOFORGE, "src", "main", "resources")):
        for f in files:
            if not f.endswith(".mixins.json"):
                continue
            import json as _json
            try:
                data = _json.load(open(os.path.join(root, f), encoding="utf-8-sig"))
            except Exception:
                continue
            for k, v in data.items():
                if isinstance(v, list):
                    names.update(x for x in v if isinstance(x, str))
    return names


def main():
    verbose = "--verbose" in sys.argv
    idx = Index([CLIENT_JAR, NEOFORGE_JAR])
    if not idx.supers:
        print("[FATAL] 未能加载原版类索引，检查 CLIENT_JAR 路径")
        return 2
    registered = registered_mixins()
    print("类索引：%d 个类；已注册 mixin：%d 个" % (len(idx.supers), len(registered)))

    files = []
    for d in SRC_DIRS:
        if os.path.isdir(d):
            for root, _, names in os.walk(d):
                for n in sorted(names):
                    if n.endswith(".java"):
                        files.append(os.path.join(root, n))
    skipped = [f for f in files if os.path.basename(f)[:-5] not in registered]
    files = [f for f in files if os.path.basename(f)[:-5] in registered]
    if skipped:
        print("跳过未注册 mixin 文件 %d 个：%s" % (
            len(skipped), ", ".join(sorted(os.path.basename(f)[:-5] for f in skipped))))

    problems = []
    checked = 0
    for path in files:
        raw = open(path, encoding="utf-8", errors="replace").read()
        src = strip_comments(raw)
        pkg = (re.search(r"^package\s+([\w.]+)\s*;", src, re.M) or [None, ""])[1]
        imports = parse_imports(src)
        tgts = targets_of(src, imports, pkg)
        tgts = [t for t in tgts if t.startswith("net.minecraft") or t.startswith("net.neoforged")]
        if not tgts:
            continue
        rel = os.path.relpath(path, NEOFORGE)

        for kind, sel, ats, ln in injectors(src, imports, pkg):
            checked += 1
            if not sel or sel.startswith("<"):
                continue
            parts = sel.split()
            name_desc = parts[-1]
            if "(" in name_desc:
                name, desc = name_desc.split("(", 1)
                desc = "(" + desc
            else:
                name, desc = name_desc, None
            for t in tgts:
                if not idx.known(t):
                    continue
                if not idx.has_method(t, name, desc):
                    problems.append("%s:%d  %s method=\"%s\" 在 %s 中不存在" % (rel, ln, kind, sel, t))
                    continue
                # 注入点必须真实出现在目标方法体内（否则运行期 "Scanned 0 targets"）
                refs = idx.code_refs(t, name)
                for at in ats:
                    if not at.startswith("L"):
                        continue
                    owner, rest = at[1:].split(";", 1)
                    owner = owner.replace("/", ".")
                    m2 = re.match(r"([\w$<>]+)(\(.*)", rest)
                    if not m2:
                        continue
                    mname, mdesc = m2.group(1), m2.group(2)
                    sym = "%s.%s%s" % (owner, mname, mdesc)
                    if sym not in refs:
                        problems.append(
                            "%s:%d  @At 注入点 %s 不在 %s.%s 的代码中（该调用不存在/方法已重构）"
                            % (rel, ln, sym, t, name))
        for tgt, ln in at_targets(src):
            if tgt.startswith("L"):
                owner, rest = tgt[1:].split(";", 1)
                owner = owner.replace("/", ".")
                m2 = re.match(r"([\w$<>]+)(\(.*)", rest)
                if m2:
                    name, desc = m2.group(1), m2.group(2)
                    checked += 1
                    if idx.known(owner) and not idx.has_method(owner, name, desc):
                        problems.append("%s:%d  @At target %s.%s%s 不存在" % (rel, ln, owner, name, desc))
        for kind, member, ln in member_refs(src):
            checked += 1
            for t in tgts:
                if not idx.known(t):
                    continue
                ok = idx.has_field(t, member) if kind == "Accessor" else idx.has_method(t, member)
                if not ok:
                    problems.append("%s:%d  @%s(\"%s\") 在 %s 中不存在" % (rel, ln, kind, member, t))

    print("检查注入点：%d 个，涉及 mixin 文件 %d 个" % (checked, len(files)))
    if problems:
        print("\n发现 %d 个问题：" % len(problems))
        for p in problems:
            print("  " + p)
        return 1
    print("全部注入点均可解析 ✓")
    return 0


if __name__ == "__main__":
    sys.exit(main())
