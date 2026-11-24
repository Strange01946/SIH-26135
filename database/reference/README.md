# India geography directory

The project uses the Ministry of Panchayati Raj’s Local Government Directory (LGD) for selectable states, union territories and districts. The bundled snapshot from 23 November 2026 contains 28 states, eight union territories and 790 districts. Its source URLs, observation dates and SHA-256 checksums are stored in `india-lgd.json`. The data are distributed under the Government Open Data License – India. [Government of India LGD catalog](https://data.gov.in/catalog/local-government-directory-lgd) · [Archived LGD reports](https://ramseraph.github.io/opendata/lgd/)

## Install or verify

From the project root, with MySQL on loopback and the existing project schema already created, run the following.

```powershell
# Uses SIH_TEST_DB_USERNAME and SIH_TEST_DB_PASSWORD.
node database/install-india-geography.mjs --catalog SIH26135_TEST
node database/install-india-geography.mjs --catalog SIH26135_TEST --verify
```

For the regular local `SIH26135` catalog, pass `--catalog SIH26135` and set `DB_USERNAME` and `DB_PASSWORD`. This is reference-data installation. Use the test catalog for browser tests.

The installer validates the connected catalog and the full state–district hierarchy in one transaction. It uses LGD codes to reconcile existing records, preserves internal IDs and existing assignments, and inserts missing reference rows. It reports conflicts for review and can be rerun. The application reads the installed data directly from MySQL during normal local use.

## Refresh the bundled edition

Install `pyarrow` in a Python environment and run `python database/reference/build-lgd-snapshot.py`. The generator verifies the source checksums and coverage. Review the snapshot changes, apply to the test catalog first, and rerun the geography tests.

```powershell
Set-Location frontend
npm run test:e2e -- --grep 'nationwide directory'
```
