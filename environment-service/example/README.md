# Environment config templates

`config-service` serves the per-service configuration from `environment-service/local/`.
That folder holds real passwords and keys, so it is **git-ignored**. The files here are the same
configuration with every secret replaced by a placeholder.

## Setup

1. Copy the templates:

   ```bash
   mkdir -p environment-service/local
   cp environment-service/example/* environment-service/local/
   ```

   (skip `README.md`, or delete the copy.)

2. Fill in the values marked `CHANGE_ME` (and `your-bucket-name`) in `environment-service/local/*`:

   | File | What to fill in |
   |---|---|
   | `identity-service.yaml` | MySQL url/user/password, `jwt.signerKey`, Google client id/secret |
   | `profile-service.yaml` | Neo4j password |
   | `chat-service.yaml` | MongoDB credentials, Redis password |
   | `file-service.yaml` | MongoDB credentials, storage dir |
   | `storage-service.yml` | PostgreSQL url/user/password, `signature.md5.secret-key`, S3 bucket/region/keys, ffmpeg path |

3. Point `config-service` at the folder: set `spring.cloud.config.server.native.search-locations`
   in `config-service/src/main/resources/application.yml` (see `config-service.yml` here for the format).

Never commit `environment-service/local/`. If a real key was ever pushed, rotate it.
