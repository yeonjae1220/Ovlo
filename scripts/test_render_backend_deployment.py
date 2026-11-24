import copy
import json
import unittest
from render_backend_deployment import parse_resources, render


class DeploymentRenderingTest(unittest.TestCase):
    def setUp(self):
        self.deployment = {"kind": "Deployment", "metadata": {"name": "app", "namespace": "ovlo"},
                           "spec": {"template": {"spec": {"containers": [{
                               "name": "app", "image": "ghcr.io/yeonjae1220/ovlo:latest",
                               "readinessProbe": {"httpGet": {"path": "/actuator/health/readiness", "port": 8080}},
                               "lifecycle": {"preStop": {"exec": {"command": ["sleep", "15"]}}},
                               "env": [{"name": "JWT_SECRET", "valueFrom": {"secretKeyRef": {
                                   "name": "ovlo-secret", "key": "JWT_SECRET"}}},
                                   {"name": "OVLO_OFFICE_ALLOW_DEGRADED", "value": "false"}]
                           }]}}}}

    def test_keeps_manifest_probes_hooks_and_secret_references_without_applying_storage(self):
        original = copy.deepcopy(self.deployment)
        rendered = render({"kind": "List", "items": [
            {"kind": "PersistentVolumeClaim"}, self.deployment, {"kind": "Service"}
        ]}, "sha-123abcd", "true")
        self.assertEqual(rendered["kind"], "Deployment")
        container = rendered["spec"]["template"]["spec"]["containers"][0]
        expected = original["spec"]["template"]["spec"]["containers"][0]
        self.assertEqual(container["image"], "ghcr.io/yeonjae1220/ovlo:sha-123abcd")
        self.assertEqual(container["readinessProbe"], expected["readinessProbe"])
        self.assertEqual(container["lifecycle"], expected["lifecycle"])
        self.assertEqual(container["env"][0], expected["env"][0])
        self.assertEqual(container["env"][1]["value"], "true")
        self.assertEqual(self.deployment, original)

    def test_rejects_missing_or_duplicate_target_and_mutable_image(self):
        for document, tag, mode in [
            ({"kind": "Service"}, "sha-123abcd", "false"),
            ({"kind": "List", "items": [self.deployment, self.deployment]}, "sha-123abcd", "false"),
            (self.deployment, "latest", "false"), (self.deployment, "sha-123abcd", "")
        ]:
            with self.subTest(tag=tag, mode=mode), self.assertRaises(ValueError):
                render(document, tag, mode)

    def test_rejects_missing_flag_instead_of_silently_losing_operator_setting(self):
        self.deployment["spec"]["template"]["spec"]["containers"][0]["env"].pop()
        with self.assertRaises(ValueError):
            render(self.deployment, "sha-123abcd", "true")

    def test_accepts_actual_multi_document_kubectl_output_and_rejects_trailing_garbage(self):
        stream = '\n'.join(json.dumps(item) for item in [
            {"kind": "PersistentVolumeClaim"}, self.deployment, {"kind": "Service"}
        ])
        rendered = render(parse_resources(stream), "sha-123abcd", "false")
        self.assertEqual(rendered["metadata"]["name"], "app")
        with self.assertRaises(ValueError):
            parse_resources(stream + '\nnot-json')
