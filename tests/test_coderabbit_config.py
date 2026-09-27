"""Acceptance contracts for the repository's CodeRabbit configuration.

Requires Python 3.9+ and PyYAML. From the repository root, run:
    python3 -m unittest discover -s tests -p 'test_coderabbit_config.py' -v

These tests read the real configuration. Glob checks use disposable file trees
and Python's recursive glob for the literal, *, and ** patterns used here;
they do not exercise the hosted CodeRabbit service or evaluate review prose.
"""

import glob
from pathlib import Path
import tempfile
import unittest

import yaml


CONFIG_PATH = Path(__file__).resolve().parents[1] / ".coderabbit.yaml"
KOTLIN = "app/src/main/java/**/*.kt"
PRESENTATION = "app/src/main/java/**/presentation/**/*.kt"
SCREEN = "app/src/main/java/**/screen/**/*.kt"
VIEW_MODEL = "app/src/main/java/**/*ViewModel.kt"
NAVIGATION = "app/src/main/java/**/navigation/**/*.kt"
DATA = "app/src/main/java/**/data/**/*.kt"
DOMAIN = "app/src/main/java/**/domain/**/*.kt"
UNIT_TEST = "app/src/test/**/*.kt"
ANDROID_TEST = "app/src/androidTest/**/*.kt"
RESOURCES = "app/src/main/res/**"
MANIFEST = "app/src/main/AndroidManifest.xml"
GRADLE = "**/build.gradle.kts"
VERSIONS = "gradle/libs.versions.toml"
WORKFLOWS = ".github/workflows/**"


class UniqueKeySafeLoader(yaml.SafeLoader):
    """Reject silently overwritten settings, including nested review settings."""

    def construct_mapping(self, node, deep=False):
        keys = set()
        for key_node, _ in node.value:
            key = self.construct_object(key_node, deep=deep)
            if key in keys:
                raise ValueError(f"Duplicate configuration key: {key!r}")
            keys.add(key)
        return super().construct_mapping(node, deep=deep)


class CodeRabbitConfigTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with CONFIG_PATH.open(encoding="utf-8") as config_file:
            cls.config = yaml.load(config_file, Loader=UniqueKeySafeLoader)
        if not isinstance(cls.config, dict):
            raise AssertionError("The configuration must be a YAML mapping")

    def assert_boolean_setting(self, mapping, key, expected):
        self.assertIn(key, mapping)
        self.assertIs(mapping[key], expected, f"{key} must be a YAML boolean")

    def test_review_language_and_profile(self):
        self.assertEqual("pt-BR", self.config["language"])
        self.assertEqual("assertive", self.config["reviews"]["profile"])

    def test_review_feedback_is_visible_without_poems(self):
        reviews = self.config["reviews"]
        for key, expected in {
            "high_level_summary": True,
            "review_status": True,
            "poem": False,
            "collapse_walkthrough": False,
        }.items():
            with self.subTest(setting=key):
                self.assert_boolean_setting(reviews, key, expected)

    def test_automatic_reviews_are_enabled_but_drafts_are_excluded(self):
        auto_review = self.config["reviews"]["auto_review"]
        self.assert_boolean_setting(auto_review, "enabled", True)
        self.assert_boolean_setting(auto_review, "drafts", False)

    def test_chat_replies_are_enabled(self):
        self.assert_boolean_setting(self.config["chat"], "auto_reply", True)

    def test_each_review_scope_has_unique_nonempty_instructions(self):
        rules = self.config["reviews"]["path_instructions"]
        self.assertIsInstance(rules, list)
        self.assertTrue(rules, "Path instructions must not be empty")
        paths = []
        for index, rule in enumerate(rules):
            with self.subTest(rule=index):
                self.assertIsInstance(rule, dict)
                self.assertEqual({"path", "instructions"}, set(rule))
                self.assertIsInstance(rule["path"], str)
                self.assertTrue(rule["path"].strip())
                self.assertIsInstance(rule["instructions"], str)
                self.assertTrue(rule["instructions"].strip())
                paths.append(rule["path"])
        self.assertEqual(len(paths), len(set(paths)), "Duplicate review scopes")
        self.assertEqual(
            {
                KOTLIN, PRESENTATION, SCREEN, VIEW_MODEL, NAVIGATION, DATA,
                DOMAIN, UNIT_TEST, ANDROID_TEST, RESOURCES, MANIFEST, GRADLE,
                VERSIONS, WORKFLOWS,
            },
            set(paths),
        )

    def assert_scopes(self, cases):
        rules = self.config["reviews"]["path_instructions"]
        with tempfile.TemporaryDirectory(prefix="coderabbit-config-test-") as root:
            for relative_path in cases:
                fixture = Path(root, relative_path)
                fixture.parent.mkdir(parents=True, exist_ok=True)
                fixture.touch()

            matches = {
                rule["path"]: set(
                    glob.glob(str(Path(root, rule["path"])), recursive=True)
                )
                for rule in rules
            }
            for relative_path, expected in cases.items():
                with self.subTest(path=relative_path):
                    actual = {
                        pattern for pattern, files in matches.items()
                        if str(Path(root, relative_path)) in files
                    }
                    self.assertEqual(expected, actual)

    def test_paths_receive_all_applicable_review_instructions(self):
        # Direct children exercise ** matching zero directories; nested paths
        # exercise recursion and overlapping general/specialized instructions.
        self.assert_scopes({
            "app/src/main/java/MainActivity.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/MainActivity.kt": {KOTLIN},
            "app/src/main/java/presentation/Theme.kt": {KOTLIN, PRESENTATION},
            "app/src/main/java/com/olius/app/presentation/screen/home/HomeScreen.kt": {
                KOTLIN, PRESENTATION, SCREEN,
            },
            "app/src/main/java/screen/Login.kt": {KOTLIN, SCREEN},
            "app/src/main/java/LoginViewModel.kt": {KOTLIN, VIEW_MODEL},
            "app/src/main/java/com/olius/app/presentation/screen/home/HomeViewModel.kt": {
                KOTLIN, PRESENTATION, SCREEN, VIEW_MODEL,
            },
            "app/src/main/java/navigation/Routes.kt": {KOTLIN, NAVIGATION},
            "app/src/main/java/com/olius/app/navigation/auth/Routes.kt": {
                KOTLIN, NAVIGATION,
            },
            "app/src/main/java/data/Repository.kt": {KOTLIN, DATA},
            "app/src/main/java/com/olius/app/data/repository/UserRepositoryImpl.kt": {
                KOTLIN, DATA,
            },
            "app/src/main/java/domain/User.kt": {KOTLIN, DOMAIN},
            "app/src/main/java/com/olius/app/domain/usecases/LoginUseCase.kt": {
                KOTLIN, DOMAIN,
            },
            "app/src/test/ExampleTest.kt": {UNIT_TEST},
            "app/src/test/java/com/olius/app/presentation/HomeViewModelTest.kt": {
                UNIT_TEST,
            },
            "app/src/androidTest/ExampleTest.kt": {ANDROID_TEST},
            "app/src/androidTest/java/com/olius/app/navigation/NavigationTest.kt": {
                ANDROID_TEST,
            },
            "app/src/main/res/icon.png": {RESOURCES},
            "app/src/main/res/values-pt-rBR/strings.xml": {RESOURCES},
            "app/src/main/res/drawable/logo.xml": {RESOURCES},
            "app/src/main/AndroidManifest.xml": {MANIFEST},
            "build.gradle.kts": {GRADLE},
            "app/build.gradle.kts": {GRADLE},
            "features/login/build.gradle.kts": {GRADLE},
            "gradle/libs.versions.toml": {VERSIONS},
            ".github/workflows/ci.yml": {WORKFLOWS},
            ".github/workflows/release.yaml": {WORKFLOWS},
        })

    def test_similar_names_and_unrelated_files_do_not_receive_wrong_rules(self):
        self.assert_scopes({
            "app/src/main/java/com/olius/app/presentations/Theme.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/screens/Home.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/database/Entity.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/domains/User.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/navigations/Routes.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/HomeViewModelTest.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/HomeViewmodel.kt": {KOTLIN},
            "app/src/main/java/com/olius/app/HomeViewModel.java": set(),
            "app/src/main/java/com/olius/app/HomeViewModel.kt.bak": set(),
            "app/src/testFixtures/java/Fixture.kt": set(),
            "app/src/androidTestBackup/java/ExampleTest.kt": set(),
            "app/src/main/resources/strings.xml": set(),
            "app/src/debug/AndroidManifest.xml": set(),
            "app/src/main/AndroidManifest.xml.bak": set(),
            "app/build.gradle.kts.bak": set(),
            "app/settings.gradle.kts": set(),
            "gradle/libs.versions.toml.bak": set(),
            "other/gradle/libs.versions.toml": set(),
            ".github/workflow/ci.yml": set(),
            ".github/actions/setup/action.yml": set(),
            "README.md": set(),
            ".coderabbit.yaml": set(),
        })


if __name__ == "__main__":
    unittest.main()
