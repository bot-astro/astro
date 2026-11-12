# Deploying Hyperion (backend + frontend)

Replaces the legacy deployment (Helm releases in namespace `astro`, ConfigMaps, old Nuxt site) with:

- **Backend**: Hyperion services deployed by ArgoCD from `bot-astro/infra`, config from Infisical via the Infisical operator.
- **Frontend**: `astro-bot.space` (Nuxt 4, branch `hyperion-rewrite`) on Cloudflare.

Hours of downtime are accepted. Cluster, Longhorn and data stores are not modified, except the two data cleanups in D3 (`errors` documents in MongoDB, `TEMP_VCS:*` keys in Redis).

Repos (siblings): `astro` (this), `infra`, `astro-bot.space`, `../../Giuliopime/gport`. kubectl context `gport`. Namespaces: prod `astro`, staging `astro-staging`.

**How to use this document.** Every step has a *Prompt* block. Paste it into Claude Code opened in the repo named in the block's first line, and Claude does the step (or tells you what you must do by hand). Steps marked **[you]** need a dashboard or a decision Claude cannot do. Claude must never apply Terraform, push to `infra`, uninstall Helm releases or delete data without you confirming the exact command first, and the prompts say so.

**Access.** The `AstroDevToolkit` skills (notably `astro-services-tools`, `astro-tech-overview`, `infisical-*`) document how to reach almost every service (Hetzner, kubectl, ArgoCD, Infisical, Cloudflare, GCP/BigQuery, MongoDB, Redis, Sentry, Semaphore, GitHub, Chargebee, Crowdin...). Whenever Claude needs a CLI, login or token for a service, ask the user to grant it (log in, add credentials, allow the tool) and Claude does the step itself instead of handing it back as **[you]**. Only steps that truly need a human (Discord portal, Chargebee dashboard, announcements) stay manual.

## Progress log

**Rule for Claude: right after finishing (or deciding to skip/defer) any step, or any action that changes the cluster, a repo, Terraform or an external service, append a line here (date, step, what was done, commit SHAs/ids, anything left open) and update "Next". Do it before moving on, so the conversation can be cleared at any time and work resumes from this section. Do not wait to be asked.**

| Date | Step | Result |
|---|---|---|
| 2026-11-12 | S | Deferred: no `cx` stock in any EU location. Control plane stays in S as the lowest priority. `gport` `kube-hetzner.tf` server types unchanged (cpx) |
| 2026-11-12 | A1 | Done. `gport` commit `6e02072` (Infisical operator Application + `enableOTLPReceiver`), pushed. `infisical-operator` Synced/Healthy, CRDs `secrets.infisical.com` present, OTLP receiver on |
| 2026-11-12 | A1 extra | Prometheus volume was already faulted (single replica, failed 2026-10-27, no backup): PVC deleted and recreated empty, metric history lost (accepted). All Longhorn volumes attached/healthy. 9 stale Longhorn node objects deleted |
| 2026-11-12 | Autoscaler cap | `max_nodes = 3` (min 3) in `kube-hetzner.tf`, `gport` commit `6878f40`. Live `cluster-autoscaler` already runs `--nodes=3:3:cpx52:...`, applied with a targeted `terraform apply` on `configure_autoscaler` only |
| 2026-11-12 | Firewall | Hetzner firewall `gport` is wide open on purpose (all inbound TCP/UDP/ICMP, all outbound). `kube-hetzner.tf` aligned to it, `gport` commit `f97b338`, firewall plan has no diff. Left unapplied (optional housekeeping): local `kustomization_backup` file and the autoscaled-node registry entry swap (could restart k3s on one node if `registries.yaml` differs) |
| 2026-11-12 | A2 config check | Read-only check done. All required keys present in prod/staging (except pod-injected ones and, in staging, `BOT__BIGQUERY_DATASET_NAME` + `BOT__GCP_CREDENTIALS_JSON`; prod lacks only `BOT__GCP_CREDENTIALS_JSON`). Ports, replicas (11/176 prod, 1/1 staging), REDIS_HOST, SUPPORT_BOT_ENDPOINT_BASE_URL, SB auth match, prod/staging secrets differ: OK. Open: `CA__CORS_ALLOWED_ORIGIN` still `http://localhost:3000` in both; staging `PREMIUM_RESTRICTIONS=true` (doc says false); staging `MONGODB_URI` has no database path (Spring defaults to `test`, so isolated from prod `AstroDB` but probably unintended); `R2_*` (bucket `astro-media`) exist in both envs and must be saved and deleted before the A3 Terraform apply; prod `BOT__BIGQUERY_DATASET_NAME` already `astro_bot` |
| 2026-11-12 | A2 fixes | Set `CA__CORS_ALLOWED_ORIGIN` (prod `https://astro-bot.space`, staging `https://staging.astro-bot.space`) and staging `PREMIUM_RESTRICTIONS=false` in Infisical. Still open: staging `MONGODB_URI` database name, delete old `R2_*` before A3 |
| 2026-11-12 | A2 `astro-k8s-operator` | Created via API: identity id `7050ad6b-29cd-47e5-af2c-2be29db7d26b`, Kubernetes Auth (host `https://91.98.7.112:6443`, namespaces `astro,astro-staging`, SA `infisical-service-account`, review mode api), Viewer on the backend project. Cluster: SA `infisical-token-reviewer` + clusterrolebinding `system:auth-delegator` in `kube-system`, 1y token. Not verified end to end until the operator authenticates in B |

| 2026-11-12 | A2 `astro-terraform` | Created via API: identity id `c3139bf7-973d-4ed0-bbba-03ddb58a8866`, Universal Auth, admin on the backend project. `infisical_client_id/secret` (+ `infisical_project_id` for cloudflare) appended to gitignored `infra/terraform/{cloudflare,gcp}/terraform.tfvars`. A2 done |
| 2026-11-12 | A5 | Sealed identity id for `astro` and `astro-staging` (`apps/infisical/envs/*/infisical-kubernetes-credentials-secret-sealed.yml`, added to kustomizations), all `kustomize build apps/*/envs/*` pass, the 9 InfisicalStaticSecrets point at the right project/env/secret names. Committed locally in `infra`, **not pushed yet** (awaiting user OK). ArgoCD repo access still **[you]** |

| 2026-11-12 | A5 push | `bot-astro/infra` did not exist on GitHub (local repo had no remote). Scanned tracked files and the 3 commits of history: no plaintext secrets (only SealedSecrets, `*.template.yml`, `backend.hcl.template`; tfvars gitignored). Created it **public** with `gh repo create bot-astro/infra --public --push`, branch `master` (includes `285cc80`). Public means ArgoCD needs no repo credentials; the Semaphore deploy key (A4) can now be added to this repo |
| 2026-11-12 | A3 prep | `cf` CLI is logged in (OAuth, expires ~1h, lacks the "Account API Tokens Write" scope). Created R2 bucket `astro-terraform-state` and account token `astro-terraform-state` (R2 Bucket Item Write on that bucket). Wrote `infra/terraform/backend.hcl`; the S3 creds (`AWS_ACCESS_KEY_ID/SECRET`) are in `~/.config/astro/tf-state-creds.env` (outside git). `terraform.tfvars` of `cloudflare/` and `gcp/` have the Infisical creds (cloudflare also account id, zone `e334135d93d94d9e2c015ac7d538c45e`, ingress `91.98.7.60`; gcp has `gcp_project_id`). **Missing: `cloudflare_api_token`** (OAuth session cannot mint tokens with Account API Tokens Write) |
| 2026-11-12 | A3 gcp plan | `terraform init` + `plan` in `infra/terraform/gcp` (state in R2): 6 import (prod dataset + 5 tables), 18 add, 0 change, **0 destroy**. Not applied yet (awaiting user OK). Before apply: delete prod `BOT__BIGQUERY_DATASET_NAME` (value `astro_bot`) in Infisical, terraform recreates it |
| 2026-11-12 | A3 Cloudflare token | User provided a Cloudflare user API token (`cfut_...`, verified active). Stored **temporarily, outside git** in `~/.config/astro/cloudflare-token.env` (`CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, mode 600) and in the gitignored `infra/terraform/cloudflare/terraform.tfvars`. Terraform state creds are in `~/.config/astro/tf-state-creds.env`. **At the end of the deploy (E2): delete both files in `~/.config/astro/`, remove the token line from `terraform.tfvars`, and revoke the token in Cloudflare (My Profile → API Tokens), since it was pasted in a chat** |
| 2026-11-12 | A3 cloudflare plan | Imported DNS `api` and `support-bot` into state. Fixed `infra/terraform/cloudflare/main.tf` (`resources = jsonencode({...})` in `cloudflare_api_token.r2`, provider v5 needs a string; **uncommitted in infra**). Plan: 16 add, 0 change, 0 destroy (2 DNS, 2 buckets, 2 custom domains, 2 tokens, 8 Infisical `R2_*`). Old bucket `astro-media` verified empty, still holds `cdn.astro-bot.space`. Terraform does NOT manage `R2_PUBLIC_URL` (keep it). Blocked: deleting the old Infisical `R2_ACCESS_KEY_ID/SECRET/BUCKET_NAME/ENDPOINT_URL` (prod+staging) and prod `BOT__BIGQUERY_DATASET_NAME` was denied by the permission classifier; then delete bucket `astro-media` (remove its custom domain first) and apply |
| 2026-11-12 | A3 cloudflare applied | User deleted old Infisical `R2_*` (kept `R2_PUBLIC_URL`) and prod `BOT__BIGQUERY_DATASET_NAME` (backup in `~/.config/astro/old-r2-backup-2026-11-12.env`), bucket `astro-media` and its `cdn` domain. User ran `terraform apply` in `infra/terraform/cloudflare`: 16 added, 0 destroyed (buckets `astro-media-prod/staging`, domains `cdn[-staging].astro-bot.space`, DNS `api-staging`, `support-bot-staging`, 2 R2 tokens, 8 Infisical `R2_*`). `main.tf` fix committed and pushed in `infra`. **gcp apply still pending** (user runs it: `terraform apply -auto-approve`, `!` has no tty, the classifier blocks Claude from applying) |
| 2026-11-12 | A3 gcp applied | User ran `terraform apply` in `infra/terraform/gcp`: 6 imported, 18 added, 0 destroyed. Datasets `astro_bot` (adopted, 5 tables intact) and `astro_staging` exist; `BOT__BIGQUERY_DATASET_NAME` and `BOT__GCP_CREDENTIALS_JSON` now in Infisical prod+staging. `gcp/imports.tf` removed and pushed (`32b77c1`). **A3 done**. Cloudflare staging DNS exists; `terraform/cloudflare/.cloudflare/` is an untracked cf CLI cache dir, ignore |
| 2026-11-12 | A7 partial | Backups in `~/astro-backups/2026-11-12/` (mode 700, outside git): `redis-backup.rdb` (325 MB, DBSIZE 26835 at the time), `astro-legacy-backup.yaml` (cm/secret/ingress/sts/deploy/cronjob/svc of `astro`), `helm-values-{bot,central-api,support-bot,entitlements-expiration-job}.yaml`. BigQuery: 5 tables copied into dataset `astro_bot_backup_20261112`, row counts equal (6200665 / 247030 / 655095 / 1456295 / 14465508). Note for zsh: write `${P}:dataset.table`, `$P:x` is a path modifier. **Still open: MongoDB Atlas snapshot [you], Longhorn volume snapshots (Redis + Prometheus), note the old Cloudflare frontend deployment id** |
| 2026-11-12 | A4 Semaphore | User provided a Semaphore API token (org `astro-bot.semaphoreci.com`). Stored **temporarily, outside git** in `~/.config/astro/semaphore-token.env`; `sem connect` also keeps it in `~/.sem.yaml`. **After the deploy: delete `~/.config/astro/cloudflare-token.env` and `~/.config/astro/semaphore-token.env`, run `sem disconnect`/remove `~/.sem.yaml`, and revoke both tokens (Cloudflare My Profile → API Tokens, Semaphore Profile → API Token regenerate), since they were pasted in a chat.** Secrets found: `github` (`GITHUB_ACTOR`, `GITHUB_TOKEN`), `sentry` (`SENTRY_AUTH_TOKEN`), plus legacy `helm_*`, `kube` (delete in E2). Created `infra-deploy-key` (file `/home/semaphore/.ssh/infra_deploy_key`) and its public half as read-write deploy key `semaphore-deploy` on `bot-astro/infra`; private key not kept locally. `GHCR_ORGANIZATION` was missing everywhere (read by `service-conventions.gradle.kts`): added to `global_job_config.env_vars` in `.semaphore/semaphore.yml` (**uncommitted**, needs merge to master). Not verified: that `GITHUB_TOKEN` has `write:packages` |
| 2026-11-12 | A7 Longhorn + F1 | Longhorn snapshot `pre-hyperion-20261112` of the Redis volume (`pvc-336b6ed4...`, local only, no backup target configured; Prometheus skipped, empty). F1 done in `astro-bot.space` (`hyperion-rewrite`, pushed): SSR on Cloudflare Workers (the app forwards the session cookie during SSR, dynamic `/guilds/[guild_id]` routes, so not static), `@nuxt/content` auto-switches to D1 binding `DB`. Created D1 `astro-bot-space-prod` (`2a2b8ec4-d817-4fb8-b9b3-83a339afe825`) and `astro-bot-space-staging` (`ec96a544-8602-4e56-95df-8d4f0c56a802`), `wrangler.jsonc` (top level = prod, `env.staging`), `.node-version` 24, README Deploy table. Robots: staging noindex via `NUXT_SITE_ENV=staging`. Not built or deployed (CLAUDE.md: never build). **F2 needs the user**: connect the GitHub repo to Cloudflare Workers Builds (GitHub app authorisation), settings in the README table of `astro-bot.space` |

Next: A4 (needs `sem connect`), A7 backups, A6 Discord redirects **[you]**, then F and B. (Older "Next" text below is obsolete.) A3 needs Cloudflare credentials Claude does not have (cf CLI not authenticated): R2 bucket `astro-terraform-state` + its S3 token (`backend.hcl`, `AWS_*`), `cloudflare_api_token`, `cloudflare_account_id`, `cloudflare_zone_id`, `r2_bucket_item_write_permission_group_id`. Known values: `ingress_ipv4` = `91.98.7.60` (Hetzner LB `gport-nginx`), `gcp_project_id` = `astro-405113`. Also before apply: save and delete old `R2_*` in Infisical prod/staging, empty/delete bucket `astro-media`, import the 2 DNS records. Then A5 (seal identity `7050ad6b-29cd-47e5-af2c-2be29db7d26b`), which does not depend on A3.

## 0. State verified on 2026-11-05

Checked by comparing every `${VAR}` without default in the `application.yml` of `bot`, `api-central`, `support-bot` and `entitlements-expiration-job` with the keys in Infisical:

| Check | Result |
|---|---|
| Required keys in `prod` and `staging` | All present, except `BOT__POD_HOSTNAME`/`BOT__POD_ORDINAL` (injected by the StatefulSet, correct to be absent) and `BOT__BIGQUERY_DATASET_NAME` in `staging` (written by Terraform, A3) |
| `CA__CORS_ALLOWED_ORIGIN` | **Wrong in both `prod` and `staging`: `http://localhost:3000`.** Must be the frontend origin or every dashboard request fails CORS. Fixed in A2 |
| Cookie config | OK: `CA__SESSION_COOKIE_DOMAIN` = `astro-bot.space` / `staging.astro-bot.space`, secure `true`, SameSite `Lax`, allow-origin = frontend origin, `FRONTEND_BASE_URL` set |
| `DISCORD_OAUTH_REDIRECT_URI` | prod `https://api.astro-bot.space/v2/auth/discord/callback`, staging `https://api-staging.astro-bot.space/v2/auth/discord/callback` (must also be registered in the Discord portals, A6) |
| Frontend Infisical project (`c624625b-...`) | `ASTRO_API_BASE_URL` and `CHARGEBEE_SITE` set in `dev`, `staging`, `prod` with the right values (`https://api[-staging].astro-bot.space`, site `astro-bot` for prod and `astro-bot-test` for staging). Plus `CROWDIN_*` (translations only, not needed at build) |
| Frontend deploy config | None in the repo (no wrangler file, no CI). Hosting is Cloudflare, configured in the dashboard: F section |

Re-run the check before the cutover (prompt in A2).

## 1. Order

```
S  DEFERRED: cpx → cx node migration (no cx stock, see S), do it after the deploy
A  Prepare (old version keeps running)   gport, Infisical, Terraform, Semaphore, infra repo, Discord, backups
B  Merge + build + staging up            backend images, staging apps, staging frontend
C  Test staging end to end               backend + frontend
D  Production cutover (downtime)         stop legacy, clean data, start new backend, deploy new frontend
E  After                                 Chargebee webhooks, cleanup
```

Danger: as soon as ArgoCD sees `apps/*/envs/astro`, it starts the new prod bots. If the legacy bots still run, two processes share one Discord token and Redis keys. Prod stays excluded in the appset until D4.

The new frontend talks to the `/v2` API only. It goes live in prod **after** the new API is up (D5), and the old frontend is down or broken between D2 and D6: that is the downtime.

## S. Right-size servers (DEFERRED, skip for now)

**Status 2026-11-12: deferred. Start at A.** Checked live with the Hetzner API (`/v1/server_types`, `locations[].available`): `cx23` is sold out in nbg1/fsn1 (only hel1), `cx33`, `cx43` and `cx53` are sold out in every location, `cax` too. Changing location does not help (hel1 is ~15% cheaper for the same cpx types, not worth moving the cluster). The cpx nodes keep running as they are: the two old cpx52 are at the grandfathered €36.49, and the deploy does not change node count or types. Rules until S is done: do not touch `agent-sailboat`/control-plane, do not drain autoscaler nodes (a replacement would be a cpx52 at €100.49), keep `min_nodes = 3` / `max_nodes = 3` (capped on 2026-11-12 so the autoscaler can never buy a 4th server at €100.49; raise it only on purpose).

Also corrected: cx53 is €35.98 gross (≈€29.49 net) in the Hetzner API, so prices in this section and in `docs/infrastructure.md` are net of VAT, the CLI shows gross.

Decision 2026-11-12: the control plane (3x cpx22 → cx23, saves ~€42/month net) stays in S but is the lowest priority: do it last, only when cx23 is available in nbg1 (today only hel1), one node at a time to keep etcd quorum, and note the disk drops from 80 to 40 GB. Order when S resumes: autoscaler pool, then `agent-sailboat`, then control plane.

When to resume: after the deploy and the JVM memory work (E3), when `cx33`/`cx53` are available in nbg1 again (check `curl -s -H "Authorization: Bearer $(grep -m1 token ~/.config/hcloud/cli.toml | sed 's/.*= *\"\(.*\)\"/\1/')" https://api.hetzner.cloud/v1/server_types?per_page=50`, field `locations[].available`). The text below is the original plan, kept for then.

Original rationale (kept):

Why first: Hetzner raised CPX prices on 2026-06-15; every autoscaler replacement pays the new price (cpx52 €100.49 vs cx53 €29.49). Moving to `cx` saves ~€154/month with the same RAM. Full analysis in `docs/infrastructure.md`. The pod right-sizing (step 2 there, ~€29/month more) is **not** part of the deploy: it is JVM work done after (E3), so keep `min_nodes = 3` / `max_nodes = 4` for now (11 bots x 5 Gi requests need 3 nodes).

Node replacement is kept apart from the app migration on purpose: do S, let it settle (at least a few days, watch shard latency and memory), then start A. The legacy bots keep running and each node drain restarts some of their pods: accepted.

Target (already in `gport/hcloud-cluster/kube-hetzner.tf`, uncommitted; details and rollout in `gport/hcloud-cluster/docs/cx-migration.md`):

| Pool | Now | Target |
|---|---|---|
| `control-plane` (3) | cpx22 | cx23 |
| `agent-sailboat` (1, holds Longhorn volumes) | cpx32 | cx33 |
| `agent-as-cruiser` (autoscaler, 3) | cpx52 | cx53 |

Known risks: stock on 2026-10-24 had only cx23 available (cx33 sold out, cx53 not available), cx53 disk is 320 GB (cpx52 480 GB), and replacing `agent-sailboat` destroys local disk, where the Longhorn Redis (30 GB) and Prometheus (10 GB) volumes live (single replica).

### S1. **[you]** Check capacity and backups

- Hetzner console (or `hcloud server-type list`): cx23, cx33, cx53 available in nbg1. If one is sold out, ask Hetzner support or use the fallback (cx43 8c/16G €15.99 for the agent; for the autoscaler stay on cpx52 until cx53 is available, do not replace those nodes) and update `kube-hetzner.tf` and `cx-migration.md` accordingly.
- Take the A7 backups of **Redis and Longhorn** now (Redis `BGSAVE` + copy, Longhorn snapshot/backup of the Redis and Prometheus volumes). They protect the agent node replacement in S3.

### S2. Plan the change

Prompt:
```
Repo: ../../Giuliopime/gport
Review `git diff hcloud-cluster/kube-hetzner.tf` and hcloud-cluster/docs/cx-migration.md (fix its reference to the non-existent docs/infra-cost-migration.md: the analysis is in bot-astro/astro docs/infrastructure.md). Check (kubectl context gport, hcloud context gport): current servers and types (`hcloud server list`), Longhorn volumes and where their replicas live (`kubectl get volumes.longhorn.io -n longhorn-system`, `kubectl get replicas.longhorn.io -n longhorn-system -o wide`), disk usage per node vs the cx53 320 GB disk, requests per node (`kubectl describe nodes | grep -A8 "Allocated resources"`). Then run `terraform init && terraform plan` in hcloud-cluster/ and READ the plan to me. Hard stop if the plan destroys or replaces more than the pool I am about to change, or anything outside nodepools. Do NOT apply and do NOT commit anything yet. Give me the rollout order, one pool per apply, with the exact commands.
```

### S3. Autoscaler pool (cx53)

Prompt:
```
Repo: ../../Giuliopime/gport
Do the autoscaler step of docs/cx-migration.md, one node at a time. Show me each exact command and wait for my OK before running it (drain, terraform apply, anything deleting a server). Order: apply only the autoscaler pool change (`-target` or only that pool edited, as I confirm), cordon and drain ONE cpx52 node (`kubectl drain <node> --ignore-daemonsets --delete-emptydir-data`), wait until the autoscaler brings a cx53 replacement and all its pods are Ready, report `kubectl get nodes -o wide` and `kubectl top nodes`. Stop there: I watch the new node for a few days (bot pods Ready, shard latency, memory) and ask you to replace the next two. Never drain two nodes at once. Remember the Longhorn volumes are NOT on autoscaler nodes (only on agent-sailboat): verify before each drain with the replicas listing.
```

Repeat for the remaining two cpx52 nodes, one at a time, after the watch period. The two old cpx52 at €36.49 are the expensive ones to lose (replacing them with cpx52 would cost €100.49 each), so the autoscaler must never recreate a cpx52: confirm `server_type = "cx53"` is applied before draining.

### S4. Agent node (cx33) and control plane (cx23)

Prompt:
```
Repo: ../../Giuliopime/gport
Continue docs/cx-migration.md: first agent-sailboat, then the control plane, one step at a time with my confirmation of each exact command.
agent-sailboat: confirm fresh Longhorn backups of the Redis and Prometheus volumes exist (S1), then, because replacing the node destroys local disk data, tell me the safest way (raise the Longhorn replica count and wait for rebuild onto the autoscaler nodes, or backup and restore) and wait for my choice. After the replacement verify Redis (redis-master-0 Ready, DBSIZE sane vs before) and Prometheus data, and that all Longhorn volumes are healthy/attached.
control-plane: one node at a time, never more than 1 of 3 down (etcd quorum): cordon, drain, remove, apply, wait for the new node Ready and `kubectl get --raw=/readyz` OK before the next. If etcd or the API server gets OOM-killed on 4 GB nodes, stop and propose cx33 (€8.49) for the control plane.
```

### S5. Done when

- [ ] `hcloud server list` shows only `cx` types and the node count/pools match `kube-hetzner.tf`; `terraform plan` is clean (no diff).
- [ ] All nodes Ready, all Longhorn volumes healthy/attached, legacy bots/Redis/Prometheus healthy, no OOM kills on control-plane nodes.
- [ ] The `kube-hetzner.tf` and `cx-migration.md` changes are committed and pushed in gport (**[you]** confirm the diff first).
- [ ] The next Hetzner invoice/pricing page shows the expected costs (see `docs/infrastructure.md` totals).

## A. Preparation (no downtime)

### A1. gport: Prometheus OTLP receiver + Infisical operator

Prompt:
```
Repo: ../../Giuliopime/gport
Commit and push only these two changes: argocd/apps/infisical-operator.yaml (new) and the enableOTLPReceiver: true change in k8s-resources/kube-prometheus-stack/values.yaml. Do NOT touch hcloud-cluster/kube-hetzner.tf or hcloud-cluster/docs/cx-migration.md (the server migration in S is deferred and its uncommitted changes must stay out of this commit; node replacement must not happen together with the app migration). Show me the git diff and the commit message first, wait for my OK, then push. Afterwards verify (kubectl context gport): argocd applications infisical-operator and kube-prometheus-stack are Synced/Healthy, CRDs secrets.infisical.com exist, pods in infisical-operator-system run, the prometheus pod restarted once and its Longhorn PVC is still bound, and all Longhorn volumes are healthy/attached. Report anything off.
```

### A2. Infisical

Project `b9071f19-b984-4138-a1e8-a15c67e02603` (backend), `c624625b-a4c6-41b6-aac9-eb9985ff2469` (frontend).

**[you] Machine identities**

1. `astro-k8s-operator`, **Kubernetes Auth**, role Viewer on the backend project (`prod` + `staging`):
   - API URL: cluster API (Cloudflare record from gport). Infisical Cloud must reach it on 6443: check the control plane firewall/load balancer in `hcloud-cluster/kube-hetzner.tf`.
   - Token reviewer JWT:
     ```sh
     kubectl create serviceaccount infisical-token-reviewer -n kube-system
     kubectl create clusterrolebinding infisical-token-reviewer --clusterrole=system:auth-delegator --serviceaccount=kube-system:infisical-token-reviewer
     kubectl create token infisical-token-reviewer -n kube-system --duration=8760h
     ```
   - CA certificate: `certificate-authority-data` from the kubeconfig, base64-decoded.
   - Allowed service account names `infisical-service-account`, namespaces `astro,astro-staging`.
   - Copy the **Identity ID** (used in A5).
2. `astro-terraform`, **Universal Auth**, write access to the backend project. Copy client id/secret (for `terraform.tfvars`, A3).

**Fix the CORS value and re-verify config**

`CA__CORS_ALLOWED_ORIGIN` is a single value (the property builds a one-element list). Set `prod` = `https://astro-bot.space`, `staging` = `https://staging.astro-bot.space`.

Prompt:
```
Repo: astro
1. Run `infisical export --env=<env> --format=json` for staging and prod (print KEYS ONLY, never values, except for these non-secret ones: FRONTEND_BASE_URL, CA__CORS_ALLOWED_ORIGIN, CA__SESSION_COOKIE_DOMAIN, CA__SESSION_COOKIE_ALLOW_ORIGIN, CA__SESSION_COOKIE_SECURE, CA__SESSION_COOKIE_SAME_SITE, DISCORD_OAUTH_REDIRECT_URI, R2_PUBLIC_URL, SUPPORT_BOT_ENDPOINT_BASE_URL, BOT_SHARD_MANAGER_TOTAL_PODS, BOT_SHARD_MANAGER_TOTAL_SHARDS, REDIS_HOST, MONGODB_URI host only).
2. Compare every ${VAR} without default in services/{bot,api-central,support-bot,entitlements-expiration-job}/src/main/resources/application.yml (and anything under shared/core that reads env) with those keys, and tell me what is missing per env. Expected missing: BOT__POD_HOSTNAME/BOT__POD_ORDINAL (injected by the StatefulSet), BOT__BIGQUERY_DATASET_NAME and R2_*/BOT__GCP_CREDENTIALS_JSON in staging until Terraform runs.
3. Check the values against what the manifests in ../infra/apps/*/envs/* expect: ports (bot 9000, api-central 9001, support-bot 9002), SUPPORT_BOT_ENDPOINT_BASE_URL = http://support-bot.<namespace>:9002, BOT_SHARD_MANAGER_TOTAL_PODS = replicas in apps/bot/envs/<env>/replicas.yml, TOTAL_SHARDS = pods x shards per pod, REDIS_HOST (prod redis-master.databases.svc.cluster.local, staging redis-staging), that staging uses a DIFFERENT Discord application token, Mongo database and Redis from prod, and that SUPPORT_BOT_ENDPOINT_AUTH equals SB__API_AUTH in each env.
4. Flag CA__CORS_ALLOWED_ORIGIN if it is not exactly the frontend origin of that env (prod https://astro-bot.space, staging https://staging.astro-bot.space). Do not change anything, give me a report and the exact `infisical secrets set` commands, I run them.
```

**[you]** Staging keys that must be a separate staging application/database (verify the prompt's report): `BOT__DISCORD_APPLICATION_*`, `EEJ__*`, `CA__DISCORD_APPLICATION_*`, `DISCORD_OAUTH_*`, `SB__*`, `MONGODB_URI`, `REDIS_*`, Chargebee test site (`astro-bot-test`), `PREMIUM_RESTRICTIONS=false`.
### A3. Terraform (`infra/terraform`)

Creates R2 buckets (`astro-media-prod/staging`), `cdn[-staging].astro-bot.space`, DNS for `api`, `support-bot` and staging hosts, BigQuery (adopts the existing prod dataset, creates staging), service accounts, and writes `R2_*`, `BOT__BIGQUERY_DATASET_NAME`, `BOT__GCP_CREDENTIALS_JSON` into Infisical. Details: `terraform/README.md`.

**[you] before**: create R2 bucket `astro-terraform-state` + R2 API token; `cp backend.hcl.template backend.hcl` and fill it; `export AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY`; fill `terraform.tfvars` for both modules (Infisical `astro-terraform` creds, Cloudflare token, zone id, `ingress_ipv4` = gport `module.kube-hetzner.ingress_public_ipv4`).

Prompt:
```
Repo: ../infra (terraform/)
Read terraform/README.md and walk me through the Terraform apply for cloudflare/ and gcp/. Rules:
- First list which of R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET_NAME, R2_ENDPOINT_URL, BOT__BIGQUERY_DATASET_NAME, BOT__GCP_CREDENTIALS_JSON exist in Infisical prod/staging (keys only). Terraform fails if they exist: tell me to save the old R2_* values aside and delete them, and give me the commands.
- The old prod bucket astro-media must be verified empty (I do this in the Cloudflare dashboard) and deleted, because cdn.astro-bot.space can be attached to one bucket only.
- The prod DNS records api and support-bot already exist: give me the terraform import commands from the README.
- Run `terraform init -backend-config=../backend.hcl && terraform plan` in each module and READ the plan to me. Hard stop if gcp/ shows any destroy or replace on a BigQuery dataset or table, or if cloudflare/ shows any destroy. Imported DNS records may show in-place updates, fine.
- Apply only after I confirm the plan. After the first successful apply of gcp/, delete gcp/imports.tf.
- Afterwards verify the keys now exist in Infisical for both envs and that https://cdn.astro-bot.space resolves.
```

### A4. Semaphore and GitHub

**[you]** Check with `sem get secrets`:
- `github`: `GHCR_ORGANIZATION=bot-astro`, `GITHUB_ACTOR`, `GITHUB_TOKEN` (`write:packages`).
- `sentry`: `SENTRY_AUTH_TOKEN`.
- `infra-deploy-key`: file `/home/semaphore/.ssh/infra_deploy_key` (private key). Create: `ssh-keygen -t ed25519 -N "" -f infra_deploy_key`, add the `.pub` to `bot-astro/infra` → Settings → Deploy keys with **write access**.

Prompt:
```
Repo: astro
Audit .semaphore/*.yml and ../infra/scripts/{deploy.sh,bump-version.sh} in depth: pipeline triggers only on master, the build blocks' change_in paths, secrets used vs the secrets listed above, jib push of <sha>/<version>/latest to ghcr.io/bot-astro/<service>, staging promotion auto and prod promotion manual, the deploy script cloning infra with the deploy key, yq bump of apps/<app>/envs/<env>/version.yml, commit and push with rebase retry. Tell me every bug or missing piece. Remember an image tag is the git SHA: a prod promotion only works for a commit whose build of that service actually ran.
```

### A5. Infra repo: sealed secrets and ArgoCD access

`docker-registry` SealedSecrets already exist for both namespaces. The Infisical identity secret does not yet.

Prompt:
```
Repo: ../infra
1. Seal the Infisical machine identity id (I give it to you now: <IDENTITY_ID>) for apps/infisical/envs/astro and apps/infisical/envs/astro-staging following the templates in secrets/infisical-kubernetes-credentials-secret.template.yml, using scripts/seal-secrets.sh infisical. Check the sealed files are the only thing that gets committed (the plain *-secret.yml must be gitignored) and add the sealed file to resources: in each kustomization.yml.
2. Run `for d in apps/*/envs/*; do kustomize build $d > /dev/null || echo FAIL $d; done` and fix failures.
3. Review that every InfisicalStaticSecret (apps/infisical/envs/*) points at project b9071f19-b984-4138-a1e8-a15c67e02603, environment slug prod for astro and staging for astro-staging, and that the generated Secret names match what the workloads reference (bot-config, api-central-config, support-bot-config, entitlements-expiration-job-config, redis-staging-config).
4. Show me the diff, wait for my OK, commit and push. Nothing deploys yet because the root app is not applied.
```

**[you]** ArgoCD must be able to read `bot-astro/infra`. If private, add credentials (Settings → Repositories, HTTPS + token or deploy key); persist them as a sealed secret in `gport/argocd-installation`.

### A6. External services **[you]**

- **Discord developer portal**, prod application: add redirect `https://api.astro-bot.space/v2/auth/discord/callback` (OAuth2 → Redirects). Staging application: `https://api-staging.astro-bot.space/v2/auth/discord/callback`. Keep the old redirect until E.
- **Cloudflare**: DNS/R2 by Terraform (A3). The frontend domains are configured in F.
- **Chargebee**: webhooks changed in E1 (they need the new API up).

### A7. Backups (before the cutover)

| What | How |
|---|---|
| MongoDB | **[you]** Atlas → Backup → on-demand snapshot |
| Redis | `kubectl exec -n databases redis-master-0 -- redis-cli -a "$(kubectl get secret rediscreds -n databases -o jsonpath='{.data.password}' \| base64 -d)" BGSAVE`, wait until `LASTSAVE` changes, then `kubectl cp databases/redis-master-0:/data/dump.rdb ./redis-backup.rdb` |
| Longhorn | Longhorn UI → snapshot/backup of the Redis and Prometheus volumes |
| Legacy config | `kubectl get cm,secret,ingress,sts,deploy,cronjob,svc -n astro -o yaml > astro-legacy-backup.yaml` (contains secrets, outside git) and `helm get values -n astro <release>` for `bot`, `central-api`, `support-bot`, `entitlements-expiration-job` |
| BigQuery | `bq cp` the 5 tables of `astro_bot` (`CONNECTION_INVOCATION`, `GUILD_EVENT`, `GUILD_SNAPSHOT`, `SLASH_COMMAND_INVOCATION`, `TEMPORARY_VC_GENERATION`) into a backup dataset |
| Old frontend | Note the current Cloudflare deployment id so you can roll back (F5) |

Prompt:
```
Repo: astro
Run the Redis BGSAVE + copy, the legacy config dump and the helm get values dumps from docs/deploy-hyperion.md A7 into ~/astro-backups/<today>/ (outside any git repo), and the bq cp of the 5 astro_bot tables into a dataset astro_bot_backup_<today>. Confirm each file is non-empty and each table copy has the same row count as the source. Do not delete or modify anything.
```

## F. Frontend preparation (can be done during A, B, C)

Facts: Nuxt 4, modules `@nuxt/content` (uses `better-sqlite3`), `@nuxt/image`, `@nuxtjs/i18n` (locales `en-US`, `it`), sitemap, robots. `runtimeConfig.public` reads `ASTRO_API_BASE_URL` and `CHARGEBEE_SITE` from `process.env` **inside `nuxt.config.ts`**, so they are baked at **build time**: they must be set as Cloudflare *build* variables, and each environment (prod, staging) needs its own build with its own values. Branch `hyperion-rewrite` is 56 files ahead of `main`.

### F1. Decide Pages vs Workers and make the build work on Cloudflare

Nuxt Content v3 stores content in SQLite; on Cloudflare it needs the Cloudflare/D1 setup, which is the part most likely to break.

Prompt:
```
Repo: astro-bot.space
Make the app deployable on Cloudflare (Pages or Workers with static assets, use whichever Cloudflare currently recommends for Nuxt 4 / Nitro) and tell me exactly what to configure in the Cloudflare dashboard. Check in order:
1. Nitro preset for Cloudflare and whether `nuxt build` (SSR) or `nuxt generate` (static) fits: look at which routes need server rendering (content pages, dashboard with cookie auth is client-side calling the API?). Prefer static generation if every route can be prerendered, otherwise SSR on Workers.
2. @nuxt/content 3.16 on Cloudflare: required database config (D1 binding or other), wrangler config, and what it needs at build time. better-sqlite3 is native: check it does not break the Cloudflare build.
3. @nuxt/image (provider), @nuxt/fonts, sitemap url (site.url is hard-coded to https://astro-bot.space: it must not index the staging site, add noindex/robots disallow for staging via an env flag), the particles.js CDN script, and routeRules redirects in app/constants/redirects.
4. The API calls: confirm they send credentials (cookie session) to ASTRO_API_BASE_URL and that this works cross-origin from astro-bot.space -> api.astro-bot.space given the backend config (CA__CORS_ALLOWED_ORIGIN = frontend origin, cookie domain astro-bot.space, Secure, SameSite=Lax). Read services/api-central in ../astro to confirm CORS allowCredentials and methods.
5. Add the config files needed (wrangler.jsonc, nuxt.config changes, .node-version or engines, pnpm version) and a short README "Deploy" section. Do NOT build or run anything that touches Cloudflare. Show me the diff.
Then give me the exact dashboard settings: framework preset, build command, output directory, Node version, environment variables per environment (ASTRO_API_BASE_URL, CHARGEBEE_SITE, anything else you found), production branch and preview/staging branch.
```

### F2. **[you]** Cloudflare project

Create one Cloudflare project connected to `bot-astro/astro-bot.space` with the settings from F1:

| Environment | Branch | Domain | Build variables |
|---|---|---|---|
| Production | `main` (after merge) | `astro-bot.space` (+ `www` redirect if it exists today) | `ASTRO_API_BASE_URL=https://api.astro-bot.space`, `CHARGEBEE_SITE=astro-bot` |
| Staging (preview/branch deploy) | `hyperion-rewrite` | `staging.astro-bot.space` | `ASTRO_API_BASE_URL=https://api-staging.astro-bot.space`, `CHARGEBEE_SITE=astro-bot-test` |

Do not attach `astro-bot.space` to the new project until D6: it is currently serving the old site. Before that, test through the project's `*.pages.dev`/`*.workers.dev` URL or the staging domain.

For the preview URL to work against the staging API the origin must be allowed by CORS: staging only accepts `https://staging.astro-bot.space`. So test staging through `staging.astro-bot.space`, not through the `*.pages.dev` URL.

### F3. Frontend release checklist before D6

- [ ] `hyperion-rewrite` builds on Cloudflare with the staging variables and is reachable on `staging.astro-bot.space`.
- [ ] Prod build (branch `main` after merge, or a preview of `hyperion-rewrite` with prod variables) succeeds and is *not yet* attached to the prod domain.
- [ ] Crowdin: `l10n_hyperion-rewrite` translation PR is merged into `hyperion-rewrite` and `i18n/*.json` are the final ones.

## B. Merge, build, staging up

1. **Backend**: merge `version/hyperion` into `master`. Semaphore builds every service and pushes `<sha>`, `<version>`, `latest` to `ghcr.io/bot-astro/<service>`; each build auto-promotes to staging by writing `apps/<app>/envs/astro-staging/version.yml` in `infra`.
2. Promote **production** in Semaphore for each of the 4 services (manual). This only writes the SHA into `apps/<app>/envs/astro/version.yml`; nothing starts yet because prod is excluded in step 3.
3. Apply the root app with **prod excluded**. In `infra/appsets/appset.yml` add under `generators[0].git.directories`:
   ```yaml
          - path: apps/*/envs/astro
            exclude: true
   ```

Prompt:
```
Repo: ../infra
1. Show me the Semaphore build status for the merge commit of the astro repo (sem CLI if connected) and the commits `deploy(<app>): astro-staging <sha>` and `deploy(<app>): astro <sha>` in infra's git log, for bot, api-central, support-bot, entitlements-expiration-job. Verify for each that the image ghcr.io/bot-astro/<service>:<sha> exists (docker manifest inspect / gh api).
2. Add the `exclude: true` entry for apps/*/envs/astro to appsets/appset.yml and show me the diff. After my OK commit, push, and run `kubectl apply -f root-argocd-app.yml` (context gport).
3. Confirm the created applications are exactly: registry-astro-staging, infisical-astro-staging, redis-staging-astro-staging, bot-astro-staging, api-central-astro-staging, support-bot-astro-staging, entitlements-expiration-job-astro-staging, and that NO *-astro (prod) application exists.
```

4. **Frontend**: merge `hyperion-rewrite` into `main` **only at D6**. For staging, Cloudflare builds `hyperion-rewrite` as is (F2).

## C. Test staging

Prompt:
```
Repo: astro
Check staging (kubectl context gport, namespace astro-staging) and report a pass/fail checklist:
- `kubectl get applications -n argocd | grep staging` all Synced/Healthy.
- Secrets exist: docker-registry, bot-config, api-central-config, support-bot-config, entitlements-expiration-job-config, redis-staging-config; `kubectl describe infisicalauth -n astro-staging` has no auth errors; InfisicalStaticSecrets are synced.
- Pods Ready: bot-0 (readiness takes ~2 min), api-central, support-bot-0, redis-staging; no CreateContainerConfigError/ImagePullBackOff/CrashLoopBackOff; show the last 100 log lines of bot-0 and api-central and flag errors.
- Certificates issued and https://api-staging.astro-bot.space/docs answers; the management ports (9100/9101/9102) are NOT exposed by any ingress.
- Sentry (use the Sentry MCP): events from staging with the release = deployed SHA.
- Prometheus (port-forward kube-prometheus-stack-prometheus 19090:9090): the services' OTLP metrics are present.
- BigQuery astro_staging.TEMPORARY_VC_GENERATION receives rows after I create a temp voice channel (I will do that, tell me when).
- Run the job once: `kubectl create job --from=cronjob/entitlements-expiration-job test-run -n astro-staging`, show its logs, then delete the job.
```

**[you]** Manual checks on staging:
- [ ] Staging bot online in a test guild; slash commands respond; a temporary voice channel is created and deleted.
- [ ] `https://staging.astro-bot.space` loads; language switch works (en-US/it); content pages and guides render.
- [ ] Sign in with Discord from the staging site: redirect to Discord → back to the staging site with a session cookie (`Domain=staging.astro-bot.space`, Secure, HttpOnly); refresh keeps the session; no CORS errors in the browser console.
- [ ] Dashboard lists the test guild, a setting change persists (check in MongoDB staging DB), an interface/template move or upload works (R2 `astro-media-staging`, images served from `cdn-staging.astro-bot.space`).
- [ ] Premium flow on the Chargebee **test** site (`astro-bot-test`): checkout opens from the frontend (Chargebee.js initialised with the test site), webhook `/v2/chargebee/events/subscription/create` returns 200 and the guild becomes premium; cancel flow likewise. (Set up the test webhook as in E1.)
- [ ] Robots: staging is not indexable.

Fix issues, redeploy staging, repeat. Do not go on to D until all pass.

## D. Production cutover (downtime starts at D2)

**D1. Go/no-go.** All of A, F, B and C done; backups of A7 exist; Infisical prod re-verified (A2 prompt); old Cloudflare deployment id noted. **[you]** Announce the downtime (support server, status).

**D2. Record and stop legacy.**

Prompt:
```
Repo: astro
Start of the production cutover. Do these in order and stop at any surprise:
1. Record the legacy state: `helm list -n astro` (expect releases bot, central-api, support-bot, entitlements-expiration-job) and `kubectl get all,ingress,pvc,cm,secret -n astro`. Confirm there are no PVCs in astro.
2. Show me the exact command to uninstall the 4 Helm releases. After I confirm, run `helm uninstall <r> -n astro` for bot, central-api, support-bot, entitlements-expiration-job. NEVER touch the namespaces astro, databases, longhorn-system or monitoring themselves.
3. List what remains in astro. Leftovers to delete only after showing me: secret docker-registry if not owned by a SealedSecret, gcp-bigquery-creds, central-api-tls, support-bot-tls.
4. Confirm no pod in astro is running anymore (legacy bots are down: no process uses the Discord token).
```

**D3. Data cleanups** (legacy bots stopped, new ones not started yet).

- MongoDB `AstroDB.errors`: legacy documents `{guildId, description, premiumRequired, guide, timestamp}` cannot be read by `ConfigurationErrorEntity` (`{guildId, code, args, timestamp}`, `code` required). Last check: 40,376 documents, 0 with `code`. They are regenerated when a config problem happens again. The `guildId_1` index stays.
- Redis `TEMP_VCS:<guildId>`: legacy JSON keys (`ownerID`, `genID`, `position`) cannot be read by `TemporaryVCEntity`. Those channels stay on Discord untracked (never auto-deleted): accepted.

Prompt:
```
Repo: astro
Legacy bots are stopped and the new prod ones are not started. Clean the legacy data, one command at a time, showing me the count first and waiting for my OK before each delete:
1. MongoDB prod (MONGODB_URI from `infisical export --env=prod`, never print it): count total and legacy documents of collection `errors` with `{code: {$exists: false}}`, then deleteMany for that filter only, then confirm the count of remaining legacy documents is 0 and the guildId_1 index still exists.
2. Redis (namespace databases, pod redis-master-0): count `TEMP_VCS:*` keys via --scan, then UNLINK them, then confirm 0 remain. If $REDIS_PASSWORD is not in the pod, take it from secret rediscreds in databases.
Do not touch any other collection or key.
```

**D4. Start the new backend.**

Prompt:
```
Repo: ../infra
Remove the `exclude: true` entry for apps/*/envs/astro from appsets/appset.yml, show me the diff, and after my OK commit and push. Then force an appset refresh: `kubectl annotate applicationset astro-appset -n argocd argocd.argoproj.io/application-set-refresh=true`. Watch (kubectl context gport): applications *-astro Synced/Healthy; pods, infisicalstaticsecrets, certificates and ingresses in namespace astro. The 11 bot pods start in parallel and take 2+ minutes each to log in their shards. Report when `kubectl get sts bot -n astro` is 11/11 and list any pod in error with its logs.
```

**D5. Verify the backend.**

Prompt:
```
Repo: astro
Verify production and report pass/fail:
- sts bot 11/11 Ready; Prometheus (port-forward) shows all 176 shards connected; Sentry (MCP) has no new unresolved errors since the start, release = deployed SHA, environment prod.
- https://api.astro-bot.space/docs answers over valid TLS; management ports are not exposed publicly; api-central, support-bot Ready; cronjob entitlements-expiration-job exists with schedule 0 1 * * *.
- bot pods use the new Redis keys (REDIS_HOST = redis-master.databases...), no read/deserialisation errors in logs or Sentry, MongoDB `errors` contains only documents with `code`.
- BigQuery astro_bot.TEMPORARY_VC_GENERATION has rows newer than the cutover time.
```

**[you]** Real-guild checks: a slash command, create and delete a temp voice channel, premium role change through the support bot.

**D6. Frontend goes live.**

1. Merge `hyperion-rewrite` into `main` in `astro-bot.space` (Crowdin: `l10n_*` branch already merged).
2. In the Cloudflare project make sure the production environment has the prod build variables (F2), build, and verify on the project URL.
3. Attach the custom domain `astro-bot.space` (and `www` if used) to the new project, removing it from the old one if applicable. Cloudflare DNS manages it.

Prompt:
```
Repo: astro-bot.space
Verify the production frontend after deploy (use the Claude in Chrome tools on https://astro-bot.space, no destructive actions): home page and a guide render, language switch, no console errors, network calls go to https://api.astro-bot.space, no CORS errors, `Set-Cookie` of the session has Domain=astro-bot.space; sitemap.xml and robots.txt are right for production (indexable); redirects from app/constants/redirects work. Tell me what to click for the Discord login myself, and I will tell you the result.
```

**[you]** Real login on `https://astro-bot.space`, dashboard shows your guilds, change and save a setting.

**D7.** Announce the end of the downtime.

### Rollback

- **Frontend**: Cloudflare → Deployments → rollback to the noted old deployment (instant), or re-attach the domain to the old project. Old frontend only works with the old API.
- **Backend, before any data deletion (D3)**: add the `exclude` back in the appset and push, `kubectl delete application -n argocd bot-astro api-central-astro support-bot-astro entitlements-expiration-job-astro`, reinstall the legacy releases from the pre-merge `master` charts (`git show <commit-before-merge>:services/<svc>/chart`) with the values saved in A7. Legacy image `ghcr.io/bot-astro/bot:243fb4da98e18ae2ebc587575dcc75e0cc745b5b` is still in ghcr.
- **After D3**: legacy `errors` documents and `TEMP_VCS:*` keys are gone (restore from the A7 backups if really needed). MongoDB and Redis are shared, data written by the new version stays.

## E. After the cutover

**E1. Chargebee webhooks [you]** (Settings → Configure Chargebee → Webhooks; the new API is live now). Replace the legacy `central-api` webhooks. Basic auth on each: username `chargebee`, password = `CA__CHARGEBEE_WEBHOOK_TOKEN` of the matching Infisical env.

| Site | Event | URL |
|---|---|---|
| live (`astro-bot`) | Subscription created | `https://api.astro-bot.space/v2/chargebee/events/subscription/create` |
| live | Subscription cancelled | `https://api.astro-bot.space/v2/chargebee/events/subscription/cancel` |
| test (`astro-bot-test`) | Subscription created | `https://api-staging.astro-bot.space/v2/chargebee/events/subscription/create` |
| test | Subscription cancelled | `https://api-staging.astro-bot.space/v2/chargebee/events/subscription/cancel` |

Send a sample event (expect 200). Chargebee retries failed deliveries, but check the event log for failures during the downtime and replay them.

**E2. Security cleanup**

Prompt:
```
Repo: astro
Prepare the post-cutover cleanup and show me each step before doing it:
1. Rotate SB__API_AUTH and SUPPORT_BOT_ENDPOINT_AUTH (same new value, in prod and staging), CA__AUTH, and any leftover of KUBE_LIFECYCLE_AUTH_KEY (the value committed in services/api-central/chart/values.yaml on the old branch). Generate with `openssl rand -base64 48`, give me the `infisical secrets set` commands; the operator syncs and the pods auto-reload, then confirm the pods restarted healthy.
2. Find gcp-bigquery-creds.json in this repo and in git history, tell me how to remove it from git (git rm + history decision) and remind me to revoke the old service account key in GCP (I do it in the console).
3. List Semaphore secrets and Infisical keys no longer used by any manifest or application.yml.
```

**[you]**: remove the old Discord OAuth redirects if they differ, delete unused Semaphore secrets, check the old frontend project and old R2 bucket are removed.

**E3.** Later and separately: JVM memory work (`docs/memory-improvement-plan.md`; pod right-sizing then allows `min_nodes = 2`, see `docs/infrastructure.md` step 2). The `cpx → cx` node migration (S) is deferred until `cx` stock returns in nbg1: do it after this step. Update `CLAUDE.md` steps: mark "deploy" done.
