import yaml
import os

def fix_context(file_path):
    if not os.path.exists(file_path):
        return
        
    with open(file_path, "r") as f:
        data = yaml.safe_load(f)

    if "services" in data:
        for s_name, s_config in data["services"].items():
            if "build" in s_config and isinstance(s_config["build"], dict):
                if "context" in s_config["build"]:
                    s_config["build"]["context"] = "../.."

    with open(file_path, "w") as f:
        yaml.dump(data, f, sort_keys=False, default_flow_style=False)

fix_context("docker-compose.yml")
fix_context("docker-compose-hibrido.yml")
