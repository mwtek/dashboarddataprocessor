FROM alpine:latest

RUN apk add --no-cache git
RUN apk add --no-cache openjdk17 maven

RUN cd /opt && mkdir dashboardprocessor
WORKDIR /opt/dashboardprocessor

RUN git clone --depth 1 --branch v0.5.4+update.11 https://www.github.com/mwtek/utilities.git
RUN git clone --depth 1 --branch v0.5.4+update.11 https://www.github.com/mwtek/dashboardlogic.git
RUN git clone --depth 1 --branch v0.5.4+update.11 https://www.github.com/mwtek/dashboarddataprocessor.git

# Experimental
#RUN git clone --depth 1 --branch main https://ghp_TOKEN@github.com/mwtek/utilities-experimental.git /opt/dashboardprocessor/utilities
#RUN git clone --depth 1 --branch main https://ghp_TOKEN@github.com/mwtek/dashboardlogic-experimental.git /opt/dashboardprocessor/dashboardlogic
#RUN git clone --depth 1 --branch main https://ghp_TOKEN@github.com/mwtek/dashboarddataprocessor-experimental.git /opt/dashboardprocessor/dashboarddataprocessor

RUN cd dashboarddataprocessor && sh build.sh

SHELL ["/bin/sh","-c"]
ENTRYPOINT cd dashboarddataprocessor && sh run.sh
