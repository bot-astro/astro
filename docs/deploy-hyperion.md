# Deploying Hyperion (new bot, new infra)

Replaces the legacy deployment (Helm releases in the `astro` namespace, `master` branch charts, ConfigMaps) with the Hyperion services deployed by ArgoCD from `bot-astro/infra` and configured from Infisical. Downtime of the bot is accepted. Nothing in this guide touches the cluster itself (`gport/hcloud-cluster`), Longhorn, Redis data or MongoDB data.

Repos: `astro` (this), `infra` (`../infra`), `gport` (`../../Giuliopime/gport`). Cluster context: `gport`. Prod namespace `astro`, staging `astro-staging`.

## 0. What changes, and the order

| | Legacy | Hyperion |
|---|---|---|
| Deploy | `helm install` from `master` charts | ArgoCD appset on `bot-astro/infra` (`apps/*/envs/*`, namespace = env folder) |
| Config | ConfigMaps (`bot-config`, `central-api-config`, ...) | Infisical (`prod`, `staging`) synced to Secrets by the Infisical operator |
| Names | `central-api`, ingress `support-bot-ing` | `api-central`, ingress `support-bot` |
| Pull secret | `docker-registry` (Helm) | `docker-registry` (SealedSecret, app `registry`) |
| BigQuery key | secret `gcp-bigquery-creds` | `BOT__GCP_CREDENTIALS_JSON` in Infisical, mounted from `bot-config` |
| Probes | custom `/ready`, `KUBE_LIFECYCLE_AUTH_KEY` | actuator on separate management ports 9100/9101/9102 |
| Metrics | none in Prometheus | OTLP push to Prometheus |

Order: **(A) prepare while the old version keeps running** → **(B) merge to master, images get built, staging comes up** → **(C) test on staging** → **(D) prod cutover (downtime)** → **(E) after cutover**.

Danger to keep in mind: the moment the appset sees `apps/*/envs/astro`, ArgoCD starts the new prod bots. If the old bots are still running, two processes use the same Discord token and the same Redis keys. That is why section B/C keeps prod excluded and section D stops the old ones first.

## A. Preparation (no downtime)

### A1. gport

Two changes in `gport`, both applied by ArgoCD from `main`:

1. `k8s-resources/kube-prometheus-stack/values.yaml`: `enableOTLPReceiver: true` (uncommitted today). Without it the services cannot push metrics.
2. `argocd/apps/infisical-operator.yaml` (new): installs the Infisical operator (`secrets-operator` chart 0.11.15) in `infisical-operator-system`.

```sh
cd ../../Giuliopime/gport
git add argocd/apps/infisical-operator.yaml k8s-resources/kube-prometheus-stack/values.yaml
git commit -m "feat: infisical operator, prometheus otlp receiver" && git push
```

Do **not** commit/apply `hcloud-cluster/kube-hetzner.tf` or `hcloud-cluster/docs/cx-migration.md` as part of this: node replacement and the app migration should not happen together (the cx migration drains nodes holding Longhorn volumes with Redis and Prometheus data).

Verify:
```sh
kubectl get applications -n argocd                       # infisical-operator and kube-prometheus-stack Synced/Healthy
kubectl get crd | grep infisical                          # infisicalconnections, infisicalauths, infisicalstaticsecrets (secrets.infisical.com)
kubectl get pods -n infisical-operator-system
kubectl get pods -n monitoring | grep prometheus          # restarted once for the OTLP flag (Longhorn PVC 10Gi stays)
```

Longhorn check (read only), all volumes should be `healthy`/`attached`:
```sh
kubectl get volumes.longhorn.io -n longhorn-system
```

### A2. Infisical

Project `b9071f19-b984-4138-a1e8-a15c67e02603`, environments `prod` and `staging`.

**1. Machine identity for the operator (Kubernetes Auth)**

1. Organization → Access Control → Machine Identities → create `astro-k8s-operator`. Add it to the project with a role that can read secrets in `prod` and `staging` (Viewer is enough).
2. Authentication → add **Kubernetes Auth**:
   - Kubernetes host/API URL: the cluster API URL (gport creates a Cloudflare record for it; Infisical Cloud must be able to reach it on 6443, check the control plane load balancer/firewall in `hcloud-cluster/kube-hetzner.tf`).
   - Token reviewer JWT: the token of a service account that has `system:auth-delegator`, e.g.
     ```sh
     kubectl create serviceaccount infisical-token-reviewer -n kube-system
     kubectl create clusterrolebinding infisical-token-reviewer --clusterrole=system:auth-delegator --serviceaccount=kube-system:infisical-token-reviewer
     kubectl create token infisical-token-reviewer -n kube-system --duration=8760h
     ```
     (a long-lived token Secret of type `kubernetes.io/service-account-token` is the alternative if you want it not to expire).
   - CA certificate: from the kubeconfig (`certificate-authority-data`, base64 decoded).
   - Allowed service account names: `infisical-service-account`. Allowed namespaces: `astro,astro-staging`.
3. Copy the **Identity ID**. It goes in the sealed secret in A5 (it is the same for both namespaces, or create one identity per env if you prefer to scope them).

**2. Machine identity for Terraform (Universal Auth)**

Create `astro-terraform` with write access to the project (secrets in both envs). Copy client id/secret for `terraform.tfvars` (A3).

**3. Secrets**

Compare against what the code reads. Last check (keys only, no values):

*`prod`: 59 keys set, add these:*

| Key | Value |
|---|---|
| `SUPPORT_BOT_ENDPOINT_BASE_URL` | `http://support-bot.astro:9002` |
| `SUPPORT_BOT_ENDPOINT_AUTH` | same value as `SB__API_AUTH` |
| `CA__CHARGEBEE_WEBHOOK_TOKEN` | basic auth password you will configure in Chargebee (E2). The old one was `chargebee.webhookToken` in the old config |
| `CA__DISCORD_APPLICATION_ID`, `CA__DISCORD_APPLICATION_TOKEN` | same app/token as `BOT__DISCORD_APPLICATION_*` (used to refresh guild entitlements from Discord) |
| `PREMIUM_SERVER_SKU_ID`, `PREMIUM_MONTHLY_PLAN_ID`, `PREMIUM_YEARLY_PLAN_ID` | see `docs/premium-env-vars.md` (not set = nobody is premium) |

Also check these in `prod`:
- `DISCORD_OAUTH_REDIRECT_URI=https://api.astro-bot.space/v2/auth/discord/callback` and that URL is registered in the Discord developer portal (OAuth2 → Redirects). The old redirect keeps working until you remove it.
- `BOT_SHARD_MANAGER_TOTAL_PODS=11`, `BOT_SHARD_MANAGER_TOTAL_SHARDS=176` (match the 11 replicas in `apps/bot/envs/astro/replicas.yml`).
- Ports: `BOT_API_PORT=9000`, `CA__PORT=9001`, `SB__PORT=9002`.
- `REDIS_HOST=redis-master.databases.svc.cluster.local`, `REDIS_PORT=6379`, `REDIS_DATABASE=0`, `REDIS_PASSWORD` (same as secret `rediscreds` in `databases`).
- `MONGODB_URI` is the Atlas string. `R2_*` and the BigQuery vars are written by Terraform (A3).
- Do not set `BOT__POD_HOSTNAME`/`BOT__POD_ORDINAL` in prod/staging, the StatefulSet injects them (the ordinal is the pod name, e.g. `bot-3`).

*`staging`: 39 keys set, add these:*

| Key | Value |
|---|---|
| `BOT__DISCORD_APPLICATION_ID`, `BOT__DISCORD_APPLICATION_TOKEN` | a **separate** Discord application for staging (never reuse the prod token) |
| `EEJ__DISCORD_APPLICATION_ID`, `EEJ__DISCORD_APPLICATION_TOKEN`, `CA__DISCORD_APPLICATION_ID`, `CA__DISCORD_APPLICATION_TOKEN` | same app/token as above (or its own) |
| `DISCORD_OAUTH_ID`, `DISCORD_OAUTH_SECRET` | OAuth2 of the staging application; redirect `https://api-staging.astro-bot.space/v2/auth/discord/callback` |
| `DISCORD_OAUTH_REDIRECT_URI` | the staging redirect above |
| `CA__AUTH`, `SB__API_AUTH`, `SUPPORT_BOT_ENDPOINT_AUTH` | random strings (`openssl rand -base64 48`); `SUPPORT_BOT_ENDPOINT_AUTH` = `SB__API_AUTH` |
| `SUPPORT_BOT_ENDPOINT_BASE_URL` | `http://support-bot.astro-staging:9002` |
| `CA__SESSION_COOKIE_ALLOW_ORIGIN` | staging frontend origin |
| `CA__CHARGEBEE_WEBHOOK_TOKEN`, `CHARGEBEE_API_KEY` | Chargebee test site (`astro-bot-test`) |
| `MONGODB_URI` | a separate database for staging (new database name in the same Atlas cluster is enough, user with access to only that database) |
| `REDIS_HOST` | `redis-staging` |
| `REDIS_PORT` / `REDIS_DATABASE` | `6379` / `0` |
| `REDIS_PASSWORD` | random (the staging Redis in `apps/redis-staging` reads it from here) |
| `SB__DISCORD_APPLICATION_TOKEN`, `SB__SERVER_GUILD_ID`, `SB__SERVER_PREMIUM_ROLE_ID`, `SB__SERVER_PREMIUM_SKU_ID`, `SB__SERVER_ENTITLEMENTS_APPLICATION_ID`, `SB__SERVER_ENTITLEMENTS_APPLICATION_TOKEN` | a staging support server and application |
| `PREMIUM_RESTRICTIONS` | `false` to treat every guild as premium on staging |
| `BOT__BIGQUERY_DATASET_NAME`, `BOT__GCP_CREDENTIALS_JSON`, `R2_*` | written by Terraform (A3), do not add by hand (if they already exist, delete or import them first, see `terraform/README.md`) |

Staging already has `BOT_SHARD_MANAGER_TOTAL_PODS=1`, `TOTAL_SHARDS=1`.

### A3. Terraform (`infra/terraform`)

Details in `terraform/README.md`. Prod and staging are both declared. Summary:

1. By hand once: create the R2 bucket `astro-terraform-state` and an R2 API token for it. `cp backend.hcl.template backend.hcl`, fill the account id, `export AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=...`.
2. Delete in Infisical, in both `prod` and `staging`, the keys terraform will create (it fails with "already exists"): `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`, `R2_ENDPOINT_URL`, `BOT__BIGQUERY_DATASET_NAME`, `BOT__GCP_CREDENTIALS_JSON`. Nothing running reads them from Infisical yet (the legacy bot uses ConfigMaps). Copy `R2_*` and the dataset name aside first if you want to be able to roll back.
3. `cloudflare/` (buckets `astro-media-prod` and `astro-media-staging`, `cdn.astro-bot.space` and `cdn-staging.astro-bot.space`, one token per env, DNS for `api`, `support-bot` and the two staging hosts → ingress IP; `ingress_ipv4` from gport's `module.kube-hetzner.ingress_public_ipv4`):
   - The old prod bucket `astro-media` is empty: verify it (Cloudflare dashboard → R2), then delete it. This frees `cdn.astro-bot.space`, a custom domain can be attached to one bucket only. Also delete its old API token.
   - The prod DNS records `api` and `support-bot` already exist: import them (commands in `terraform/README.md`) so they are not recreated.
4. `gcp/` (project `astro-405113`): `imports.tf` adopts the existing prod dataset `astro_bot` and its 5 tables (`CONNECTION_INVOCATION`, `GUILD_EVENT`, `GUILD_SNAPSHOT`, `SLASH_COMMAND_INVOCATION`, `TEMPORARY_VC_GENERATION`) without touching the data; tables have `deletion_protection` and `prevent_destroy`, so a mistake fails instead of dropping data. It also creates the service accounts (`astro-bot-prod`, `astro-bot-staging`), `astro_staging`, and writes `BOT__GCP_CREDENTIALS_JSON`. Revoke the old key (`gcp-bigquery-creds` secret, committed `gcp-bigquery-creds.json`) once the new version runs, and remove the file from git.
5. In each module: `terraform init -backend-config=../backend.hcl && terraform plan`. **Read the plan** before applying:
   - `gcp/`: the dataset and 5 prod tables are only `import`ed (at most in-place updates, no schema change); there must be **no** `destroy` or `replace` on any BigQuery resource.
   - `cloudflare/`: no `destroy`; the imported DNS records may show `update in-place` (proxy/ttl), that is fine.
   - After the first successful apply delete `gcp/imports.tf`.

### A4. Semaphore and GitHub

1. Semaphore secrets (check with `sem get secrets` after `sem connect`):
   - `github`: env `GHCR_ORGANIZATION=bot-astro`, `GITHUB_ACTOR`, `GITHUB_TOKEN` (token with `write:packages`).
   - `sentry`: env `SENTRY_AUTH_TOKEN`.
   - `infra-deploy-key`: file `/home/semaphore/.ssh/infra_deploy_key` with the **private** key. Create the pair: `ssh-keygen -t ed25519 -N "" -f infra_deploy_key`, add `infra_deploy_key.pub` to `bot-astro/infra` → Settings → Deploy keys with **write access**.
2. The pipeline only builds and auto-promotes on **`master`**. Production promotion is manual. Promote a prod deploy only for a commit whose build for that service actually ran (the image tag is the git SHA; if the service block was skipped because nothing changed, the tag does not exist).

### A5. Infra repo: secrets and ArgoCD access

1. `docker-registry` SealedSecrets are already generated for both namespaces (`apps/registry/envs/*/docker-config-secret-sealed.yml`). To regenerate, see the instructions in `apps/registry/envs/<env>/secrets/docker-config-secret.template.yml`.
2. Seal the Infisical identity id for both namespaces (instructions inside `apps/infisical/envs/<env>/secrets/infisical-kubernetes-credentials-secret.template.yml`), then add the sealed file to `resources:` in `apps/infisical/envs/<env>/kustomization.yml`:
   ```sh
   cd ../infra
   cp apps/infisical/envs/astro/secrets/infisical-kubernetes-credentials-secret.template.yml apps/infisical/envs/astro/secrets/infisical-kubernetes-credentials-secret.yml   # then set identityId
   cp apps/infisical/envs/astro-staging/secrets/infisical-kubernetes-credentials-secret.template.yml apps/infisical/envs/astro-staging/secrets/infisical-kubernetes-credentials-secret.yml
   scripts/seal-secrets.sh infisical
   ```
3. `kustomize build` every overlay (`for d in apps/*/envs/*; do kustomize build $d > /dev/null || echo FAIL $d; done`), then commit and push `infra`. Nothing deploys yet, the root app is not applied.
4. ArgoCD must be able to read `bot-astro/infra`. If the repo is private, add credentials in ArgoCD (Settings → Repositories, HTTPS + token with `repo:read`, or add a deploy key; this belongs in `gport/argocd-installation` as a sealed secret so it survives reinstalls).

### A6. External services

- **Discord developer portal** (prod application): add the new OAuth2 redirect (A2).
- **Chargebee**: the new webhook URLs are set in E2 (they depend on the new API being up).
- **Cloudflare**: DNS and R2 are managed by Terraform (A3).

### A7. Backups (before the cutover)

| What | How |
|---|---|
| MongoDB | Atlas → Backup → take an on-demand snapshot. The only change the migration makes is deleting the legacy `errors` documents (D4) |
| Redis | `kubectl exec -n databases redis-master-0 -- redis-cli -a "$(kubectl get secret rediscreds -n databases -o jsonpath='{.data.password}' \| base64 -d)" BGSAVE`, wait for `LASTSAVE` to change, then `kubectl cp databases/redis-master-0:/data/dump.rdb ./redis-backup.rdb` (see `redis-migration.md` for the layout). Redis is **not** modified by the migration |
| Longhorn | Longhorn UI → Volume → Create Backup/Snapshot for the Redis and Prometheus volumes |
| Old config | `kubectl get cm,secret,ingress,sts,deploy,cronjob,svc -n astro -o yaml > astro-legacy-backup.yaml` (contains secrets, keep it out of git). Also `helm get values -n astro <release>` for `bot`, `central-api`, `support-bot`, `entitlements-expiration-job` |
| BigQuery | Adopted by Terraform with `imports.tf`, data not modified. Take a copy before the first apply anyway: `bq cp` each of the 5 tables of `astro_bot` to a backup dataset (or a table snapshot) |

## B. Merge, build, staging

1. Merge `version/hyperion` into `master` (the legacy charts under `services/*/chart` on master are replaced). Semaphore builds every service whose paths changed and pushes images tagged `<sha>`, `<version>` (`6.0.0` bot and api-central, `3.0.0` support-bot and expiration job) and `latest` to `ghcr.io/bot-astro/<service>`.
2. Each build auto-promotes to staging: `scripts/deploy.sh` clones `infra`, sets the image of `apps/<app>/envs/astro-staging/version.yml` to the SHA, and pushes. Check the commits `deploy(<app>): astro-staging <sha>` in `infra`.
3. Promote production in Semaphore for each service (manual promotion). This only writes the SHA into `apps/<app>/envs/astro/version.yml` of `infra`; nothing starts yet because ArgoCD does not know about the prod folders (step 4).
4. Apply the root app **with prod excluded**. Edit `appsets/appset.yml` in `infra` and add under `generators[0].git.directories`:
   ```yaml
          - path: apps/*/envs/astro
            exclude: true
   ```
   commit, push, then:
   ```sh
   kubectl apply -f root-argocd-app.yml
   ```
   Apps created: `registry-astro-staging`, `infisical-astro-staging`, `redis-staging-astro-staging`, `bot-astro-staging`, `api-central-astro-staging`, `support-bot-astro-staging`, `entitlements-expiration-job-astro-staging`.

## C. Test staging

```sh
kubectl get applications -n argocd | grep staging
kubectl get pods,infisicalstaticsecrets -n astro-staging
kubectl describe infisicalauth infisical-auth -n astro-staging      # auth errors show up here
kubectl logs -n astro-staging bot-0 --tail=100
```

Checklist:
- [ ] `docker-registry`, `bot-config`, `api-central-config`, `support-bot-config`, `entitlements-expiration-job-config`, `redis-staging-config` Secrets exist; pods leave `CreateContainerConfigError`/`ImagePullBackOff`.
- [ ] `bot-0`, `api-central`, `support-bot-0` Ready (readiness takes ~2 min for the bot); `redis-staging` Running.
- [ ] Ingress certificates issued: `kubectl get certificate -n astro-staging`; `https://api-staging.astro-bot.space` answers.
- [ ] Staging bot is online in a test guild; create a temporary voice channel.
- [ ] Sentry receives events (release = SHA); Prometheus has the OTLP metrics (port-forward `kube-prometheus-stack-prometheus` and query e.g. `up` / the app meters).
- [ ] BigQuery `astro_staging.TEMPORARY_VC_GENERATION` gets rows (`bq query` with a partition filter).
- [ ] Dashboard login via Discord OAuth works against the staging API, Chargebee test webhooks reach `/v2/chargebee/events/subscription/*`.
- [ ] Trigger the job: `kubectl create job --from=cronjob/entitlements-expiration-job test-run -n astro-staging`.

## D. Production cutover (downtime starts here)

1. Announce the downtime. Make sure A1–A7 and B3 are done and staging is healthy.
2. Record the legacy state: `helm list -n astro` (expect releases `bot`, `central-api`, `support-bot`, `entitlements-expiration-job`), `kubectl get all,ingress,pvc -n astro`. There are no PVCs in `astro`.
3. Stop the legacy workloads by uninstalling the Helm releases (this removes their Services, StatefulSets, Deployments, CronJob, Ingresses and the ConfigMaps/Secrets they own):
   ```sh
   for r in bot central-api support-bot entitlements-expiration-job; do helm uninstall $r -n astro; done
   kubectl get all,ingress,cm,secret -n astro        # only kube-root-ca.crt and leftovers not owned by Helm remain
   ```
   If `gcp-bigquery-creds`, `docker-registry` or the `central-api-tls`/`support-bot-tls` secrets remain, delete them: `docker-registry` is recreated by the SealedSecret (if a non-Helm `docker-registry` is still there, the SealedSecret controller will refuse to overwrite it: delete it), the TLS secrets are reissued under the new names `api-central-tls`/`support-bot-tls`.
   Do **not** delete the `astro` namespace, the `databases` namespace or anything in `longhorn-system`/`monitoring`.
4. **Clear the legacy configuration error documents** (MongoDB `AstroDB`, collection `errors`). The legacy bot stored `{guildId, description, premiumRequired, guide, timestamp}`; the new entity (`ConfigurationErrorEntity`) stores `{guildId, code, args, timestamp}` with a required `code` enum, so legacy documents cannot be read by the new version (last check: 40,376 documents, 0 with `code`). Do it now, with the legacy bots stopped (step 3) and the new ones not started (step 5 starts them), so nothing writes in between. The errors are regenerated by the bot when a configuration problem happens again, nothing else depends on them. The `guildId_1` index is the same in both versions and stays.
   ```sh
   cd ../astro && URI=$(infisical export --env=prod --format=json | jq -r '.[]|select(.key=="MONGODB_URI")|.value')
   mongosh "$URI" --quiet --eval 'printjson({total: db.errors.countDocuments({}), legacy: db.errors.countDocuments({code: {$exists: false}})})'
   mongosh "$URI" --quiet --eval 'printjson(db.errors.deleteMany({code: {$exists: false}}))'     # deletes only the legacy shape
   mongosh "$URI" --quiet --eval 'printjson(db.errors.countDocuments({}))'                       # expect 0
   ```
   Do the same on the staging database if it was ever filled with legacy data (`MONGODB_URI` of `staging`).
5. In `infra`, remove the `exclude` entry added in B4, commit and push. ArgoCD creates the `*-astro` apps (the appset refreshes within ~3 minutes, or force it: `kubectl annotate applicationset astro-appset -n argocd argocd.argoproj.io/application-set-refresh=true`).
6. Watch it come up:
   ```sh
   kubectl get applications -n argocd | grep -- '-astro '
   kubectl get pods,infisicalstaticsecrets -n astro -w
   kubectl get certificate,ingress -n astro
   ```
   The 11 bot pods start in parallel; each takes 2+ minutes to log in its 16 shards. Readiness gates at 120s + probes.
7. Verify:
   - [ ] `kubectl get sts bot -n astro` shows 11/11; shards connected (Prometheus `pod_id`/shard gauges, Sentry).
   - [ ] `https://api.astro-bot.space` and the dashboard work, Discord login uses the new redirect.
   - [ ] Support bot Ready and premium role changes work.
   - [ ] A temporary voice channel is created and cleaned up in a real guild; slash commands respond.
   - [ ] BigQuery `astro_bot.TEMPORARY_VC_GENERATION` receives new rows (`SELECT COUNT(*) ... WHERE timestamp > TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 HOUR)`).
   - [ ] Redis is used by the new pods (`REDIS_HOST` points at `databases`), no errors in Sentry.
   - [ ] `db.errors` only contains documents with a `code` (no read errors in Sentry when a guild opens its configuration errors).
   - [ ] The expiration job schedule `0 1 * * *` shows in `kubectl get cronjob -n astro`.
8. Announce the end of the downtime.

### Rollback

Before any data migration step, rollback is: stop ArgoCD from running prod (add the `exclude` back and push, then `kubectl delete application -n argocd bot-astro api-central-astro support-bot-astro entitlements-expiration-job-astro`), then reinstall the legacy releases from the `master` charts before the merge (`git show <commit-before-merge>:services/<svc>/chart`) with `helm install` and the values saved in A7. The legacy image `ghcr.io/bot-astro/bot:243fb4da98e18ae2ebc587575dcc75e0cc745b5b` is still in ghcr. Redis and MongoDB are shared, so data written by the new version stays.

## E. After the cutover

1. **Chargebee webhooks** (the new API is live now, so they can be validated). In Chargebee → Settings → Configure Chargebee → Webhooks (do it on the **live** site, and on the **test** site `astro-bot-test` for staging), replace the old webhooks (the legacy `central-api` URLs) with:

   | Site | Event | URL |
   |---|---|---|
   | live | Subscription created | `https://api.astro-bot.space/v2/chargebee/events/subscription/create` |
   | live | Subscription cancelled | `https://api.astro-bot.space/v2/chargebee/events/subscription/cancel` |
   | test | Subscription created | `https://api-staging.astro-bot.space/v2/chargebee/events/subscription/create` |
   | test | Subscription cancelled | `https://api-staging.astro-bot.space/v2/chargebee/events/subscription/cancel` |

   - Enable basic authentication on each webhook: username `chargebee`, password = `CA__CHARGEBEE_WEBHOOK_TOKEN` of the matching Infisical environment (A2).
   - The same plan ids must be in Infisical: `PREMIUM_MONTHLY_PLAN_ID`, `PREMIUM_YEARLY_PLAN_ID`, `CHARGEBEE_SERVER_PREMIUM_PLAN_ID`, `CHARGEBEE_SITE_NAME`, `CHARGEBEE_API_KEY` (see `docs/premium-env-vars.md`).
   - Verify: Chargebee → the webhook → "Test webhook"/send a sample event, expect `200`. For a real check subscribe/cancel on the test site with a staging guild and watch `kubectl logs -n astro-staging deploy/api-central` and the premium role changing. Chargebee retries failed events, so events sent during the downtime are redelivered once the API is up; check the Chargebee event log for failed deliveries in the window and replay them.
2. Security cleanup:
   - Rotate `SUPPORT_BOT_AUTH`/`SB__API_AUTH` (the old value was readable in the legacy ConfigMap) and `KUBE_LIFECYCLE_AUTH_KEY` leftovers (the value committed in `services/api-central/chart/values.yaml` on the old branch).
   - Revoke the old BigQuery key (`gcp-bigquery-creds.json`) and remove the file from git.
3. Delete leftovers: Helm release secrets (`sh.helm.release.v1.*`) disappear with `helm uninstall`; old ghcr tags can stay.
4. Remove the old Discord OAuth redirect if it differs, and any Semaphore secrets that are no longer used.
5. Later, separately: the `cpx → cx` node migration (`gport/hcloud-cluster/docs/cx-migration.md`) and the JVM memory work (`docs/memory-improvement-plan.md`).
