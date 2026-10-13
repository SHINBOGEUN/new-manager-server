#!/usr/bin/env bash
set -Eeuo pipefail

# Runs on the deployment host. The production Compose file and .env stay on the host.
app_dir=/dcim_new/backend
release_id=${1:?release ID is required}
[[ "$release_id" =~ ^[0-9]+-[0-9]+$ ]] || { echo 'Invalid release ID' >&2; exit 1; }

cd "$app_dir"
exec 9> .cd-deploy.lock
flock -n 9 || { echo 'Another app deployment is running' >&2; exit 1; }

command -v docker >/dev/null
command -v curl >/dev/null
[[ -f docker-compose.yml && -f .env ]] || { echo 'App Compose or .env is missing' >&2; exit 1; }
docker info >/dev/null

jars=(
  new-manager-server-1.0.0.jar
  new-collector-server-1.0.0.jar
  new-sensor-data-server-1.0.0.jar
)
bundled_jars=(manager.jar collector.jar sensor-data.jar)
services=(manager-server collector-service sensor-data-service)
containers=(dcim_new_manager_server dcim_new_collector_service dcim_new_sensor_data_service)
project=
for index in "${!services[@]}"; do
  running_project=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "${containers[$index]}")
  running_service=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.service"}}' "${containers[$index]}")
  [[ "$running_project" =~ ^[A-Za-z0-9][A-Za-z0-9_-]*$ && "$running_service" == "${services[$index]}" ]] || {
    echo "Unexpected Compose labels on ${containers[$index]}" >&2
    exit 1
  }
  if [[ -z "$project" ]]; then project=$running_project; fi
  [[ "$running_project" == "$project" ]] || { echo 'App containers have different Compose projects' >&2; exit 1; }
  mounted_jar=$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/opt/dcim/app.jar"}}{{.Source}}{{end}}{{end}}' "${containers[$index]}")
  [[ "$mounted_jar" == "$app_dir/${jars[$index]}" ]] || {
    echo "Unexpected JAR mount for ${containers[$index]}" >&2
    exit 1
  }
done
[[ "$project" == dcim-new-backend ]] || { echo "Refusing unexpected Compose project: $project" >&2; exit 1; }
compose=(docker compose -p "$project" --env-file .env -f docker-compose.yml)
"${compose[@]}" config --services >/dev/null
for index in "${!services[@]}"; do
  compose_id=$("${compose[@]}" ps -q "${services[$index]}")
  [[ -n "$compose_id" && "$(docker inspect -f '{{.Id}}' "$compose_id")" == "$(docker inspect -f '{{.Id}}' "${containers[$index]}")" ]] || {
    echo "Compose does not own ${containers[$index]}" >&2
    exit 1
  }
done

incoming="$app_dir/.cd-incoming/$release_id"
release="$app_dir/.cd-releases/$release_id"
[[ -d "$incoming" && ! -e "$release" ]] || { echo 'Release is missing or already deployed' >&2; exit 1; }
for index in "${!jars[@]}"; do
  [[ -s "$incoming/${bundled_jars[$index]}" && -f "$app_dir/${jars[$index]}" ]] || {
    echo "Missing JAR: ${jars[$index]}" >&2
    exit 1
  }
done
[[ -s "$incoming/revisions.txt" ]] || { echo 'Missing revision manifest' >&2; exit 1; }
grep -Fxq 'version=1.0.1' "$incoming/revisions.txt" || { echo 'Unexpected bundle version' >&2; exit 1; }

mkdir -p "$app_dir/.cd-releases"
mv "$incoming" "$release"
mkdir "$release/previous"
for jar in "${jars[@]}"; do
  cp -p "$app_dir/$jar" "$release/previous/$jar"
done

updated=0
rollback() {
  local original_status=$?
  trap - ERR HUP INT TERM
  (( original_status != 0 )) || original_status=1
  if (( updated )); then
    echo 'Deployment failed; restoring the previous three JARs' >&2
    for jar in "${jars[@]}"; do
      if ! { install -m 0644 "$release/previous/$jar" "$app_dir/.$jar.rollback" &&
             mv -f "$app_dir/.$jar.rollback" "$app_dir/$jar"; }; then
        echo "Rollback copy failed for $jar" >&2
      fi
    done
    if ! "${compose[@]}" up -d --no-deps --force-recreate \
      manager-server collector-service sensor-data-service; then
      echo 'Rollback container recreation failed; manual intervention required' >&2
    fi
  fi
  exit "$original_status"
}
trap rollback ERR
trap rollback HUP INT TERM

updated=1
for index in "${!jars[@]}"; do
  install -m 0644 "$release/${bundled_jars[$index]}" "$app_dir/.${jars[$index]}.deploy"
  mv -f "$app_dir/.${jars[$index]}.deploy" "$app_dir/${jars[$index]}"
done

# Recreate is necessary: bind mounts may still point at the old inode after atomic rename.
"${compose[@]}" up -d --no-deps --force-recreate \
  manager-server collector-service sensor-data-service

wait_http() {
  local name=$1 url=$2 attempt
  for attempt in {1..24}; do
    if curl --fail --silent --max-time 5 --output /dev/null "$url"; then
      echo "$name is responding"
      return 0
    fi
    sleep 5
  done
  echo "$name did not become ready: $url" >&2
  return 1
}

wait_http manager http://127.0.0.1:21080/ops-console.html
wait_http collector http://127.0.0.1:21081/api/health
wait_http sensor-data http://127.0.0.1:21082/api/health

printf '%s\n' "$release_id" > "$app_dir/.cd-current-release.tmp"
mv -f "$app_dir/.cd-current-release.tmp" "$app_dir/.cd-current-release"
echo "Deployed release $release_id; MQTT, database, InfluxDB, and legacy services were untouched"
