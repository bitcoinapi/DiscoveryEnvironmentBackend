#!/bin/sh
set -x
set -e

docker run --rm -t -a stdout -a stderr -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/build -v ~/.m2:/root/.m2 -w /build discoenv/buildenv lein uberjar
docker build --rm -t $DOCKER_USER/template-mover:dev .
docker push $DOCKER_USER/template-mover:dev
