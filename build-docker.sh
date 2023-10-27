#!/bin/bash

# Check group
if [[ ! "$(id -Gn)" =~ .*"docker".* ]]; then
        echo "You need to be member of group $(tput setaf 1)docker$(tput sgr 0)"
        echo "try \"sudo usermod -G docker -a $(id -un)\""
        echo "then relogin or try \"sudo su -l $(id -un)\""
        exit 1
fi

### Start ###
name="dashboard-data-processor"

echo -e "Build $name"
docker build --quiet -f Dockerfile -t dashboard-data-processor:0.5.0 .

echo -e "Save $name"
docker save dashboard-data-processor:0.5.0 -o DDP-V0.5.0.tar

docker system df -v
