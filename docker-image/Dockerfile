FROM alpine:latest

RUN apk add --no-cache git
RUN apk add --no-cache openjdk25 maven

RUN mkdir -p /opt/dashboarddataprocessor
WORKDIR /opt/dashboarddataprocessor

RUN git clone --depth 1 --branch v0.5.7+update.0 https://www.github.com/mwtek/utilities.git
RUN git clone --depth 1 --branch v0.5.7+update.0 https://www.github.com/mwtek/dashboardlogic.git
RUN git clone --depth 1 --branch v0.5.7+update.0 https://www.github.com/mwtek/dashboarddataprocessor.git

# Experimental repos
# RUN git clone --depth 1 --branch main https://ghp_TOKEN@github.com/mwtek/utilities-experimental.git utilities
# RUN git clone --depth 1 --branch main https://ghp_TOKEN@github.com/mwtek/dashboardlogic-experimental.git dashboardlogic
# RUN git clone --depth 1 --branch main https://ghp_TOKEN@github.com/mwtek/dashboarddataprocessor-experimental.git dashboarddataprocessor

RUN cd dashboarddataprocessor && sh build.sh

RUN chown -R 1000:1000 .

WORKDIR /opt/dashboarddataprocessor/dashboarddataprocessor
ENTRYPOINT ["sh", "run.sh"]
