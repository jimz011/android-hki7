# Contributing

Issues and feedback are welcome at
[github.com/jimz011/android-hki7](https://github.com/jimz011/android-hki7).

## Building

Open the project in a recent Android Studio and run the `app` configuration, or build from the
command line:

```bash
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose, Material 3 |
| **minSdk / targetSdk / compileSdk** | 34 / 37 / 37 |
| **Application id** | `com.jimz011apps.hki7` |

## Project layout

```
app/src/main/java/com/jimz011apps/hki7/
├── data/          Home Assistant client, models, preferences, background work
├── ui/
│   ├── components/  Reusable dialogs, cards, editors
│   ├── screens/     Full screens and widgets
│   ├── theme/
│   └── utils/
├── MainActivity.kt
└── ui/MainViewModel.kt
app/src/main/res/values*/   String resources, one folder per locale
tools/localization/         Translation pipeline
```

Broadly: `data/` talks to Home Assistant and to disk, `MainViewModel` holds app state, and
`ui/` draws it. Dashboard items are `HKIRoomWidget` subclasses in
`data/HomeAssistantModels.kt`, serialised polymorphically — an unrecognised `type` decodes to
`HKIUnknownWidget` and is skipped rather than failing the whole dashboard, which is what lets a
family dashboard survive version skew.

## Strings and localisation

**New user-facing strings ship in `values/` (English) only.** Do not hand-translate — the other
locales are generated.

The scripts live in `tools/localization/`:

| Script | Does |
|---|---|
| `extract_ui_strings.py` | Pulls direct Compose string literals out into resources |
| `audit_literals.py` | Lists likely user-facing literals that are not resources yet |
| `generate_translations.py` | Generates complete locale resources from the English source |
| `insert_missing_translations.py` | Fills the locales the main generator skips: `pt`, `pt-rBR`, `b+es+419`, `zh-rCN`, `zh-rTW` |
| `generate_plurals.py` | Fills `<plurals>` for `ja` and `ko`, which need only the CLDR `other` category |
| `insert_whats_new_array.py` | Translates one `cr_whats_new_*` release-notes array into every locale |
| `verify_translations.py` | **The one that decides whether a locale is complete** |

Typical flow after adding English strings:

```bash
python tools/localization/generate_translations.py
python tools/localization/insert_missing_translations.py
python tools/localization/verify_translations.py
```

!!! warning "verify_translations.py does not check string-arrays"

    It covers `<string>` and `<plurals>` only. The `cr_whats_new_*` release-notes arrays are
    string-arrays, so they are not covered and are easy to forget — that is what
    `insert_whats_new_array.py` is for.

!!! warning "Apostrophes"

    An unescaped apostrophe in a translated string breaks `mergeDebugResources`. Scan for them
    after any translation pass; the error points at the resource merge, not at the string.

## Release notes

Every user-facing change goes in **two** places:

1. `CHANGELOG.md` — the canonical list, newest first.
2. The `cr_whats_new_*` string arrays — what the in-app "What's new" dialog shows. Keep them in
   sync with `ui/components/WhatsNewDialog.kt`.

Items that require the HKI 7 Cloud component are marked with `\*` in the changelog.

## Documentation

This site is [MkDocs](https://www.mkdocs.org/) with
[Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Sources are in `docs/`,
configuration in `mkdocs.yml`.

Serve it locally:

```bash
pip install -r docs/requirements.txt
```

```bash
mkdocs serve
```

Then open <http://127.0.0.1:8000>. Edits reload live.

Build a static copy into `site/`:

```bash
mkdocs build --strict
```

`--strict` turns warnings — a broken internal link, a missing file — into errors. The CI workflow
builds with it, so run it before pushing.

`docs/reference/privacy.md` and `docs/reference/changelog.md` are thin wrappers that include
`PRIVACY_POLICY.md` and `CHANGELOG.md` from the repository root, so those two documents have a
single source of truth. Edit the root files, not the wrappers.

Pushing to `main` deploys the site to GitHub Pages automatically — see
`.github/workflows/docs.yml`.

## Licensing your contribution

Community source code in this repository is [MPL-2.0](https://www.mozilla.org/MPL/2.0/).
Contributions are accepted under the same licence. Premium materials — premium icon packs,
animated icon and artwork collections, premium themes, entitlement and storefront services — are
proprietary and are not part of the community core.

See [LICENSE](https://github.com/jimz011/android-hki7/blob/main/LICENSE) for the full notice.
