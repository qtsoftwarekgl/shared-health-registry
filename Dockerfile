FROM tomcat:jdk14-openjdk-oracle

ADD ../rhies-shr-server/target/rhies-shr-server-1.0.0.war /usr/local/tomcat/webapps/shr.war

EXPOSE 8080

