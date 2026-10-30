"""Package only committed source and known build outputs; never a whole workspace."""
from __future__ import annotations

import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'artifacts'
FORBIDDEN_PATH = re.compile(
    r'(^|/)(?:\.env(?:\..*)?|local\.properties|id_rsa|id_ed25519|credentials[^/]*|'
    r'service-account[^/]*|.*\.(?:jks|keystore|p12|pfx|pem|key))$', re.I
)
SECRET_PATTERNS = [
    rb'-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----',
    rb'gh[pousr]_[A-Za-z0-9]{20,}',
    rb'github_pat_[A-Za-z0-9_]{20,}',
    rb'AKIA[A-Z0-9]{16}',
]


def scan(name: str, data: bytes) -> None:
    if FORBIDDEN_PATH.search(name):
        raise RuntimeError(f'Excluded sensitive file: {name}')
    if any(re.search(pattern, data) for pattern in SECRET_PATTERNS):
        raise RuntimeError(f'Potential credential in {name}; do not upload')
    for key in ('GH_TOKEN', 'GITHUB_TOKEN', 'ACTIONS_RUNTIME_TOKEN'):
        value = os.environ.get(key, '')
        if len(value) >= 16 and value.encode() in data:
            raise RuntimeError(f'Known credential found in {name}; do not upload')


def command(*args: str) -> str:
    return subprocess.check_output(args, cwd=ROOT, text=True, stderr=subprocess.STDOUT).strip()


def main() -> None:
    if command('git', 'status', '--porcelain', '--untracked-files=no'):
        raise RuntimeError('Tracked source differs from HEAD; refuse a mismatched source ZIP')
    paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    paths = [name for name in paths if name]
    for name in paths:
        if re.search(r'\.(?:apk|aab|zip|7z)$', name, re.I):
            raise RuntimeError(f'Prebuilt deliverable tracked in source: {name}')
        scan(name, (ROOT / name).read_bytes())

    # A passing runner with no tests is not readiness evidence.
    results = sorted((ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    totals = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
    for file in results:
        suite = ET.parse(file).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, '0'))
    if totals['tests'] == 0 or totals['failures'] or totals['errors']:
        raise RuntimeError(f'Unit tests did not pass: {totals}')

    if OUT.exists():
        shutil.rmtree(OUT)  # Own generated, ignored output directory only.
    OUT.mkdir()
    outputs = {
        'CalculatorKidKid-0.1.0-debug.apk': 'app/build/outputs/apk/debug/app-debug.apk',
        'CalculatorKidKid-0.1.0-UNSIGNED.aab': 'app/build/outputs/bundle/release/app-release.aab',
    }
    for name, relative in outputs.items():
        source = ROOT / relative
        if not source.is_file():
            raise RuntimeError(f'Missing build output: {relative}')
        shutil.copy2(source, OUT / name)
        # Scan archive members, including binary entries, without logging contents.
        with zipfile.ZipFile(source) as archive:
            if archive.testzip() is not None:
                raise RuntimeError(f'Invalid archive: {relative}')
            for entry in archive.infolist():
                if not entry.is_dir():
                    scan(name + '/' + entry.filename, archive.read(entry))

    # git archive records exactly the built HEAD, not local caches or keys.
    archive_data = subprocess.check_output(
        ['git', 'archive', '--format=zip', '--prefix=CalculatorKidKid/', 'HEAD'], cwd=ROOT
    )
    with zipfile.ZipFile(io.BytesIO(archive_data)) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Invalid source archive')
        for name in archive.namelist():
            if not name.endswith('/'):
                scan(name, archive.read(name))
    (OUT / 'CalculatorKidKid-0.1.0-source.zip').write_bytes(archive_data)
    shutil.copytree(ROOT / 'docs', OUT / 'docs')
    reports = OUT / 'verification'
    reports.mkdir()
    for file in results:
        shutil.copy2(file, reports / file.name)
    lint = ROOT / 'app/build/reports/lint-results-debug.txt'
    if not lint.is_file():
        raise RuntimeError('Missing lint results')
    shutil.copy2(lint, reports / lint.name)
    screenshots = ROOT / 'app/build/reports/screenshots'
    if screenshots.is_dir():
        for file in screenshots.glob('*.png'):
            shutil.copy2(file, reports / file.name)

    info = {
        'source_commit': command('git', 'rev-parse', 'HEAD'),
        'java': command('java', '-version'),
        'gradle': command('./gradlew', '--version'),
        'android_compile_sdk': 35,
        'android_target_sdk': 35,
        'android_min_sdk': 28,
        'android_gradle_plugin': '8.7.3',
        'kotlin': '2.0.21',
        'room': '2.6.1',
        'junit': '4.13.2',
        'robolectric': '4.14.1',
        'test_results': totals,
        'release_signing': 'UNSIGNED; no production signing configured',
        'device_acceptance': 'NOT RUN by this workflow; Robolectric is not a device test',
    }
    sdk = Path(os.environ['ANDROID_HOME'])
    info['sdk_packages'] = {
        package: (sdk / package / 'source.properties').read_text()
        for package in ('platforms/android-35', 'build-tools/34.0.0', 'build-tools/35.0.0')
    }
    (OUT / 'BUILD_INFO.json').write_text(json.dumps(info, indent=2) + '\n')
    (OUT / 'START_HERE.txt').write_text(
        'Calculator Kid Kid 0.1.0 development test delivery\n\n'
        'Install the debug APK on Android 9+. The AAB is unsigned and not uploadable to Play.\n'
        'Extract the explicitly named source ZIP for the exact built commit.\n'
        'BUILD_INFO.json records toolchain and automated test results.\n'
        'Physical-device acceptance and unresolved requirements remain outstanding.\n'
        'See docs/PHYSICAL_DEVICE_ACCEPTANCE.md and docs/REMAINING_VERIFICATION.md.\n'
        'No production key or signing credentials are included.\n'
    )
    files = sorted(path for path in OUT.rglob('*') if path.is_file())
    for file in files:
        scan(str(file.relative_to(OUT)), file.read_bytes())
    checksums = ''.join(
        hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + str(file.relative_to(OUT)) + '\n'
        for file in files
    )
    (OUT / 'SHA256SUMS').write_text(checksums)
    bundle = OUT / 'CalculatorKidKid-0.1.0-All-Deliverables.zip'
    with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in files + [OUT / 'SHA256SUMS']:
            archive.write(file, Path('CalculatorKidKid-0.1.0') / file.relative_to(OUT))
    with zipfile.ZipFile(bundle) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Invalid combined delivery ZIP')
    print(f'Packaged source commit {info["source_commit"]}; test results: {totals}')
    print('Generated APK, unsigned AAB, source ZIP, docs, reports and combined ZIP in artifacts/.')


if __name__ == '__main__':
    main()
