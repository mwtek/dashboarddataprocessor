FROM alpine:latest

RUN apk add --no-cache git
RUN apk add --no-cache openjdk17 maven

RUN cd /opt && mkdir dashboardprocessor
WORKDIR /opt/dashboardprocessor

RUN git clone --depth 1 --branch v0.5.4-dev.0 https://www.github.com/mwtek/utilities.git
RUN git clone --depth 1 --branch v0.5.4-dev.0 https://www.github.com/mwtek/dashboardlogic.git
RUN git clone --depth 1 --branch v0.5.4-dev.0 https://www.github.com/mwtek/dashboarddataprocessor.git

ENTRYPOINT cd dashboarddataprocessor && sh build.sh && sh run.sh
