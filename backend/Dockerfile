FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace
COPY . .

ARG MODULE_DIR=backend

RUN set -eux; \
  cd "$MODULE_DIR"; \
  if [ ! -f build.gradle ] && [ ! -f build.gradle.kts ]; then \
    echo "Gradle 프로젝트를 찾을 수 없습니다"; \
    ls -la; exit 1; \
  fi; \
  [ -f gradlew ] || gradle wrapper --gradle-version 8.8 || true; \
  [ -f gradlew ] && sed -i "s/\r$//" gradlew || true; \
  [ -f gradlew ] && chmod +x gradlew || true; \
  if [ -f gradlew ]; then ./gradlew --no-daemon clean bootJar -x test; \
  else gradle --no-daemon clean bootJar -x test; fi; \
  ART="$(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -n1)"; \
  test -n "$ART"; cp "$ART" /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8081
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-lc","exec java $JAVA_OPTS -jar /app/app.jar"]
