"""Render only the backend Deployment from kubectl's client-side JSON output."""
import argparse
import copy
import json
import re
import sys


def parse_resources(text):
    # kubectl create -f multi-document.yaml -o json emits adjacent JSON objects,
    # while other kubectl inputs produce a single List. Accept both, strictly.
    items = []
    decoder = json.JSONDecoder()
    while text.strip():
        text = text.lstrip()
        document, end = decoder.raw_decode(text)
        if not isinstance(document, dict):
            raise ValueError("Expected Kubernetes resource objects")
        items.extend(document.get("items", []) if document.get("kind") == "List" else [document])
        text = text[end:]
    return {"kind": "List", "items": items}


def render(document, tag, allow_degraded):
    if not re.fullmatch(r"sha-[0-9a-f]{7,40}", tag):
        raise ValueError("Expected an immutable sha image tag")
    if allow_degraded not in ("true", "false"):
        raise ValueError("allow-degraded must be true or false")
    items = document.get("items", []) if document.get("kind") == "List" else [document]
    matches = [item for item in items if item.get("kind") == "Deployment"
               and item.get("metadata", {}).get("name") == "app"
               and item.get("metadata", {}).get("namespace") == "ovlo"]
    if len(matches) != 1:
        raise ValueError("Expected exactly one ovlo/app Deployment")
    deployment = copy.deepcopy(matches[0])
    containers = deployment["spec"]["template"]["spec"]["containers"]
    matches = [container for container in containers if container["name"] == "app"]
    if len(matches) != 1:
        raise ValueError("Expected exactly one app container")
    container = matches[0]
    container["image"] = "ghcr.io/yeonjae1220/ovlo:" + tag
    flags = [entry for entry in container.get("env", [])
             if entry["name"] == "OVLO_OFFICE_ALLOW_DEGRADED"]
    if len(flags) != 1 or "valueFrom" in flags[0]:
        raise ValueError("Expected one explicit degraded-mode slot in the manifest")
    flags[0]["value"] = allow_degraded
    return deployment


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", required=True)
    parser.add_argument("--allow-degraded", choices=("true", "false"), required=True)
    args = parser.parse_args()
    try:
        result = render(parse_resources(sys.stdin.read()), args.tag, args.allow_degraded)
    except (ValueError, KeyError, TypeError) as error:
        parser.exit(1, f"Deployment rendering failed: {error}\n")
    json.dump(result, sys.stdout, indent=2)
    sys.stdout.write("\n")
