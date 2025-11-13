FROM eclipse-temurin:23.0.2_7-jdk-alpine

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia os arquivos do projeto para o contêiner
COPY . .

# Torna o script gradlew executável
RUN chmod +x gradlew

# Executa o comando de build do Gradle
RUN ./gradlew clean build

# Lista os arquivos gerados para debug
RUN ls -la build/libs/

# Copia o arquivo JAR gerado pelo Gradle para o contêiner
RUN cp build/libs/toiter-userservice-0.0.1-SNAPSHOT.jar app.jar

# Define o comando de inicialização do contêiner
ENTRYPOINT ["java", "-jar", "app.jar"]