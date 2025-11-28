MyBrotherCompanion
=====================

Small Ktor service that exposes simple endpoints to print labels on a Brother QL series printer. A multi‑stage Dockerfile is provided to build and run the app without requiring Java/Gradle on the host.

Quick start (build and run)
---------------------------

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
- GET /print-datetime-br → prints a label with the current date/time (pt-BR)
- GET /print-proverb-pt → prints a random Portuguese proverb
- GET /print-weekly-house-routine → prints today's or a specified day's routine (optional query param day)

Swagger / OpenAPI UI
--------------------

This project exposes a Swagger UI to browse and try the API endpoints.

- OpenAPI spec file: src/main/resources/openapi/documentation.yaml
- When the server is running, open the UI at:
  - http://localhost:8088/swagger

From there you can expand endpoints and execute requests directly from the browser.

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
