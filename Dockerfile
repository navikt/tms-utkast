FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ADD app/build/distributions/app.tar / ./
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-cp", "/app/lib/*", "no.nav.tms.utkast.ApplicationKt"]