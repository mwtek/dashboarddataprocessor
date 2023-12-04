FROM maven:3.9.5-amazoncorretto-17 AS dashboard-data-processor
COPY dashboarddataprocessor/target/dashboarddataprocessor-0.5.0-alpha1.jar /dashboarddataprocessor/dashboard-data-processor.jar
COPY dashboarddataprocessor/application.yaml /dashboarddataprocessor
ENTRYPOINT ["java","-jar","/dashboarddataprocessor/dashboard-data-processor.jar","--spring.config.location=/dashboarddataprocessor/application.yaml"]
