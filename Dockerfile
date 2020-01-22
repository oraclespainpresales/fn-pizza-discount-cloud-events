FROM fnproject/fn-java-fdk-build:jdk11-1.0.105 as build-stage
WORKDIR /function
ENV MAVEN_OPTS -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts= -Dmaven.repo.local=/usr/share/maven/ref/repository

ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=true", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target", "--fail-never"]

ADD src /function/src
RUN ["mvn", "package", "-DskipTests=true"]

FROM fnproject/fn-java-fdk:jre11-1.0.105
RUN mkdir .oci
COPY config /.oci/config
COPY oci_api_key.pem /.oci/oci_api_key.pem
RUN ls -la /.oci/
RUN mkdir /.oci/pp
COPY /oci-config/config /.oci/pp/config
COPY /oci-config/oci_api_key.pem /.oci/pp/oci_api_key.pem
RUN ls -la /.oci/pp

WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/

CMD ["com.example.fn.DiscountCampaignUploader::handleRequest"]