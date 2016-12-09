# Build docker images
```
sbt assembly
docker build -t senz/sdbl-trans .
```

# Run with docker
```
docker run -it \
-e SWITCH_HOST=10.22.196.108 \
-e SWITCH_PORT=9090 \
-e EPIC_HOST=124.43.16.185 \
-e EPIC_PORT=8200 \
-e CASSANDRA_HOST=192.168.22.171 \
-e CASSANDRA_PORT=9042 \
-v /home/senz/SENZ_APPS/SDBL/sdbl-trans/logs:/app/logs:rw \
-v /home/senz/SENZ_APPS/SDBL/sdbl-trans/.keys:/app/.keys:rw \
senz/sdbl-trans
```
