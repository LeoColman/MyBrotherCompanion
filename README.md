MyBrotherCompanion
=====================

Small Ktor service that exposes simple endpoints to print labels on a Brother QL series printer. A multi‑stage Dockerfile is provided to build and run the app without requiring Java/Gradle on the host.

Docker Compose quick start
-------------------------

The easiest way to run this project is via Docker Compose. It will build the image and start the service. With recent Docker Compose (v2.22+), source changes can automatically trigger a rebuild and restart.

Steps:

1) Clone the repository

- git clone https://github.com/YOUR_USER/BrotherQL700Companion.git
- cd BrotherQL700Companion

2) Start the service

- docker compose up --build

This builds the image using the Dockerfile and runs the container exposing port 8088.

3) Verify it’s running

- curl http://localhost:8088/health

Auto rebuild on code changes (Compose develop/watch):

- The provided docker-compose.yml includes a develop.watch section. With Docker Compose v2.22+ (Docker Desktop 4.25+ or an up‑to‑date Docker CLI), edits to source or Gradle files will trigger an automatic image rebuild and container restart while compose is running.
- Keep docker compose up running and just edit files; Compose will rebuild and restart the service.

Troubleshooting: Symbola font download during image build
--------------------------------------------------------

The runtime image downloads the Symbola font at build time and already tries several mirrors automatically. If your environment blocks those mirrors or they are temporarily unavailable, you can provide your own URL via a build argument.

- With Docker build:
  - docker build --build-arg SYMBOLA_URL=https://example.com/path/to/Symbola.ttf -t brotherql700-companion:latest .

- With Docker Compose (add under services.app.build in docker-compose.yml):

  build:
    context: .
    dockerfile: Dockerfile
    args:
      SYMBOLA_URL: "https://example.com/path/to/Symbola.ttf"

Notes:
- The URL can point to either a .ttf file or a .zip containing Symbola.ttf; the Dockerfile handles both.
- If you don’t set SYMBOLA_URL, the build will attempt multiple known mirrors automatically.

Connecting to your CUPS with Compose:

- Linux host: prefer host networking so the container can reach the host’s cupsd at 127.0.0.1:631.
  - Edit docker-compose.yml to uncomment network_mode: host and remove the ports mapping.
- Docker Desktop (macOS/Windows): use CUPS_SERVER=host.docker.internal so lp inside the container can reach the host’s cupsd.
  - docker compose up -d (then set the env in docker-compose.yml or pass with -e when using docker run)

If your CUPS is elsewhere, set CUPS_SERVER to that host/IP and ensure remote printing is allowed.

Quick start (build and run with Docker directly)
-----------------------------------------------

Prerequisites:

- Docker installed and running

Build the image (the Dockerfile uses the Gradle wrapper to create a fat JAR inside the image):

- docker build -t brotherql700-companion:latest .

Run the container (basic):

- docker run --rm -p 8088:8088 brotherql700-companion:latest

Verify the service is up:

- curl http://localhost:8088/health

Available endpoints:

- GET /health → simple health check
- POST /print-datetime-br → prints a label with the current date/time (pt-BR)
- POST /print-proverb-pt → prints a random Portuguese proverb
- POST /print-weekly-house-routine → prints today's or a specified day's routine (optional query param day)

Swagger / OpenAPI UI
--------------------

This project exposes a Swagger UI to browse and try the API endpoints.

- OpenAPI spec file: src/main/resources/openapi/documentation.yaml
- When the server is running, open the UI at:
  - http://localhost:8088/swagger

From there you can expand endpoints and execute requests directly from the browser.

Run with Docker Compose
-----------------------

This repo ships a `docker-compose.yml` to make it easy to run and rebuild.

- Rebuild when starting (recommended):

  - docker compose up --build

- Linux host (use host network to reach local CUPS):

  - docker compose --profile hostnet up --build

- macOS/Windows: publish the port and optionally point to host CUPS via env:

  - docker compose up --build
  - CUPS example: `CUPS_SERVER=host.docker.internal docker compose up --build`

Convenience Makefile targets
---------------------------

If you prefer short commands, use the provided Makefile which always rebuilds on up:

- make up              # equivalent to: docker compose up --build
- make up-hostnet      # Linux host network mode (rebuilds too)
- make down            # stop and remove
- make logs            # follow logs

Printing notes (CUPS and queue)
-------------------------------

The app shells out to the OS tools to generate and send labels:

- ImageMagick convert
- brother_ql_create (Python package brother_ql)
- lp (CUPS client)

By default the print queue name is hardcoded to Koda (see BasePrinter.DEFAULT_QUEUE). Ensure a CUPS queue with that name exists and points to your Brother printer, or change the code and rebuild if you need a different queue name.

Connecting the container to your host's CUPS

- Linux (recommended): share the host network namespace so the container can access the host's cupsd on 127.0.0.1:631.

  - docker run --rm --network=host brotherql700-companion:latest

  In this mode you do not need -p 8088:8088. The app will be reachable at http://localhost:8088 on the host.

- macOS/Windows (Docker Desktop): --network=host is not supported. Use host.docker.internal to reach services on the host. Set CUPS_SERVER so lp inside the container talks to the host’s CUPS.

  - docker run --rm -p 8088:8088 -e CUPS_SERVER=host.docker.internal brotherql700-companion:latest

Alternatively, if your CUPS server is on a different machine, set CUPS_SERVER to that hostname or IP. Make sure the CUPS server allows remote connections and your queue (default: Koda) exists and is accepting jobs.

Stop and remove the container
-----------------------------

If you started it in the foreground with --rm, press Ctrl+C to stop and auto‑remove. If you ran it detached (add -d), you can stop and remove with:

- docker ps
- docker stop <container_id>

Build and publish to Docker Hub
-------------------------------

1) Log in to Docker Hub:

- docker login

2) Tag and push a versioned image (replace YOUR_USER and version tag as needed):

- export IMAGE=YOUR_USER/brotherql700-companion:0.1.0
- docker build -t $IMAGE .
- docker push $IMAGE

3) Optionally tag and push latest as well:

- docker tag $IMAGE YOUR_USER/brotherql700-companion:latest
- docker push YOUR_USER/brotherql700-companion:latest

Multi‑arch publish (optional)
-----------------------------

To publish images for both amd64 and arm64 (requires Buildx):

- docker buildx create --use
- docker buildx build \
    --platform linux/amd64,linux/arm64 \
    -t YOUR_USER/brotherql700-companion:0.1.0 \
    -t YOUR_USER/brotherql700-companion:latest \
    --push .

Additional details
------------------

- The container listens on 0.0.0.0:8088 (EXPOSE 8088 in Dockerfile).
- Runtime base: eclipse-temurin:21-jre-alpine. Build base: eclipse-temurin:21-jdk.
- Installed in the runtime image: ImageMagick, cups-client, Python3 pip + brother_ql, DejaVu fonts, bash.
