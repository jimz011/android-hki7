# Translating HKI 7

HKI 7 speaks 36 languages. Most of them were machine-translated to get them off the ground, which
means they are complete but not always *right* — a native speaker usually spots something stilted,
too literal, or plainly wrong within a minute of opening the app.

This folder exists so you can fix that without touching Android code. Every language is one file.

| I want to… | Do this |
| --- | --- |
| Fix a few words in my language | Edit `<language>.csv`, open a pull request |
| Review my whole language | Open `<language>.csv` on GitHub — it renders as a searchable table |
| Add a language that isn't here | Copy `_new-language-template.csv`, fill in the `translation` column |
| Just report something wrong | [Open a translation issue](https://github.com/jimz011/android-hki7/issues/new?template=translation.yml) — no files, no git |

## The files

One CSV per language, named by its Android locale qualifier:

`ar` Arabic · `bg` Bulgarian · `b+es+419` Spanish (Latin America) · `cs` Czech · `da` Danish ·
`de` German · `de-rAT` German (Austria) · `de-rCH` German (Switzerland) · `el` Greek ·
`es` Spanish · `es-rMX` Spanish (Mexico) · `et` Estonian · `fi` Finnish · `fr` French ·
`hr` Croatian · `hu` Hungarian · `it` Italian · `iw` Hebrew · `ja` Japanese · `ko` Korean ·
`lt` Lithuanian · `lv` Latvian · `nb` Norwegian · `nl` Dutch · `pl` Polish · `pt` Portuguese ·
`pt-rBR` Portuguese (Brazil) · `ro` Romanian · `ru` Russian · `sk` Slovak · `sv` Swedish ·
`th` Thai · `tr` Turkish · `zh-rCN` Chinese (Simplified) · `zh-rTW` Chinese (Traditional)

Each has about 3,800 rows and six columns:

| Column | What it is |
| --- | --- |
| `key` | The internal name. **Never change this** — it is how the text finds its way back into the app. |
| `type` | `string`, `plural`, or `array`. |
| `item` | Empty for a plain string; a plural category (`one`, `other`, …) or a position number otherwise. |
| `english` | The original. Also **do not change** — it is what the tooling checks your row against. |
| `translation` | **This is the column you edit.** |
| `notes` | Anything the row requires of you, generated automatically. |

Only `translation` is yours. The other five let the importer put your work in the right place and
tell you if something drifted.

## Editing

Open the file in a spreadsheet (Excel, LibreOffice, Numbers, Google Sheets) or a text editor —
whatever you prefer. A few rules the tooling enforces:

**Keep the format arguments.** `%1$s`, `%2$d`, `%.1f` and friends are replaced with real values
when the app runs. They may move to wherever your language needs them, but every one has to
survive. `%%` is a literal percent sign. Getting this wrong is how a translation crashes the app,
so the importer refuses a file that loses one.

**Keep product names in English.** Home Assistant, HKI 7, HKI 7 Cloud, Google Drive, Nabu Casa and
Valetudo stay as they are. The `notes` column flags the rows where this applies.

**`\n` means a line break** and `\u0020` means a space. Leave them as written.

**Leading and trailing spaces matter** on some rows, because the text gets joined with whatever sits
next to it — `" is"` and `"%1$s · "` are not accidents. Spreadsheets like to trim these; the notes
column marks the rows where the space is load-bearing.

**Apostrophes and quotes need no special treatment.** Write `l'éclairage`, not `l\'éclairage`. The
importer escapes them for Android.

**Leave a row blank if you're unsure.** An empty `translation` is skipped, never used to erase what
is already there. A partly finished file is a perfectly good pull request.

**Plurals** get one row per category. If your language needs categories English doesn't have —
Arabic uses all six of `zero`, `one`, `two`, `few`, `many`, `other` — add rows for them, copying the
`key` and setting `item` to the category name.

## Sending it back

Open a pull request with just the changed CSV. A maintainer runs:

```bash
python tools/localization/import_translations.py translations/nl.csv
```

which merges it into the app's resource files, validates every row, and refuses the whole file if
anything is wrong rather than half-applying it.

If git isn't your thing, the
[translation issue form](https://github.com/jimz011/android-hki7/issues/new?template=translation.yml)
takes a string and a correction, or an attached CSV.

## Adding a new language

1. Copy `_new-language-template.csv` to `<qualifier>.csv`, using the
   [Android locale qualifier](https://developer.android.com/guide/topics/resources/providing-resources#LocaleQualifier)
   for your language — `values-fa` → `fa.csv`, `values-es-rAR` → `es-rAR.csv`.
2. Fill in the `translation` column. You do not have to finish it in one go; a partial file is
   still useful, and the rows you leave empty keep showing English until someone fills them.
3. Open a pull request. The qualifier has to be registered in the app before the import will
   accept it, so say in the PR which language it is and a maintainer will wire it up.

Right-to-left languages need no extra work in the CSV — the app already lays itself out
right-to-left for Arabic and Hebrew, and a new RTL language inherits that.

## Where these files come from

They are generated from the app's real resources, so they are never out of date:

```bash
python tools/localization/export_translations.py
```

CI regenerates them on every pull request and fails if the checked-in files differ, which means the
English you are translating against is always the English currently in the app.
