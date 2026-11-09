#!/usr/bin/env python3
"""
ChordPlay Translation Synchronization & Audit Tool
==================================================

Audits, exports, imports, and synchronizes string resources between
base English values/strings.xml and all localized values-*/strings.xml
across both the `app` module and `sharedUi` Compose Multiplatform module.

Usage:
  python tools/sync_translations.py --check
  python tools/sync_translations.py --export-missing missing.json
  python tools/sync_translations.py --import-translations translations.json
  python tools/sync_translations.py --prune-obsolete
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, List, Set, Tuple

PROJECT_ROOT = Path(__file__).resolve().parent.parent

APP_RES_DIR = PROJECT_ROOT / "app" / "src" / "main" / "res"
SHARED_RES_DIR = PROJECT_ROOT / "sharedUi" / "src" / "commonMain" / "composeResources"

NON_LOCALE_DIRS = {"values", "values-night", "values-v31", "values-land", "values-sw600dp", "values-w820dp"}

# Language tags to qualifier folders mapping
# BCP-47 / Android folder rules
ALL_APP_LOCALES = [
    "values-b+zh+Hant",
    "values-de",
    "values-es",
    "values-fr",
    "values-he",
    "values-hi",
    "values-id",
    "values-it",
    "values-ja",
    "values-pl",
    "values-pt",
    "values-ru",
    "values-th",
    "values-tr",
    "values-vi",
    "values-zh",
    "values-zh-rHK",
    "values-zh-rTW",
]

ALL_SHARED_LOCALES = [
    "values-de",
    "values-es",
    "values-fr",
    "values-he",
    "values-hi",
    "values-id",
    "values-it",
    "values-ja",
    "values-pl",
    "values-pt",
    "values-ru",
    "values-th",
    "values-tr",
    "values-vi",
    "values-zh",
    "values-zh-rHK",
    "values-zh-rTW",
]


def escape_android_xml(text: str) -> str:
    """Escape text according to Android strings.xml rules."""
    if not text:
        return ""
    # Already escaped entities should be handled carefully
    # First unescape common entities to avoid double escaping
    s = text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    # Handle single quotes: in Android strings.xml, an unquoted text with ' must be escaped as \'
    # If the text is wrapped in double quotes, ' doesn't need to be escaped, but \' is always valid
    # Avoid double escaping \\'
    s = re.sub(r"(?<!\\)'", r"\'", s)
    # Question marks and @ at start of string
    if s.startswith("?"):
        s = "\\" + s
    if s.startswith("@"):
        s = "\\" + s
    return s


def parse_strings_xml(file_path: Path) -> Dict[str, dict]:
    """Parse a strings.xml file into a dictionary of {name: metadata}."""
    if not file_path.exists():
        return {}

    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
    except Exception as e:
        print(f"Error parsing {file_path}: {e}", file=sys.stderr)
        return {}

    entries = {}
    for elem in root:
        name = elem.get("name")
        if not name:
            continue
        if elem.get("translatable") == "false":
            continue

        tag = elem.tag
        attrib = dict(elem.attrib)

        if tag == "string":
            raw_text = "".join(elem.itertext())
            entries[name] = {
                "tag": "string",
                "text": raw_text,
                "attrib": attrib,
            }
        elif tag == "string-array":
            items = ["".join(it.itertext()) for it in elem.findall("item")]
            entries[name] = {
                "tag": "string-array",
                "items": items,
                "attrib": attrib,
            }
        elif tag == "plurals":
            items = {it.get("quantity", "other"): "".join(it.itertext()) for it in elem.findall("item")}
            entries[name] = {
                "tag": "plurals",
                "items": items,
                "attrib": attrib,
            }
    return entries


def audit_module(res_dir: Path, target_locales: List[str]) -> Tuple[int, Dict[str, Set[str]], Dict[str, Set[str]]]:
    """Audit a module resource directory against target locales."""
    base_file = res_dir / "values" / "strings.xml"
    base_entries = parse_strings_xml(base_file)
    base_keys = set(base_entries.keys())

    missing_by_locale = {}
    obsolete_by_locale = {}

    for loc in target_locales:
        loc_file = res_dir / loc / "strings.xml"
        loc_entries = parse_strings_xml(loc_file)
        loc_keys = set(loc_entries.keys())

        missing = base_keys - loc_keys
        obsolete = loc_keys - base_keys

        missing_by_locale[loc] = missing
        obsolete_by_locale[loc] = obsolete

    return len(base_keys), missing_by_locale, obsolete_by_locale


def print_audit_report():
    """Print an audit report of all modules and locales."""
    print("=" * 75)
    print(" ChordPlay Localization Audit Report")
    print("=" * 75)

    app_total, app_missing, app_obsolete = audit_module(APP_RES_DIR, ALL_APP_LOCALES)
    print(f"\n[app module] Base English Keys: {app_total}")
    print(f"{'Locale Qualifier':<22} | {'Missing Keys':<14} | {'Obsolete Keys':<14} | {'Status'}")
    print("-" * 75)
    app_has_issues = False
    for loc, missing in app_missing.items():
        obs = app_obsolete[loc]
        status = "OK" if len(missing) == 0 else f"MISSING {len(missing)}"
        if len(obs) > 0:
            status += f" (+{len(obs)} obs)"
        if len(missing) > 0:
            app_has_issues = True
        print(f"{loc:<22} | {len(missing):<14} | {len(obs):<14} | {status}")

    shared_total, shared_missing, shared_obsolete = audit_module(SHARED_RES_DIR, ALL_SHARED_LOCALES)
    print(f"\n[sharedUi module] Base English Keys: {shared_total}")
    print(f"{'Locale Qualifier':<22} | {'Missing Keys':<14} | {'Obsolete Keys':<14} | {'Status'}")
    print("-" * 75)
    shared_has_issues = False
    for loc, missing in shared_missing.items():
        obs = shared_obsolete[loc]
        status = "OK" if len(missing) == 0 else f"MISSING {len(missing)}"
        if len(obs) > 0:
            status += f" (+{len(obs)} obs)"
        if len(missing) > 0:
            shared_has_issues = True
        print(f"{loc:<22} | {len(missing):<14} | {len(obs):<14} | {status}")

    print("\n" + "=" * 75)
    if app_has_issues or shared_has_issues:
        print("❌ Localization is INCOMPLETE. Run with --export-missing to extract missing keys.")
        return 1
    else:
        print("✅ All locales have 100% key coverage!")
        return 0


def export_missing_keys(output_file: Path):
    """Export missing keys and base English content to a JSON file."""
    app_base = parse_strings_xml(APP_RES_DIR / "values" / "strings.xml")
    shared_base = parse_strings_xml(SHARED_RES_DIR / "values" / "strings.xml")

    export_data = {
        "app": {},
        "sharedUi": {}
    }

    # App module
    for loc in ALL_APP_LOCALES:
        loc_entries = parse_strings_xml(APP_RES_DIR / loc / "strings.xml")
        missing_keys = set(app_base.keys()) - set(loc_entries.keys())
        if missing_keys:
            export_data["app"][loc] = {
                k: app_base[k] for k in sorted(missing_keys)
            }

    # Shared module
    for loc in ALL_SHARED_LOCALES:
        loc_entries = parse_strings_xml(SHARED_RES_DIR / loc / "strings.xml")
        missing_keys = set(shared_base.keys()) - set(loc_entries.keys())
        if missing_keys:
            export_data["sharedUi"][loc] = {
                k: shared_base[k] for k in sorted(missing_keys)
            }

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(export_data, f, indent=2, ensure_ascii=False)
    print(f"Exported missing keys to {output_file}")


def append_entry_to_xml(file_path: Path, name: str, entry_data: dict, translated_text_or_items):
    """Insert or append an entry before </resources> in a strings.xml file."""
    if not file_path.exists():
        # Create minimal XML
        file_path.parent.mkdir(parents=True, exist_ok=True)
        file_path.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n', encoding="utf-8")

    content = file_path.read_text(encoding="utf-8")
    closing_tag_index = content.rfind("</resources>")
    if closing_tag_index == -1:
        raise ValueError(f"No </resources> tag found in {file_path}")

    tag_type = entry_data.get("tag", "string")
    attrib = entry_data.get("attrib", {})
    attrib_str = "".join(f' {k}="{v}"' for k, v in attrib.items() if k != "name")

    lines_to_add = []
    if tag_type == "string":
        escaped = escape_android_xml(str(translated_text_or_items))
        lines_to_add.append(f'    <string name="{name}"{attrib_str}>{escaped}</string>\n')
    elif tag_type == "string-array":
        lines_to_add.append(f'    <string-array name="{name}"{attrib_str}>\n')
        items = translated_text_or_items if isinstance(translated_text_or_items, list) else [translated_text_or_items]
        for it in items:
            escaped = escape_android_xml(str(it))
            lines_to_add.append(f'        <item>{escaped}</item>\n')
        lines_to_add.append('    </string-array>\n')
    elif tag_type == "plurals":
        lines_to_add.append(f'    <plurals name="{name}"{attrib_str}>\n')
        items_dict = translated_text_or_items if isinstance(translated_text_or_items, dict) else {"other": str(translated_text_or_items)}
        for q, val in items_dict.items():
            escaped = escape_android_xml(str(val))
            lines_to_add.append(f'        <item quantity="{q}">{escaped}</item>\n')
        lines_to_add.append('    </plurals>\n')

    insertion = "".join(lines_to_add)
    new_content = content[:closing_tag_index] + insertion + content[closing_tag_index:]
    file_path.write_text(new_content, encoding="utf-8")


def import_translations(input_file: Path):
    """Import translations from JSON file and append them to appropriate XML files."""
    with open(input_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    app_base = parse_strings_xml(APP_RES_DIR / "values" / "strings.xml")
    shared_base = parse_strings_xml(SHARED_RES_DIR / "values" / "strings.xml")

    # Import app translations
    app_imports = data.get("app", {})
    for loc, keys in app_imports.items():
        loc_file = APP_RES_DIR / loc / "strings.xml"
        existing = parse_strings_xml(loc_file)
        added_count = 0
        for key_name, val in keys.items():
            if key_name in existing:
                continue
            meta = app_base.get(key_name, {"tag": "string", "attrib": {}})
            append_entry_to_xml(loc_file, key_name, meta, val)
            added_count += 1
        if added_count > 0:
            print(f"[app] {loc}: added {added_count} translations")

    # Import sharedUi translations
    shared_imports = data.get("sharedUi", {})
    for loc, keys in shared_imports.items():
        loc_file = SHARED_RES_DIR / loc / "strings.xml"
        existing = parse_strings_xml(loc_file)
        added_count = 0
        for key_name, val in keys.items():
            if key_name in existing:
                continue
            meta = shared_base.get(key_name, {"tag": "string", "attrib": {}})
            append_entry_to_xml(loc_file, key_name, meta, val)
            added_count += 1
        if added_count > 0:
            print(f"[sharedUi] {loc}: added {added_count} translations")

    print("\nImport complete. Run --check to verify.")


def main():
    parser = argparse.ArgumentParser(description="ChordPlay Localization Sync & Audit Tool")
    parser.add_argument("--check", action="store_true", help="Audit all locales and report missing keys")
    parser.add_argument("--export-missing", type=Path, help="Export missing keys to specified JSON file")
    parser.add_argument("--import-translations", type=Path, help="Import translated strings from JSON file")

    args = parser.parse_args()

    if args.export_missing:
        export_missing_keys(args.export_missing)
    elif args.import_translations:
        import_translations(args.import_translations)
    else:
        # Default action is audit
        code = print_audit_report()
        sys.exit(code)


if __name__ == "__main__":
    main()

