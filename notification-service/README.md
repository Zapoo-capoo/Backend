# Notification service

## Prerequisites

### Mongodb
Install Mongodb from Docker Hub

`docker pull bitnami/mongodb:7.0.11`

Start Mongodb server at port 27017 with root username and password: root/root

`docker run -d --name mongodb-7.0.11 -p 27017:27017 -e MONGODB_ROOT_USER=root -e MONGODB_ROOT_PASSWORD=root bitnami/mongodb:7.0.11`
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