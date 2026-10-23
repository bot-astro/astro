# API recipes

Short multi-step flows that recur in Crowdin integrations, written against `@crowdin/crowdin-api-client`. Each recipe names the problem, the Crowdin objects involved, the exact module and method chain, and a compact typed snippet. The snippets assume the `client` from the [main skill's bootstrap](../SKILL.md#bootstrap) and the three helpers in the first section. They type-check against the client's typings, and the preflight rule still applies: confirm every method in the local `index.d.ts` before shipping, because methods get added and renamed between releases.

- [Helpers: wait, save, chunk](#helpers-wait-save-chunk)
- [Upload or update a source file](#upload-or-update-a-source-file)
- [Build and download translations](#build-and-download-translations)
- [Pre-translate, then export](#pre-translate-then-export)
- [Filter strings, label them, open a task](#filter-strings-label-them-open-a-task)
- [Batch-edit strings without losing translations](#batch-edit-strings-without-losing-translations)
- [Upload translations](#upload-translations)
- [Progress across all projects](#progress-across-all-projects)
- [TM and glossary import and export](#tm-and-glossary-import-and-export)
- [QA issues per language](#qa-issues-per-language)
- [Screenshots and tags](#screenshots-and-tags)
- [Over-the-air delivery: distributions and bundles](#over-the-air-delivery-distributions-and-bundles)
- [Generate and download a report](#generate-and-download-a-report)

## What every recipe relies on

**Long-running operations share one shape.** Builds, pre-translation, TM and glossary import and export, bundle exports, report generation, branch merges and AI datasets all start with a POST that returns a status object, and each has a matching `check...Status` method to poll. The terminal statuses are `finished`, `failed` and `canceled`. The in-progress spelling differs (`in_progress` for most, `inProgress` for builds and distribution releases) and a distribution release ends in `success` instead of `finished`, so test only for terminal statuses and never loop on "not finished". Poll with a delay of a second or two; a tight loop burns the rate limit and gains nothing.

**Download links expire.** `downloadTranslations`, `downloadTm`, `downloadGlossary`, `downloadReport`, `downloadBundle` and the file build methods return `{ url, expireIn }`. Fetch the URL right away; the client never downloads the bytes for you.

**Storage is a one-shot handoff.** `uploadStorageApi.addStorage(fileName, content, contentType?)` stores the bytes; the storage id is then consumed by exactly one `createFile`, `updateOrRestoreFile`, `uploadTranslation`, `importTm`, `importGlossaryFile` or `addScreenshot` call.

**File-based and string-based projects differ at the edges.** String-based projects have branches but no files: pre-translation takes `branchIds` (`PreTranslateStringsRequest`), translations upload through `uploadTranslationStrings`, and strings are created with `CreateStringStringsBasedRequest`. Everything else below is identical.

**Crowdin Enterprise** needs the `organization` credential and, for tasks, a `workflowStepId` alongside `type`. CroQL expressions are a separate skill (`croql`); a `croql` option cannot be combined with `labelIds`, `filter` or `scope` in the same list request.

## Helpers: wait, save, chunk

```ts
import { writeFile } from 'node:fs/promises';

interface Trackable { status: string; progress: number }

/** Polls `check` until the operation reaches a terminal status; throws on failure or timeout. */
export async function waitFor<T extends Trackable>(
  check: () => Promise<{ data: T }>,
  { done = ['finished'], failed = ['failed', 'canceled'], intervalMs = 2_000, timeoutMs = 30 * 60_000 } = {},
): Promise<T> {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    const { data } = await check();
    if (done.includes(data.status)) return data;
    if (failed.includes(data.status)) throw new Error(`Crowdin operation ${data.status}`);
    if (Date.now() >= deadline) throw new Error('Timed out waiting for Crowdin operation');
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }
}

/** Download links expire; fetch them right away. */
export async function saveUrl(url: string, path: string): Promise<void> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Download failed: HTTP ${response.status}`);
  await writeFile(path, Buffer.from(await response.arrayBuffer()));
}

export function chunk<T>(items: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) out.push(items.slice(i, i + size));
  return out;
}
```

`waitFor` accepts anything with `status` and `progress`: the generic `Status<T>` envelope, a `TranslationsModel.Build`, or a distribution release. Pass `{ done: ['success'] }` for releases.

## Upload or update a source file

**Problem:** a sync script must create a file on the first run and replace it afterwards, keeping the translations already made.

**Modules:** `sourceFilesApi.listProjectFiles` → `uploadStorageApi.addStorage` → `sourceFilesApi.createFile` or `sourceFilesApi.updateOrRestoreFile`.

```ts
const { uploadStorageApi, sourceFilesApi } = client;

/** Creates the file when nothing lives at `crowdinPath` yet, otherwise replaces its content and keeps translations. */
export async function upsertSourceFile(
  projectId: number,
  crowdinPath: string, // e.g. '/locales/en.json'; Crowdin paths start with '/'
  localPath: string,
  directoryId?: number,
): Promise<number> {
  const name = basename(crowdinPath);
  const files = await sourceFilesApi.withFetchAll().listProjectFiles(projectId, { directoryId, filter: name });
  const existing = files.data.map((f) => f.data).find((f) => f.path === crowdinPath);

  const storage = await uploadStorageApi.addStorage(name, await readFile(localPath));
  if (existing) {
    await sourceFilesApi.updateOrRestoreFile(projectId, existing.id, {
      storageId: storage.data.id,
      updateOption: 'keep_translations_and_approvals',
    });
    return existing.id;
  }
  const created = await sourceFilesApi.createFile(projectId, { storageId: storage.data.id, name, directoryId });
  return created.data.id;
}
```

- `name` is the file name only; the directory comes from `directoryId` (create it with `createDirectory` when missing) and the branch from `branchId`.
- `updateOption` defaults to clearing translations for changed strings; `keep_translations` keeps them but drops approvals, `keep_translations_and_approvals` keeps both.
- Listing is per directory unless the `recursion` option is set; a file in a subdirectory is not found from the root listing.

## Build and download translations

**Problem:** get translated files out of Crowdin, for the whole project, one file, or a filtered slice.

**Modules:** `translationsApi.buildProject` → `checkBuildStatus` → `downloadTranslations`; or `buildProjectFileTranslation`; or `exportProjectTranslation`.

```ts
const { translationsApi } = client;

/** Whole project (optionally some languages): build, wait, download the archive. */
export async function downloadProjectTranslations(projectId: number, target: string, languageIds?: string[]): Promise<void> {
  const build = await translationsApi.buildProject(projectId, { targetLanguageIds: languageIds, skipUntranslatedStrings: true });
  await waitFor(() => translationsApi.checkBuildStatus(projectId, build.data.id));
  const link = await translationsApi.downloadTranslations(projectId, build.data.id);
  await saveUrl(link.data.url, target);
}

/** One file in one language: synchronous, nothing to poll. */
export async function downloadFileTranslation(projectId: number, fileId: number, languageId: string, target: string): Promise<void> {
  const link = await translationsApi.buildProjectFileTranslation(projectId, fileId, { targetLanguageId: languageId });
  await saveUrl(link.data.url, target);
}

/** A slice of the project (files, directories, branches or labels) in one language, also synchronous. */
export async function exportLabeledStrings(projectId: number, languageId: string, labelIds: number[], target: string): Promise<void> {
  const link = await translationsApi.exportProjectTranslation(projectId, { targetLanguageId: languageId, labelIds, format: 'xliff' });
  await saveUrl(link.data.url, target);
}
```

- A project build is keyed by `build.data.id` (a number), unlike the other async operations, which use `identifier` (a string). A failed build carries `error.message`.
- `listProjectBuilds` shows builds already running or finished; reuse one whose attributes match instead of starting another on every call.
- `buildProjectFileTranslation` accepts the `etag` from a previous response; an unchanged file comes back without a new download, which makes per-file polling cheap.
- Choose the export shape by what stays out: `skipUntranslatedStrings`, `skipUntranslatedFiles`, `exportApprovedOnly`.

## Pre-translate, then export

**Problem:** fill untranslated strings from the TM, an MT engine or an AI prompt, then hand the result on.

**Modules:** `translationsApi.applyPreTranslation` → `preTranslationStatus` → `getPreTranslationReport`, then any export from the previous recipe.

```ts
const { translationsApi } = client;

export async function preTranslate(
  projectId: number,
  fileIds: number[],
  languageIds: string[],
  request: Pick<TranslationsModel.PreTranslateRequest, 'method' | 'engineId' | 'aiPromptId' | 'autoApproveOption'> = { method: 'tm' },
): Promise<TranslationsModel.PreTranslationReport> {
  // method 'mt' needs engineId, method 'ai' needs aiPromptId; string-based projects pass branchIds instead of fileIds
  const op = await translationsApi.applyPreTranslation(projectId, { languageIds, fileIds, ...request });
  await waitFor(() => translationsApi.preTranslationStatus(projectId, op.data.identifier));
  const report = await translationsApi.getPreTranslationReport(projectId, op.data.identifier);
  return report.data;
}
```

- The report lists, per language and file, how many strings were translated and how many were skipped and why, which is what to log or gate a pipeline on.
- `autoApproveOption` only makes sense for TM matches; `translateUntranslatedOnly` and `skipApprovedTranslations` protect existing work when re-running.

## Filter strings, label them, open a task

**Problem:** a subset of strings needs attention: strings with personal data, strings changed since a release, strings a stakeholder flagged. Find them, mark them so the set survives, and hand them to someone.

**Modules:** `sourceStringsApi.listProjectStrings({ croql })` → `labelsApi.listLabels` / `addLabel` → `labelsApi.assignLabelToString` → `tasksApi.addTask`.

```ts
const { sourceStringsApi, labelsApi, tasksApi } = client;

/** Filter strings with CroQL, put them under one label, open a proofreading task for that label. */
export async function labelAndAssign(projectId: number, croql: string, labelTitle: string, languageId: string): Promise<number> {
  const strings = (await sourceStringsApi.withFetchAll().listProjectStrings(projectId, { croql })).data.map((s) => s.data);
  if (strings.length === 0) throw new Error('The CroQL filter matched no strings');

  const labels = (await labelsApi.withFetchAll().listLabels(projectId)).data.map((l) => l.data);
  const label = labels.find((l) => l.title === labelTitle) ?? (await labelsApi.addLabel(projectId, { title: labelTitle })).data;

  for (const stringIds of chunk(strings.map((s) => s.id), 500)) {
    await labelsApi.assignLabelToString(projectId, label.id, { stringIds }); // 500 strings per call
  }

  const request: TasksModel.CreateTaskRequest = {
    type: TasksModel.Type.PROOFREAD,
    title: `Proofread: ${labelTitle}`,
    languageId,
    fileIds: [...new Set(strings.map((s) => s.fileId))], // a task always scopes to files, branches or string ids
    labelIds: [label.id], // ...and the label narrows it to the matched strings
  };
  const task = await tasksApi.addTask(projectId, request);
  return task.data.id;
}
```

- `assignLabelToString` is singular in the client's name and takes up to 500 string ids per call.
- A task needs `fileIds`, `branchIds` or `stringIds`; `labelIds` alone is not a valid scope. Passing `stringIds` directly skips the label but loses the reusable filter.
- Bulk labelling is a change many people can see; when acting on someone's behalf, report the count before assigning.

## Batch-edit strings without losing translations

**Problem:** fix context, text, visibility or max length on many strings at once, without resetting their translations.

**Modules:** `sourceStringsApi.stringBatchOperations` with a JSON Patch array; `editString` for a single string.

```ts
const { sourceStringsApi } = client;

/** Rewrites context (or text) on many strings without dropping their translations. */
export async function setStringContext(projectId: number, contextByStringId: Map<number, string>): Promise<void> {
  const patches: PatchRequest[] = [...contextByStringId].map(([id, context]) => ({
    op: 'replace',
    path: `/${id}/context`, // '/<stringId>/text', '/<stringId>/isHidden', '/<stringId>/maxLength' work the same way
    value: context,
  }));
  for (const batch of chunk(patches, 200)) {
    await sourceStringsApi.stringBatchOperations(projectId, batch, { updateOption: 'keep_translations_and_approvals' });
  }
}
```

- `updateOption` matters only when `text` or `identifier` changes; the default clears translations and approvals for the edited strings.
- The same endpoint adds strings (`op: 'add', path: '/-'`, value shaped like `CreateStringRequest`) and removes them (`op: 'remove', path: '/<stringId>'`).

## Upload translations

**Problem:** translations produced elsewhere (a vendor's XLIFF, a legacy export, a machine pass done locally) must land on the right file and language.

**Modules:** `uploadStorageApi.addStorage` → `translationsApi.uploadTranslation`.

```ts
const { uploadStorageApi, translationsApi } = client;

/** Pushes a translated file for one language; the file must match the source file's format. */
export async function uploadTranslationFile(projectId: number, fileId: number, languageId: string, localPath: string): Promise<void> {
  const storage = await uploadStorageApi.addStorage(basename(localPath), await readFile(localPath));
  await translationsApi.uploadTranslation(projectId, languageId, {
    storageId: storage.data.id,
    fileId,
    importEqSuggestions: false, // skip translations identical to the source
    autoApproveImported: false,
    addToTm: true,
  });
}
```

- The upload is synchronous and returns `{ projectId, storageId, languageId, fileId }`; there is no status to poll.
- String-based projects use `uploadTranslationStrings` with `branchId` instead of `fileId`.
- Single strings go through `stringTranslationsApi.addTranslation({ stringId, languageId, text })`, and approvals through `addApproval({ translationId })` or `approvalBatchOperations`.

## Progress across all projects

**Problem:** one table of every project and how complete it is, for a status report or a dashboard.

**Modules:** `projectsGroupsApi.listProjects` → `translationStatusApi.getProjectProgress` per project.

```ts
const { projectsGroupsApi, translationStatusApi } = client;

/** Every project the token can see, least complete first. Sequential on purpose: one progress call per project. */
export async function progressAcrossProjects(): Promise<Array<{ name: string; percent: number }>> {
  const projects = (await projectsGroupsApi.withFetchAll().listProjects()).data.map((p) => p.data);
  const rows: Array<{ name: string; percent: number }> = [];
  for (const project of projects) {
    const languages = (await translationStatusApi.withFetchAll().getProjectProgress(project.id)).data.map((l) => l.data);
    const percent = languages.length ? languages.reduce((sum, l) => sum + l.translationProgress, 0) / languages.length : 100;
    rows.push({ name: project.name, percent: Math.round(percent) });
  }
  return rows.sort((a, b) => a.percent - b.percent);
}
```

- Progress is per language; a project-level number is whatever aggregate fits the question (mean here, or weight by `phrases.total`). `approvalProgress` sits next to `translationProgress` when the question is about proofreading.
- `getLanguageProgress`, `getFileProgress`, `getDirectoryProgress` and `getBranchProgress` slice the same data the other way.

## TM and glossary import and export

**Problem:** seed a TM or glossary from a file, or pull one out for another tool.

**Modules:** `translationMemoryApi.importTm` → `checkImportStatus`; `exportTm` → `checkExportStatus` → `downloadTm`; `glossariesApi.importGlossaryFile` → `checkGlossaryImportStatus`; `exportGlossary` → `checkGlossaryExportStatus` → `downloadGlossary`.

```ts
const { uploadStorageApi, translationMemoryApi, glossariesApi } = client;

/** CSV/XLSX need a scheme mapping columns to languages, e.g. { en: 0, de: 1 }; TMX needs none. */
export async function importTm(tmId: number, localPath: string, scheme?: TranslationMemoryModel.Scheme): Promise<void> {
  const storage = await uploadStorageApi.addStorage(basename(localPath), await readFile(localPath));
  const op = await translationMemoryApi.importTm(tmId, { storageId: storage.data.id, scheme, firstLineContainsHeader: true });
  await waitFor(() => translationMemoryApi.checkImportStatus(tmId, op.data.identifier));
}

export async function exportTm(tmId: number, target: string): Promise<void> {
  const op = await translationMemoryApi.exportTm(tmId, { format: 'tmx' });
  await waitFor(() => translationMemoryApi.checkExportStatus(tmId, op.data.identifier));
  const link = await translationMemoryApi.downloadTm(tmId, op.data.identifier);
  await saveUrl(link.data.url, target);
}

/** Scheme keys follow the glossary CSV header convention, e.g. { term_en: 0, description_en: 1, term_de: 2 }. */
export async function importGlossary(glossaryId: number, localPath: string, scheme?: GlossariesModel.GlossaryFileScheme): Promise<void> {
  const storage = await uploadStorageApi.addStorage(basename(localPath), await readFile(localPath));
  const op = await glossariesApi.importGlossaryFile(glossaryId, { storageId: storage.data.id, scheme, firstLineContainsHeader: true });
  await waitFor(() => glossariesApi.checkGlossaryImportStatus(glossaryId, op.data.identifier));
}

export async function exportGlossary(glossaryId: number, target: string): Promise<void> {
  const op = await glossariesApi.exportGlossary(glossaryId, { format: 'csv' });
  await waitFor(() => glossariesApi.checkGlossaryExportStatus(glossaryId, op.data.identifier));
  const link = await glossariesApi.downloadGlossary(glossaryId, op.data.identifier);
  await saveUrl(link.data.url, target);
}
```

- A new TM or glossary comes from `addTm({ name, languageId })` or `addGlossary({ name, languageId })`; find an existing one with `listTm` or `listGlossaries` first so re-runs do not multiply them.
- A filtered slice of a TM (by date, author or usage) is a CroQL query over `listTmSegments`, not an export; the `croql` skill owns the expression.
- Glossary export accepts `exportFields`, `statuses` and a `text` search to narrow what comes out.

## QA issues per language

**Problem:** QA issues pile up and nobody can see which languages or checks are responsible.

**Modules:** `translationStatusApi.listQaCheckIssues`, filtered by `languageIds`, `category` or `validation`.

```ts
const { translationStatusApi } = client;

/** Counts open QA issues per language and category, e.g. 'de spellcheck' -> 42. */
export async function qaIssueCounts(projectId: number, languageIds: string[]): Promise<Map<string, number>> {
  const issues = (
    await translationStatusApi.withFetchAll().listQaCheckIssues(projectId, { languageIds: languageIds.join(',') })
  ).data.map((i) => i.data);
  const counts = new Map<string, number>();
  for (const issue of issues) {
    const key = `${issue.languageId} ${issue.category}`;
    counts.set(key, (counts.get(key) ?? 0) + 1);
  }
  return counts;
}
```

- `languageIds` is a comma-separated string, not an array; `category` and `validation` accept arrays.
- Each issue carries `stringId` and the offending `text`, enough to build a fix list or feed `stringTranslationsApi` corrections. `revalidateQaChecks` reruns the checks after a settings change and is another pollable operation.

## Screenshots and tags

**Problem:** translators cannot see where a string appears; screenshots with tagged strings show them.

**Modules:** `uploadStorageApi.addStorage` → `screenshotsApi.addScreenshot`, then `addTag` or `replaceTags`.

```ts
const { uploadStorageApi, screenshotsApi } = client;

/** Uploads a screenshot and lets Crowdin tag the strings it recognizes; scope recognition to a file when you know it. */
export async function uploadScreenshot(projectId: number, localPath: string, fileId?: number): Promise<number> {
  const storage = await uploadStorageApi.addStorage(basename(localPath), await readFile(localPath), 'image/png');
  const screenshot = await screenshotsApi.addScreenshot(projectId, { storageId: storage.data.id, name: basename(localPath), autoTag: true, fileId });
  return screenshot.data.id;
}

/** Manual tagging when you know where the string is rendered. */
export async function tagString(projectId: number, screenshotId: number, stringId: number, position: ScreenshotsModel.Position): Promise<void> {
  await screenshotsApi.addTag(projectId, screenshotId, [{ stringId, position }]);
}
```

- `autoTag` recognizes source text in the image; scoping with `fileId`, `directoryId` or `branchId` cuts false matches in big projects.
- Capturing the screenshots themselves from a running app is documented in [Automating Screenshot Management](https://support.crowdin.com/developer/automating-screenshot-management/).

## Over-the-air delivery: distributions and bundles

**Problem:** ship translations to apps without a release, or download a bundle the way the Crowdin UI does.

**Modules:** `distributionsApi.listDistributions` → `createDistributionRelease` → `getDistributionRelease`; `bundlesApi.exportBundle` → `checkBundleExportStatus` → `downloadBundle`.

```ts
const { distributionsApi, bundlesApi } = client;

/** Publishes the current translations to a distribution's CDN manifest. */
export async function releaseDistribution(projectId: number, name: string): Promise<string> {
  const distributions = (await distributionsApi.withFetchAll().listDistributions(projectId)).data.map((d) => d.data);
  const distribution = distributions.find((d) => d.name === name);
  if (!distribution) throw new Error(`No distribution named ${name}`);
  await distributionsApi.createDistributionRelease(projectId, distribution.hash);
  await waitFor(() => distributionsApi.getDistributionRelease(projectId, distribution.hash), { done: ['success'] });
  return distribution.manifestUrl;
}

/** A bundle export is the API twin of "Download bundle" in the project's Translations tab. */
export async function exportBundle(projectId: number, bundleId: number, target: string): Promise<void> {
  const op = await bundlesApi.exportBundle(projectId, bundleId);
  await waitFor(() => bundlesApi.checkBundleExportStatus(projectId, bundleId, op.data.identifier));
  const link = await bundlesApi.downloadBundle(projectId, bundleId, op.data.identifier);
  await saveUrl(link.data.url, target);
}
```

- A release finishes with status `success`, not `finished`; the release object has no identifier, it is addressed by the distribution `hash`.
- Apps read the manifest through the OTA client, not the API; the API's job ends at the release.

## Generate and download a report

**Problem:** a cost estimate, translator accuracy, top members or source content updates as a file.

**Modules:** `reportsApi.generateReport` → `checkReportStatus` → `downloadReport`.

```ts
const { reportsApi } = client;

/** Any report follows this shape; only `name` and `schema` change. */
export async function topMembersReport(projectId: number, dateFrom: string, dateTo: string, target: string): Promise<void> {
  const request: ReportsModel.GenerateReportRequest = {
    name: 'top-members',
    schema: { unit: 'words', format: 'csv', dateFrom, dateTo },
  };
  const op = await reportsApi.generateReport(projectId, request);
  await waitFor(() => reportsApi.checkReportStatus(projectId, op.data.identifier));
  const link = await reportsApi.downloadReport(projectId, op.data.identifier);
  await saveUrl(link.data.url, target);
}
```

- `GenerateReportRequest` is a union keyed by `name`; let the type narrow `schema` rather than building it as a loose object. Cost reports (`costs-estimation-pe`, `translation-costs-pe`) require `baseRates`, `individualRates` and `netRateSchemes`.
- Dates are ISO 8601 strings. `format` is `xlsx`, `csv` or `json`; `json` is what a script consumes next.
- Organization-wide reports on Crowdin Enterprise use `generateOrganizationReport` and `checkOrganizationReportStatus` on the same module.

## Upstream examples

The client repository's [EXAMPLES.md](https://github.com/crowdin/crowdin-api-client-js/blob/master/EXAMPLES.md) carries standalone scripts for creating and updating files, TM and glossary creation, pre-translation, downloads and a cost report. They are the long form of the recipes above and a place to check when a signature looks unfamiliar.
