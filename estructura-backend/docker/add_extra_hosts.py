import yaml

with open("docker-compose-hibrido.yml", "r") as f:
    data = yaml.safe_load(f)

if "services" in data:
    for service_name, service_config in data["services"].items():
        if "extra_hosts" not in service_config:
            service_config["extra_hosts"] = []
        if "host.docker.internal:host-gateway" not in service_config["extra_hosts"]:
            service_config["extra_hosts"].append("host.docker.internal:host-gateway")

with open("docker-compose-hibrido.yml", "w") as f:
    yaml.dump(data, f, sort_keys=False, default_flow_style=False)
