# GPU agent discovery

Apply `sql/migration/20261106_gpu_agent_catalog.sql` before enabling discovery.
Configure an enabled Manager MQTT source of type `AGENT` with the GPU topic filter;
Sensor Data and Manager must share the Sensor Data service API key. After an unknown
UUID publishes, an administrator opens **장비 관리 → GPU 에이전트 등록 대기** and
approves it. The next message after MQTT configuration refresh is stored in the
existing `dcim_sensor` measurement. Earlier unapproved samples are not replayed.
Disabled identities are never automatically re-enabled; rejected requests stay rejected.
The installed agent identity is persisted during reinstalls, but deleting its installed
configuration creates a new UUID and therefore a new approval request.

Broker authentication and publish ACLs must restrict who may publish agent telemetry:
MQTT payload UUIDs alone do not authenticate the sender. Approval of an observed UUID
does not stop another publisher from impersonating it.

Server metrics include CPU and RAM utilization, RAM size, uptime, GPU count and GPU
power sum. Each GPU supplies UUID, name, temperature, power, utilization, VRAM usage
and fan speed when available. Linux load averages are sent when supported; Windows
does not supply equivalent values. Missing readings are omitted, not represented as
zero. System/GPU inventory is kept in `gpu_agent_discovery.metadata_json` and can be
read by device ID through `/api/manager/gpu-agents/device/{deviceId}`. GPU 1-hour
maximum, historical averages and online/offline state must be computed by a consumer
from measurements and last-seen times; these are not sent as instantaneous values.

