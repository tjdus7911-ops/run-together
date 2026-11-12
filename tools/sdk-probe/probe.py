#!/usr/bin/env python3
"""Pleos Playground 공개 Maven 저장소에서 ai.pleos.playground SDK 목록과 공개 API를 뽑는다.

사용: python3 tools/sdk-probe/probe.py <출력 폴더>
"""
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

BASE = 'https://nexus-playground.pleos.ai'
REPO = 'maven-releases'
GROUP = 'ai.pleos.playground'
GUESSES = ['NaviHelper', 'Vehicle', 'TextToSpeech', 'SpeechToText', 'FusedLocation',
           'ADAS', 'Adas', 'LLM', 'Llm', 'Sync', 'vehicle', 'navihelper', 'tts', 'stt']

OUT = sys.argv[1] if len(sys.argv) > 1 else 'probe-out'
os.makedirs(OUT, exist_ok=True)
summary = []


def log(msg):
    print(msg, flush=True)
    summary.append(msg)


def get(url, binary=True):
    req = urllib.request.Request(url, headers={'User-Agent': 'run-together-sdk-probe'})
    with urllib.request.urlopen(req, timeout=90) as r:
        data = r.read()
    return data if binary else data.decode('utf-8', 'replace')


def version_key(v):
    return [int(p) if p.isdigit() else p for p in re.split(r'[.\-]', v)]


# 1) Nexus 검색 API로 그룹 전체 목록
artifacts = {}  # name -> set(versions)
try:
    token = None
    while True:
        q = {'repository': REPO, 'group': GROUP}
        if token:
            q['continuationToken'] = token
        data = json.loads(get(f'{BASE}/service/rest/v1/search?{urllib.parse.urlencode(q)}', binary=False))
        for item in data.get('items', []):
            artifacts.setdefault(item['name'], set()).add(item['version'])
        token = data.get('continuationToken')
        if not token:
            break
    log(f'search api: {len(artifacts)} artifacts')
except Exception as e:  # noqa: BLE001
    log(f'search api failed: {e}')

# 2) 검색이 막혀 있으면 이름을 추측해 maven-metadata.xml 확인
if not artifacts:
    for name in GUESSES:
        url = f'{BASE}/repository/{REPO}/{GROUP.replace(".", "/")}/{name}/maven-metadata.xml'
        try:
            root = ET.fromstring(get(url))
            versions = [v.text for v in root.iter('version')]
            artifacts[name] = set(versions)
            log(f'metadata ok: {name} {versions}')
        except Exception as e:  # noqa: BLE001
            log(f'metadata miss: {name} ({e})')

with open(os.path.join(OUT, 'artifacts.json'), 'w') as f:
    json.dump({k: sorted(v, key=version_key) for k, v in sorted(artifacts.items())}, f, indent=2)


def dump_api(name, version):
    path = f'{BASE}/repository/{REPO}/{GROUP.replace(".", "/")}/{name}/{version}/{name}-{version}'
    out_lines = [f'# {GROUP}:{name}:{version}']
    try:
        pom = get(path + '.pom', binary=False)
        out_lines += ['## pom', pom]
    except Exception as e:  # noqa: BLE001
        out_lines.append(f'(pom unavailable: {e})')
    blob, kind = None, None
    for ext in ('.aar', '.jar'):
        try:
            blob, kind = get(path + ext), ext
            break
        except Exception:  # noqa: BLE001
            continue
    if blob is None:
        out_lines.append('(no aar/jar found)')
        return '\n'.join(out_lines)
    with tempfile.TemporaryDirectory() as tmp:
        jar = os.path.join(tmp, 'classes.jar')
        if kind == '.aar':
            with zipfile.ZipFile(io.BytesIO(blob)) as z:
                names = z.namelist()
                out_lines += ['## aar entries'] + names
                if 'AndroidManifest.xml' in names:
                    out_lines += ['## AndroidManifest.xml', z.read('AndroidManifest.xml').decode('utf-8', 'replace')]
                if 'classes.jar' not in names:
                    out_lines.append('(aar has no classes.jar)')
                    return '\n'.join(out_lines)
                with open(jar, 'wb') as f:
                    f.write(z.read('classes.jar'))
        else:
            with open(jar, 'wb') as f:
                f.write(blob)
        with zipfile.ZipFile(jar) as z:
            classes = sorted(n[:-6].replace('/', '.') for n in z.namelist()
                             if n.endswith('.class') and not re.search(r'\$\d', n))
        out_lines.append(f'## {len(classes)} classes')
        for i in range(0, len(classes), 40):
            chunk = classes[i:i + 40]
            r = subprocess.run(['javap', '-public', '-cp', jar] + chunk, capture_output=True, text=True)
            out_lines.append(r.stdout)
            if r.stderr.strip():
                out_lines.append('stderr: ' + r.stderr.strip()[:2000])
    return '\n'.join(out_lines)


for name, versions in sorted(artifacts.items()):
    latest = sorted(versions, key=version_key)[-1]
    text = dump_api(name, latest)
    safe = re.sub(r'[^A-Za-z0-9_.-]', '_', f'{name}-{latest}')
    with open(os.path.join(OUT, safe + '.txt'), 'w') as f:
        f.write(text)
    log(f'dumped {name}:{latest} ({len(text)} chars)')

with open(os.path.join(OUT, 'README.txt'), 'w') as f:
    f.write('\n'.join(summary) + '\n')
