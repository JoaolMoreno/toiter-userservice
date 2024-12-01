# Limpa e constrói o projeto usando Gradle
./gradlew clean build

# Cria a imagem Docker
docker build -t toiter-user-service:latest .
