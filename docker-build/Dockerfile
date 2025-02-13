### Build stage ###
FROM maven:3.9.5-amazoncorretto-17 AS build_dashboarddataprocessor
# TODO: edit path
ENV HOME_utilities=/opt/utilities
ENV HOME_dashboardlogic=/opt/dashboardlogic
ENV HOME_dashboarddataprocessor=/opt/dashboarddataprocessor

RUN mkdir -p $HOME_utilities
RUN mkdir -p $HOME_dashboardlogic
RUN mkdir -p $HOME_dashboarddataprocessor

WORKDIR $HOME_utilities
ADD utilities $HOME_utilities
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME_utilities/pom.xml clean install

WORKDIR $HOME_dashboardlogic
ADD dashboardlogic $HOME_dashboardlogic
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME_dashboardlogic/pom.xml clean install

WORKDIR $HOME_dashboarddataprocessor
ADD dashboarddataprocessor $HOME_dashboarddataprocessor
RUN --mount=type=cache,target=/root/.m2 mvn -f $HOME_dashboarddataprocessor/pom.xml clean install

### Package stage ###
FROM maven:3.9.5-amazoncorretto-17 AS dashboard-data-processor
# ToDo: edit path
ARG JAR_FILE=/opt/dashboarddataprocessor/target/*.jar
COPY --from=build_dashboarddataprocessor $JAR_FILE /dashboarddataprocessor/dashboard-data-processor.jar
ENTRYPOINT ["java","-jar","/dashboarddataprocessor/dashboard-data-processor.jar","--spring.config.location=/dashboarddataprocessor/application.yaml"]
