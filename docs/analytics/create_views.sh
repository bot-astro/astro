#!/usr/bin/env bash
# Creates or updates one BigQuery view per file of queries/, named ANALYTICS_<FILE IN UPPERCASE>, in the dataset of the queries.
# Usage: docs/analytics/create_views.sh [gcp-project]   (default: astro-405113)
set -euo pipefail

project="${1:-astro-405113}"
cd "$(dirname "$0")/queries"

for file in *.sql; do
  view="ANALYTICS_$(basename "$file" .sql | tr '[:lower:]' '[:upper:]')"
  bq --project_id="$project" query --use_legacy_sql=false --quiet "CREATE OR REPLACE VIEW \`astro_bot.$view\` AS $(cat "$file")"
  echo "created astro_bot.$view"
done
