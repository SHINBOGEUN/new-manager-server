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

manifest=.cd-incoming/$release_id/revisions.txt
[[ -s "$manifest" ]] || { echo 'Missing revision manifest' >&2; exit 1; }
read_value() {
  local key=$1 value
  value=$(awk -F= -v key="$key" '$1 == key { print substr($0, index($0, "=") + 1) }' "$manifest")
  [[ -n "$value" ]] || { echo "Missing $key in revision manifest" >&2; exit 1; }
  printf '%s' "$value"
}
read_version() {
  local value
  value=$(read_value "$1")
  [[ "$value" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] || { echo "Invalid $1" >&2; exit 1; }
  printf '%s' "$value"
}

manager_version=$(read_version manager-version)
collector_version=$(read_version collector-version)
sensor_data_version=$(read_version sensor-data-version)
manager_commit=$(read_value manager)
collector_commit=$(read_value collector)
sensor_data_commit=$(read_value sensor-data)
targets=$(read_value services)
IFS=, read -r -a requested_services <<< "$targets"
(( ${#requested_services[@]} > 0 )) || { echo 'No services selected' >&2; exit 1; }

declare -A seen=()
services=()
containers=()
bundled_jars=()
jars=()
jar_stems=()
health_urls=()
versions=()
for service in "${requested_services[@]}"; do
  [[ -z "${seen[$service]+x}" ]] || { echo "Duplicate service: $service" >&2; exit 1; }
  seen[$service]=1
  case "$service" in
    manager-server)
      containers+=(dcim_new_manager_server)
      bundled_jars+=(manager.jar)
      jars+=("new-manager-server-${manager_version}.jar")
      jar_stems+=(new-manager-server)
      health_urls+=(http://127.0.0.1:21080/ops-console.html)
      versions+=("manager=$manager_version")
      ;;
    collector-service)
      containers+=(dcim_new_collector_service)
      bundled_jars+=(collector.jar)
      jars+=("new-collector-server-${collector_version}.jar")
      jar_stems+=(new-collector-server)
      health_urls+=(http://127.0.0.1:21081/api/health)
      versions+=("collector=$collector_version")
      ;;
    sensor-data-service)
      containers+=(dcim_new_sensor_data_service)
      bundled_jars+=(sensor-data.jar)
      jars+=("new-sensor-data-server-${sensor_data_version}.jar")
      jar_stems+=(new-sensor-data-server)
      health_urls+=(http://127.0.0.1:21082/api/health)
      versions+=("sensor-data=$sensor_data_version")
      ;;
    *) echo "Unsupported service: $service" >&2; exit 1 ;;
  esac
  services+=("$service")
done

current_jars=()
project=
for index in "${!services[@]}"; do
  running_project=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "${containers[$index]}")
  running_service=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.service"}}' "${containers[$index]}")
  [[ "$running_project" =~ ^[A-Za-z0-9][A-Za-z0-9_-]*$ && "$running_service" == "${services[$index]}" ]] || {
    echo "Unexpected Compose labels on ${containers[$index]}" >&2
    exit 1
  }
  if [[ -z "$project" ]]; then project=$running_project; fi
  [[ "$running_project" == "$project" ]] || { echo 'Selected app containers have different Compose projects' >&2; exit 1; }
  mounted_jar=$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/opt/dcim/app.jar"}}{{.Source}}{{end}}{{end}}' "${containers[$index]}")
  current_jar=${mounted_jar##*/}
  [[ "$mounted_jar" == "$app_dir/$current_jar" && "$current_jar" =~ ^${jar_stems[$index]}-[A-Za-z0-9][A-Za-z0-9._-]*\.jar$ && -f "$mounted_jar" ]] || {
    echo "Unexpected JAR mount for ${containers[$index]}" >&2
    exit 1
  }
  current_jars+=("$current_jar")
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
  [[ -s "$incoming/${bundled_jars[$index]}" ]] || {
    echo "Missing bundle JAR: ${bundled_jars[$index]}" >&2
    exit 1
  }
done

mkdir -p "$app_dir/.cd-releases"
mv "$incoming" "$release"
mkdir "$release/previous"
cp -p docker-compose.yml "$release/previous/docker-compose.yml"
for jar in "${current_jars[@]}"; do
  cp -p "$app_dir/$jar" "$release/previous/$jar"
done

updated=0
rollback() {
  local original_status=$?
  trap - ERR HUP INT TERM
  (( original_status != 0 )) || original_status=1
  if (( updated )); then
    echo 'Deployment failed; restoring the previous Compose file and selected JARs' >&2
    if ! { cp -p "$release/previous/docker-compose.yml" "$app_dir/.docker-compose.yml.rollback" &&
           mv -f "$app_dir/.docker-compose.yml.rollback" "$app_dir/docker-compose.yml"; }; then
      echo 'Compose rollback failed' >&2
    fi
    for jar in "${current_jars[@]}"; do
      if ! { install -m 0644 "$release/previous/$jar" "$app_dir/.$jar.rollback" &&
             mv -f "$app_dir/.$jar.rollback" "$app_dir/$jar"; }; then
        echo "Rollback copy failed for $jar" >&2
      fi
    done
    if ! "${compose[@]}" up -d --no-deps --force-recreate "${services[@]}"; then
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
  compose_tmp="$app_dir/.docker-compose.yml.deploy"
  sed -E "s#(\\./${jar_stems[$index]}-)[^/:]+\\.jar(:/opt/dcim/app\\.jar)#\\1${jars[$index]#${jar_stems[$index]}-}\\2#" \
    "$app_dir/docker-compose.yml" > "$compose_tmp"
  mount_count=$(grep -Fc "./${jars[$index]}:/opt/dcim/app.jar" "$compose_tmp" || true)
  [[ "$mount_count" == 1 ]] || {
    rm -f "$compose_tmp"
    echo "Could not update Compose JAR mount for ${services[$index]}" >&2
    exit 1
  }
  mv -f "$compose_tmp" "$app_dir/docker-compose.yml"
done

# Recreate only selected services: unselected app containers and their JARs stay untouched.
"${compose[@]}" up -d --no-deps --force-recreate "${services[@]}"

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

for index in "${!services[@]}"; do
  wait_http "${services[$index]}" "${health_urls[$index]}"
done

printf '%s\n' "$release_id" > "$app_dir/.cd-current-release.tmp"
mv -f "$app_dir/.cd-current-release.tmp" "$app_dir/.cd-current-release"
for index in "${!jars[@]}"; do
  if [[ "${current_jars[$index]}" != "${jars[$index]}" ]]; then
    if ! rm -f -- "$app_dir/${current_jars[$index]}"; then
      echo "Could not remove obsolete JAR ${current_jars[$index]}; a copy is preserved in $release/previous" >&2
    fi
  fi
done
printf 'Deployed release %s: services=%s versions=%s commits=%s,%s,%s\n' \
  "$release_id" "${services[*]}" "${versions[*]}" "$manager_commit" "$collector_commit" "$sensor_data_commit"
echo 'Unselected app services, MQTT, databases, InfluxDB, and legacy services were untouched'
